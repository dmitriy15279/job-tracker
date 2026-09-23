package com.example.jobtracker.service.referral;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage for referral codes. Exactly one implementation is active, selected by {@code referral.storage}.
 */
public interface ReferralCodeStore {

    /**
     * Stores the code for the company unless the code is already taken.
     *
     * @return {@code false} if the code already exists (caller should generate another one)
     */
    boolean saveIfAbsent(String code, UUID companyId, LocalDateTime createdAt, Duration ttl);

    /**
     * @return the company the code belongs to, or empty if the code is unknown or expired
     */
    Optional<UUID> findCompanyId(String code);
}
