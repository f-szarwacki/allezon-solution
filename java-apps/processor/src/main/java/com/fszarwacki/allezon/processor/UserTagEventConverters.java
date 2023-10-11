package com.fszarwacki.allezon.processor;

import com.fszarwacki.allezon.common.Device;
import com.fszarwacki.allezon.common.Action;
import com.fszarwacki.allezon.common.Product;
import com.fszarwacki.allezon.common.UserTagEvent;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.aerospike.convert.AerospikeReadData;
import org.springframework.data.aerospike.convert.AerospikeWriteData;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class UserTagEventConverters {

    @WritingConverter
    @RequiredArgsConstructor
    public static class UserTagEventToAerospikeWriteDataConverter implements Converter<UserTagEvent, AerospikeWriteData> {
        private static final int NEVER_EXPIRE = -1;
        private final String namespace;
        private final String setName;

        @Override
        public AerospikeWriteData convert(UserTagEvent source) {
            Key key = new Key(namespace, setName, source.getTimeInMiliseconds());
            int expiration = NEVER_EXPIRE;
            Integer version = null; // not versionable document
            Collection<Bin> bins = List.of(
                    new Bin("time_mili", source.getTimeInMiliseconds()),
                    new Bin("cookie", source.getCookie()),
                    new Bin("country", source.getCountry()),
                    new Bin("action", source.getAction()),
                    new Bin("device", source.getDevice()),
                    new Bin("origin", source.getOrigin()),
                    new Bin("brand", source.getProductInfo().getBrandId()),
                    new Bin("category", source.getProductInfo().getCategoryId()),
                    new Bin("price", source.getProductInfo().getPrice()),
                    new Bin("product", source.getProductInfo().getProductId()),
                    new Bin("price", source.getProductInfo().getPrice())
            );
            return new AerospikeWriteData(key, bins, expiration, version);
        }
    }

    @ReadingConverter
    public enum AerospikeReadDataToUserTagEventConverter implements Converter<AerospikeReadData, UserTagEvent> {
        INSTANCE;

        @Override
        public UserTagEvent convert(AerospikeReadData source) {
            String time = Instant.ofEpochMilli((Long) source.getValue("time_mili")).toString();
            String cookie = (String) source.getValue("cookie");
            String country = (String) source.getValue("country");
            Device device = Device.valueOf((String) source.getValue("device"));
            Action action = Action.valueOf((String) source.getValue("action"));
            String origin = (String) source.getValue("origin");
            Product productInfo = new Product(
                (Long) source.getValue("product"),
                (String) source.getValue("brand"),
                (String) source.getValue("category"),
                (Long) source.getValue("price")
            );
            Long timeInMiliseconds = (Long) source.getValue("time_mili");

            return new UserTagEvent(
                time,
                cookie,
                country,
                device,
                action,
                origin,
                productInfo,
                timeInMiliseconds
            );
        }
    }
}