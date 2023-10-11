package com.fszarwacki.allezon.common;

import org.springframework.data.aerospike.mapping.Document;
import org.springframework.data.aerospike.mapping.Field;
import org.springframework.data.annotation.Id;

import com.aerospike.client.query.IndexType;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.springframework.data.aerospike.annotation.Indexed;

@Getter
@Setter
@Document(collection = "aggregates-buckets")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AggregatesBucket {
  @Id
  private String id;

  @Field("1m_bucket")
  @JsonProperty("1m_bucket")
  @Indexed(name = "1m_bucket_idx", type=IndexType.NUMERIC)
  private Long bucketStartTime;

  @Indexed(name = "action_idx", type=IndexType.STRING)
  private Action action;
  @Indexed(name = "origin_idx", type=IndexType.STRING)
  private String origin;

  @Field("brand_id")
  @JsonProperty("brand_id")
  @Indexed(name = "brand_idx", type=IndexType.STRING)
  private String brandId;

  @Field("category_id")
  @JsonProperty("category_id")
  @Indexed(name = "category_idx", type=IndexType.STRING)
  private String categoryId;

  private Long count;

  @Field("sum_price")
  @JsonProperty("sum_price")
  private Long sumPrice;

  public AggregatesBucket(AggregatesBucket example, Long bucketStartTime) {
    this.bucketStartTime = bucketStartTime;
    this.action = example.action;
    this.origin = example.origin;
    this.brandId = example.brandId;
    this.categoryId = example.categoryId;
    this.count = 0l;
    this.sumPrice = 0l;
  }

  public static AggregatesBucket join(AggregatesBucket a, AggregatesBucket b) {
    return new AggregatesBucket(
      null,
      a.getBucketStartTime(),
      a.getAction(),
      (a.getOrigin() != null && b.getOrigin() != null && a.getOrigin().equals(b.getOrigin())) ? a.getOrigin() : null,
      (a.getBrandId() != null && b.getBrandId() != null && a.getBrandId().equals(b.getBrandId())) ? a.getBrandId() : null,
      (a.getCategoryId() != null && b.getCategoryId() != null && a.getCategoryId().equals(b.getCategoryId())) ? a.getCategoryId() : null,
      a.getCount() + b.getCount(),
      a.getSumPrice() + b.getSumPrice()
    );
  }

  public AggregatesBucket(Action action2, String origin2, String brandId2, String categoryId2) {
    this.action = action2;
    this.origin = origin2;
    this.brandId = brandId2;
    this.categoryId = categoryId2;
  }
}
