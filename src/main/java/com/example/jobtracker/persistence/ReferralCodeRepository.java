package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.ReferralCode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, String> {

    /**
     * Inserts the code unless it already exists, in one atomic statement.
     *
     * @return 1 if inserted, 0 if the code was already taken
     */
    @Transactional
    @Modifying
    @Query(value = """
            insert into referral_codes (code, company_id, created_at, expires_at)
            values (:code, :companyId, :createdAt, :expiresAt)
            on conflict (code) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("code") String code, @Param("companyId") UUID companyId,
                       @Param("createdAt") LocalDateTime createdAt, @Param("expiresAt") LocalDateTime expiresAt);

    @Query("select r.companyId from ReferralCode r where r.code = :code and r.expiresAt > :now")
    Optional<UUID> findActiveCompanyId(@Param("code") String code, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("delete from ReferralCode r where r.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
