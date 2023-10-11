package com.fszarwacki.allezon.processor;

import org.springframework.data.aerospike.repository.AerospikeRepository;

import com.fszarwacki.allezon.common.UserTagEvent;

import java.util.List;


public interface UserTagEventRepository extends AerospikeRepository<UserTagEvent, String> {
    List<UserTagEvent> findByCookieAndTimeInMilisecondsBetweenOrderByTimeInMilisecondsDesc(String cookie, Long startTimeMiliseconds, Long endTimeMiliseconds);
}