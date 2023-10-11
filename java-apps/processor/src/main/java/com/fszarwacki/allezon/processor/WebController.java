package com.fszarwacki.allezon.processor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fszarwacki.allezon.common.Action;
import com.fszarwacki.allezon.common.Aggregate;
import com.fszarwacki.allezon.common.AggregatesQueryResult;
import com.fszarwacki.allezon.common.UserProfileResult;
import com.fszarwacki.allezon.common.UserTagEvent;
import com.fszarwacki.allezon.common.AggregatesBucket;
import com.fszarwacki.allezon.common.AggregatesBucketRepository;

@RestController
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);
    @Autowired
    private UserTagEventRepository userTagEventRepository;
    @Autowired
    private WriteBufferService writeBufferService;
    //@Autowired
    //private AggregatesBucketRepository aggregatesBucketRepository;
    @Autowired
    private RemoveOldTagsService removeOldTagsService;

    @PostMapping("/user_tags")
    public ResponseEntity<Void> addUserTag(@RequestBody(required = false) UserTagEvent userTag) {
        Instant start = Instant.now();
        Instant requestTime = Instant.parse(userTag.getTime());
        userTag.setTimeInMiliseconds(requestTime.toEpochMilli());
        
        writeBufferService.addToBuffer(userTag);
        
        Instant end = Instant.now();
        if (Duration.between(start, end).toMillis() > 100) {
            log.warn("USERTAG Slowly processed request; " + Duration.between(start, end).toMillis() + "ms;");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user_profiles/{cookie}")
    public ResponseEntity<UserProfileResult> getUserProfile(@PathVariable("cookie") String cookie,
            @RequestParam("time_range") String timeRangeStr,
            @RequestParam(defaultValue = "200") int limit,
            @RequestBody(required = false) UserProfileResult expectedResult) {
        
        Instant start = Instant.now();
        
        UserProfileResult response = writeBufferService.getProfile(cookie, timeRangeStr, limit);
        
        if (!response.equals(expectedResult)) {
            log.error("Incorrect answer\ncookie: " + cookie + "\ntime_range: " + timeRangeStr + "\nlimit: " + limit + "\nexpected: " + expectedResult + "\nresponse: " + response);
        }

        Instant end = Instant.now();
        if (Duration.between(start, end).toMillis() > 100) {
            log.warn("PROFILE Slowly processed request; " + Duration.between(start, end).toMillis());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/aggregates")
    public ResponseEntity<AggregatesQueryResult> getAggregates(@RequestParam("time_range") String timeRangeStr,
            @RequestParam("action") Action action,
            @RequestParam("aggregates") List<Aggregate> aggregates,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "brand_id", required = false) String brandId,
            @RequestParam(value = "category_id", required = false) String categoryId,
            @RequestBody(required = false) AggregatesQueryResult expectedResult) {
        return ResponseEntity.internalServerError().build();
        /* 
        log.info("AGGREGATES");
        Instant start = Instant.now();
        AggregatesQueryResult result = null;
        List <AggregatesBucket> records = null;
        
        Long startTime = Instant.parse(timeRangeStr.split("_")[0] + "Z").getEpochSecond();
        Long endTime = Instant.parse(timeRangeStr.split("_")[1] + "Z").getEpochSecond();

        List<String> optionalCriteria = null;
        Instant beforeRequest = Instant.now();
        if ((origin == null) && (brandId == null) && (categoryId == null)) { 
            records = aggregatesBucketRepository.findByActionAndBucketStartTimeBetween(action, startTime, endTime - 1);
            optionalCriteria = List.of();
        }
        else if ((origin == null) && (brandId == null) && (categoryId != null)) { 
            records = aggregatesBucketRepository.findByActionAndCategoryIdAndBucketStartTimeBetween(action, categoryId, startTime, endTime - 1);
            optionalCriteria = List.of("category_id");
        }
        else if ((origin == null) && (brandId != null) && (categoryId == null)) { 
            records = aggregatesBucketRepository.findByActionAndBrandIdAndBucketStartTimeBetween(action, brandId, startTime, endTime - 1);
            optionalCriteria = List.of("brand_id");
        }
        else if ((origin == null) && (brandId != null) && (categoryId != null)) { 
            records = aggregatesBucketRepository.findByActionAndBrandIdAndCategoryIdAndBucketStartTimeBetween(action, brandId, categoryId, startTime, endTime - 1);
            optionalCriteria = List.of("brand_id", "category_id");
        }
        else if ((origin != null) && (brandId == null) && (categoryId == null)) { 
            records = aggregatesBucketRepository.findByActionAndOriginAndBucketStartTimeBetween(action, origin, startTime, endTime - 1);
            optionalCriteria = List.of("origin");
        }
        else if ((origin != null) && (brandId == null) && (categoryId != null)) { 
            records = aggregatesBucketRepository.findByActionAndOriginAndCategoryIdAndBucketStartTimeBetween(action, origin, categoryId, startTime, endTime - 1);
            optionalCriteria = List.of("origin", "category_id");
        }
        else if ((origin != null) && (brandId != null) && (categoryId == null)) { 
            records = aggregatesBucketRepository.findByActionAndOriginAndBrandIdAndBucketStartTimeBetween(action, origin, brandId, startTime, endTime - 1);
            optionalCriteria = List.of("origin", "brand_id");
        }
        else if ((origin != null) && (brandId != null) && (categoryId != null)) { 
            records = aggregatesBucketRepository.findByActionAndOriginAndBrandIdAndCategoryIdAndBucketStartTimeBetween(action, origin, brandId, categoryId, startTime, endTime - 1);
            optionalCriteria = List.of("origin", "brand_id", "category_id");
        } else {
            log.error("If statement should cover all options.");
        }
        Instant afterRequest = Instant.now();
        log.warn("AGGDEBUG records len: " + records.size());
        result = groupRecordsBy(records, optionalCriteria, aggregates, startTime, endTime, new AggregatesBucket(action, origin, brandId, categoryId));

        if (!result.equals(expectedResult)) {
            log.error("timeRange: " + timeRangeStr + "\nexpected result:\n" + expectedResult.toString() + "\nresult:\n" + result.toString());
        }
        Instant end = Instant.now();
        
        log.warn("AGG time: " + Duration.between(start, end).toMillis() + "ms; database roundtrip:" + Duration.between(beforeRequest, afterRequest).toMillis() + "ms");
        
        return ResponseEntity.ok(result);
        */
    }
/* 
    private AggregatesQueryResult groupRecordsBy(List<AggregatesBucket> records, List<String> optionalCriteria, List<Aggregate> aggregates, Long startTime, Long endTime, AggregatesBucket exampleBucket) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);
        
        Map<String, Function<AggregatesBucket, String>> getFieldsAsString = Map.ofEntries(
            Map.entry("1m_bucket", bucket -> formatter.format(Instant.ofEpochSecond(bucket.getBucketStartTime()))),
            Map.entry("action", bucket -> bucket.getAction().name()),
            Map.entry("origin", AggregatesBucket::getOrigin),
            Map.entry("brand_id", AggregatesBucket::getBrandId),
            Map.entry("category_id", AggregatesBucket::getCategoryId),
            Map.entry("count", bucket -> Long.toString(bucket.getCount())),
            Map.entry("sum_price", bucket -> Long.toString(bucket.getSumPrice()))
        );

        List<String> keys = new LinkedList<>();
        keys.add("1m_bucket");
        keys.add("action");
        keys.addAll(optionalCriteria);
    
        HashMap<Long, AggregatesBucket> grouped = records.stream()
            .collect(Collectors.toMap(
                record -> record.getBucketStartTime(), 
                record -> record, 
                (record1, record2) -> AggregatesBucket.join(record1, record2), 
                () -> new HashMap<>()
            ));
        log.warn("AGGDEBUG grouped len: " + grouped.size());
        List<String> columns = Stream.concat(keys.stream(), aggregates.stream().map(aggregate -> aggregate.name().toLowerCase())).collect(Collectors.toList());
        List<List<String>> rows = new LinkedList<>();

        for (Long i = startTime; i < endTime; i += 60 ) { // 60 seconds in a minute
            var record = grouped.getOrDefault(i, new AggregatesBucket(exampleBucket, i));
            var row = columns.stream().map(fieldName -> getFieldsAsString.get(fieldName).apply(record)).collect(Collectors.toList());
            rows.add(row);
        }

        return new AggregatesQueryResult(
            columns,
            rows
        );
    }

    */

}
