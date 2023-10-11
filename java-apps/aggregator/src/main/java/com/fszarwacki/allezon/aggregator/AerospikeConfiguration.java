package com.fszarwacki.allezon.aggregator;

import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import com.fszarwacki.allezon.common.AggregatesBucketConverters;
import com.fszarwacki.allezon.common.AggregatesBucketRepository;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.aerospike.config.AbstractAerospikeDataConfiguration;
import org.springframework.data.aerospike.repository.config.EnableAerospikeRepositories;

import java.util.Collection;
import java.util.List;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@EnableScheduling
@Configuration
@EnableAerospikeRepositories(basePackageClasses = { AggregatesBucketRepository.class})
@EnableConfigurationProperties(AerospikeConfiguration.AerospikeConfigurationProperties.class)
public class AerospikeConfiguration extends AbstractAerospikeDataConfiguration {

    @Autowired
    AerospikeConfigurationProperties properties;

    @Override
    protected Collection<Host> getHosts() {
        return Host.parseServiceHosts(properties.getHosts());
    }

    @Override
    protected String nameSpace() {
        return properties.getNamespace();
    }

    @Override
    protected ClientPolicy getClientPolicy() {
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.failIfNotConnected = false;
        clientPolicy.timeout = 10_000;
        clientPolicy.writePolicyDefault.sendKey = false;
        clientPolicy.maxConnsPerNode = 500;
        return clientPolicy;
    }

    @Data
    @ConfigurationProperties("aerospike")
    public static class AerospikeConfigurationProperties {
        String hosts;
        String namespace;
    }

    @Override
    protected List<?> customConverters() {
        return List.of(
            AggregatesBucketConverters.AerospikeReadDataToAggregatesBucketConverter.INSTANCE,
            new AggregatesBucketConverters.AggregatesBucketToAerospikeWriteDataConverter(properties.getNamespace(), "aggregates-buckets")
        );
    }
}

