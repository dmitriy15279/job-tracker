package com.example.jobtracker.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String JOB_APPLICATIONS_CACHE = "jobApplications";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    public CacheConfig() {
        log.info("Configuring Redis cache '{}' with TTL={}s", JOB_APPLICATIONS_CACHE, CACHE_TTL.getSeconds());
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJacksonJsonRedisSerializer.builder().build()));

        return builder -> builder.withCacheConfiguration(JOB_APPLICATIONS_CACHE, configuration);
    }
}
