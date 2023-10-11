package com.fszarwacki.allezon.common;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.aerospike.convert.AerospikeReadData;
import org.springframework.data.aerospike.convert.AerospikeWriteData;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.Collection;
import java.util.List;

public class AggregatesBucketConverters {

    @WritingConverter
    @RequiredArgsConstructor
    public static class AggregatesBucketToAerospikeWriteDataConverter implements Converter<AggregatesBucket, AerospikeWriteData> {
        private static final int NEVER_EXPIRE = -1;
        private final String namespace;
        private final String setName;

        @Override
        public AerospikeWriteData convert(AggregatesBucket source) {
            Key key = new Key(namespace, setName, source.getId());
            int expiration = NEVER_EXPIRE;
            Integer version = null; // not versionable document
            Collection<Bin> bins = List.of(
                    new Bin("1m_bucket", source.getBucketStartTime()),
                    new Bin("action", source.getAction()),
                    new Bin("origin", source.getOrigin()),
                    new Bin("brand_id", source.getBrandId()),
                    new Bin("category_id", source.getCategoryId()),
                    new Bin("sum_price", source.getSumPrice()),
                    new Bin("count", source.getCount())
            );
            return new AerospikeWriteData(key, bins, expiration, version);
        }
    }

    @ReadingConverter
    public enum AerospikeReadDataToAggregatesBucketConverter implements Converter<AerospikeReadData, AggregatesBucket> {
        INSTANCE;

        @Override
        public AggregatesBucket convert(AerospikeReadData source) {
            Long bucketStartTime = (Long) source.getValue("1m_bucket");
            Action action = Action.valueOf((String) source.getValue("action"));
            String origin = (String) source.getValue("origin");
            Long count = (Long) source.getValue("count");
            String brandId = (String) source.getValue("brand_id");
            String categoryId = (String) source.getValue("category_id");
            Long sumPrice = (Long) source.getValue("sum_price");

            return new AggregatesBucket(
                null,
                bucketStartTime,
                action,
                origin,
                brandId,
                categoryId,
                count,
                sumPrice
            );
        }
    }
}