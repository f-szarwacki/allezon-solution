package com.fszarwacki.allezon.common;

import org.springframework.data.aerospike.repository.AerospikeRepository;

import java.util.List;


public interface AggregatesBucketRepository extends AerospikeRepository<AggregatesBucket, String> {
  List<AggregatesBucket> findByActionAndBucketStartTimeBetween(Action action, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndCategoryIdAndBucketStartTimeBetween(Action action, String categoryId, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndBrandIdAndBucketStartTimeBetween(Action action, String brandId, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndBrandIdAndCategoryIdAndBucketStartTimeBetween(Action action, String brandId, String categoryId, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndOriginAndBucketStartTimeBetween(Action action, String origin, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndOriginAndCategoryIdAndBucketStartTimeBetween(Action action, String origin, String categoryId, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndOriginAndBrandIdAndBucketStartTimeBetween(Action action, String origin, String brandId, Long startTime, Long endTime);
  List<AggregatesBucket> findByActionAndOriginAndBrandIdAndCategoryIdAndBucketStartTimeBetween(Action action, String origin, String brandId, String categoryId, Long startTime, Long endTime);
}