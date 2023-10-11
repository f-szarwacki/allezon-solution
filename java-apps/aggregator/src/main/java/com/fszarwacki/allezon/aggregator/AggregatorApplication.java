package com.fszarwacki.allezon.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AggregatorApplication {

    private static final Logger log = LoggerFactory.getLogger(AggregatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AggregatorApplication.class, args);
    }

}