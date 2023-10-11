package com.fszarwacki.allezon.processor;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.Value.MapValue;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.fszarwacki.allezon.common.Action;
import com.fszarwacki.allezon.common.UserProfileResult;
import com.fszarwacki.allezon.common.UserTagEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@Service
public class WriteBufferService {
  private static final Logger log = LoggerFactory.getLogger(WriteBufferService.class);
  private String topic;

  private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  private Producer<String, String> producer;
  
  private final AerospikeClient aerospikeClient;
  private final String namespace;
  private final String set;

  @Autowired
  public WriteBufferService(Environment env) {
    this.aerospikeClient = new AerospikeClient("10.112.115.101", 3000); // TODO from config
    this.namespace = "allezon";
    this.set = "user-profiles";

    Properties properties = new Properties();
    properties.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    properties.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    properties.setProperty("bootstrap.servers", env.getProperty("kafka.bootstrap.servers"));
    
    this.producer = new KafkaProducer<>(properties);
    this.topic = env.getProperty("kafka.topic");
  }

  @Async
  public void addToBuffer(UserTagEvent userTagEvent) {
    // First part: sending to Aerospike.
    try {
      Key key = new Key(namespace, set, userTagEvent.getCookie());
      Record record = aerospikeClient.get(null, key);

      if (record != null) {
        // TODO delete old events
        // TODO maybe this can be only one case: update or create
        MapPolicy mapPolicy = new MapPolicy();

        WritePolicy writePolicy = new WritePolicy();
        writePolicy.recordExistsAction = RecordExistsAction.UPDATE;

        aerospikeClient.operate(
          writePolicy, 
          key, 
          MapOperation.put(mapPolicy, "events", Value.get(userTagEvent.getTimeInMiliseconds()), Value.get(userTagEvent))
        );

        log.debug("Record updated successfully.");

      } else {
        // User with this cookie does not exist yet. It needs to be created.

        HashMap <Long, Object> events = new HashMap<>();
        events.put(userTagEvent.getTimeInMiliseconds(), userTagEvent);
        WritePolicy writePolicy = new WritePolicy();

        Bin bin1 = new Bin("cookie", userTagEvent.getCookie());
        Bin bin2 = new Bin("events", events);

        aerospikeClient.put(writePolicy, key, bin1, bin2);

        log.debug("New record created.");
      }

    } catch (Exception e) {
        log.error("Error during sending to aerospike.", e);
    }

    // Second part: sending to Kafka.
    try {
      String key = Long.toString(userTagEvent.getTimeInMiliseconds());
      producer.send(new ProducerRecord<>(topic, key, GSON.toJson(userTagEvent)));
    } catch (Exception e) {
      log.error("Error during sending to kafka.", e);
    }
  }


  public UserProfileResult getProfile(String cookie, String timeRangeStr, int limit) {
    Long startTimeMiliseconds = Instant.parse(timeRangeStr.split("_")[0] + "Z").toEpochMilli();
    Long endTimeMiliseconds = Instant.parse(timeRangeStr.split("_")[1] + "Z").toEpochMilli();


    List <UserTagEvent> queryResponse = null;

    try {
      Key key = new Key(namespace, set, cookie);
      Record record = aerospikeClient.get(null, key);

      if (record != null) {
        Record rangeObservations = aerospikeClient.operate(
          null, 
          key, 
          MapOperation.getByKeyRange("events", Value.get(startTimeMiliseconds), Value.get(endTimeMiliseconds), MapReturnType.KEY_VALUE));

          queryResponse = (List<UserTagEvent>) rangeObservations.getValue("events");

      } else {
        log.error("User with given cookie does not exist. cookie: " + cookie);
      }

    } catch (Exception e) {
        log.error("Error during reading from aerospike.", e);
    }


    List<UserTagEvent> views = queryResponse.stream()
                .filter(userTagEvent -> userTagEvent.getAction() == Action.VIEW)
                .limit(limit)
                .collect(Collectors.toList());

    List<UserTagEvent> buys = queryResponse.stream()
                .filter(userTagEvent -> userTagEvent.getAction() == Action.BUY)
                .limit(limit)
                .collect(Collectors.toList());


    return new UserProfileResult(cookie, views, buys);
  }


}
