package com.example.jobtracker.service;

import com.example.jobtracker.config.CacheConfig;
import com.example.jobtracker.controller.dto.RandomValueResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RandomValueService {

    private final Clock clock;

    public RandomValueService(Clock clock) {
        this.clock = clock;
    }

    @Cacheable(cacheNames = CacheConfig.RANDOM_VALUE_CACHE, cacheManager = "caffeineCacheManager")
    public RandomValueResponse getR() {
        int r = ThreadLocalRandom.current().nextInt(1_000_000);
        RandomValueResponse response = new RandomValueResponse(r, LocalDateTime.now(clock));
        log.info("Caffeine cache miss for 'r' - generated new value r={} at {}", response.r(), response.generatedAt());
        return response;
    }
}
