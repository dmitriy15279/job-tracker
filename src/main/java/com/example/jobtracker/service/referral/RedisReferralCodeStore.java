package com.example.jobtracker.service.referral;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ConditionalOnProperty(name = "referral.storage", havingValue = "redis")
public class RedisReferralCodeStore implements ReferralCodeStore {

    private static final String KEY_PREFIX = "referral:code:";

    private final StringRedisTemplate redisTemplate;

    public RedisReferralCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("Referral codes are stored in Redis (expired by key TTL)");
    }

    @Override
    public boolean saveIfAbsent(String code, UUID companyId, LocalDateTime createdAt, Duration ttl) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + code, companyId.toString(), ttl));
    }

    @Override
    public Optional<UUID> findCompanyId(String code) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + code)).map(UUID::fromString);
    }
}
