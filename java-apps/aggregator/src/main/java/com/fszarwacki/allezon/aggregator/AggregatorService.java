package com.fszarwacki.allezon.aggregator;

import com.fszarwacki.allezon.common.UserTagEvent;
import com.fszarwacki.allezon.common.AggregatesBucket;
import com.fszarwacki.allezon.common.AggregatesBucketRepository;
import com.fszarwacki.allezon.common.Action;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.SortedMap;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.javatuples.Quartet;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


@Service
public class AggregatorService {
  private static final Logger log = LoggerFactory.getLogger(AggregatorService.class);

  private static final Long TIME_OUT_MS = 10000l;
  private static final Long SECONDS_IN_MINUTE = 60l;
  private static Long COUNTER = 0l;

  private KafkaConsumer<String, String> kafkaConsumer;

  private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  private static final SortedMap<Long, Long> perMinuteCounter = new TreeMap<>();
  
  private static final Map<Long, Map<Quartet<Action, String, String, String>, Pair<Long, Long>>> aggregates = new HashMap<>();
  
  private static final Set<Long> alreadySent = new HashSet<>();

  @Autowired
  private AggregatesBucketRepository aggregatesBucketRepository;

  @Autowired
  public AggregatorService(Environment env) {
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", env.getProperty("kafka.bootstrap.servers"));
    properties.setProperty("group.id", "aggregator");
    properties.setProperty("enable.auto.commit", "false");
    properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    properties.setProperty("max.poll.records", "120000");
    this.kafkaConsumer = new KafkaConsumer<>(properties);
    this.kafkaConsumer.subscribe(List.of(env.getProperty("kafka.topic")));
  }

  @Scheduled(fixedRate = 20000)
  public void runOnce() {
    log.info("Running aggregator.");
    pollMessagesFromKafka();

    try {
      Long oldestNotWrittenBucket = perMinuteCounter.firstKey();
      if (oldestNotWrittenBucket != null) {
        Long newestBucket = perMinuteCounter.lastKey();

        if (newestBucket - oldestNotWrittenBucket > SECONDS_IN_MINUTE) {
          sendAggregatesToAerospike(oldestNotWrittenBucket);
        }
      }
    } catch (NoSuchElementException e) {
      log.info("No new records available.");
    }
  }

  private void pollMessagesFromKafka() {
    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(TIME_OUT_MS));
    kafkaConsumer.commitSync();
    log.info("Polled " + records.count() + " messages from kafka.");
    for (ConsumerRecord<String, String> record : records) {
      try {
        UserTagEvent userTagEvent = GSON.fromJson(record.value(), UserTagEvent.class);
        Instant time = Instant.ofEpochMilli(userTagEvent.getTimeInMiliseconds()).truncatedTo(ChronoUnit.MINUTES);
        Long timeEpochSeconds = time.getEpochSecond();

        if (alreadySent.contains(timeEpochSeconds)) {
          log.error("Message received after its minute has been processed.");
        }
        Long counter = perMinuteCounter.getOrDefault(timeEpochSeconds, 0l);
        perMinuteCounter.put(timeEpochSeconds, counter + 1);

        Action action = userTagEvent.getAction();
        String origin = userTagEvent.getOrigin();
        String brandId = userTagEvent.getProductInfo().getBrandId();
        String categoryId = userTagEvent.getProductInfo().getCategoryId();
        Long price = Long.valueOf(userTagEvent.getProductInfo().getPrice());

        Map<Quartet<Action, String, String, String>, Pair<Long, Long>> aggregatesForTimeBucket = aggregates.getOrDefault(timeEpochSeconds, new HashMap<>());
        Pair<Long, Long> currentAggregatesValue = aggregatesForTimeBucket.getOrDefault(timeEpochSeconds, Pair.with(0l, 0l));
        aggregatesForTimeBucket.put(Quartet.with(action, origin, brandId, categoryId), Pair.with(currentAggregatesValue.getValue0() + 1, currentAggregatesValue.getValue1() + price));
        aggregates.put(timeEpochSeconds, aggregatesForTimeBucket);

      } catch (JsonSyntaxException e) {
        log.error("Parsing json exception.", e);
      }
    }
    
  }

  private void sendAggregatesToAerospike(Long timeBucketSeconds) {
    Long recordsInBucket = perMinuteCounter.get(timeBucketSeconds);
    if (recordsInBucket != 60_000) {
      // TODO very specific to this problem
      log.error("Bucket " + Instant.ofEpochSecond(timeBucketSeconds).toString() + " containing " + recordsInBucket + " records, not 60000 as expected."); 
    }

    perMinuteCounter.remove(timeBucketSeconds);

    var aggregatesForTimeBucket = aggregates.remove(timeBucketSeconds);
    alreadySent.add(timeBucketSeconds);

    LinkedList<AggregatesBucket> toSend = new LinkedList<>();

    for (var entry : aggregatesForTimeBucket.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();

      Action action = key.getValue0();
      String origin = key.getValue1();
      String brandId = key.getValue2();
      String categoryId = key.getValue3();
      
      Long count = value.getValue0();
      Long price = value.getValue1();

      toSend.add(new AggregatesBucket(Long.toString(COUNTER++), timeBucketSeconds, action, origin, brandId, categoryId, count, price));
    }

    aggregatesBucketRepository.saveAll(toSend);
    log.info("Sent " + toSend.size() + " records to aerospike.");
  }
}
