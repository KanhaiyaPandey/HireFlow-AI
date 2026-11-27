package com.hireflow.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.auth.entity.AccountToken;
import com.hireflow.auth.entity.TokenPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    /** Locked so a link clicked twice at the same moment is used only once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<AccountToken> findByTokenHashForUpdate(String tokenHash);

    /** Issuing a new link kills older unused links of the same kind: only the latest email works. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update AccountToken t set t.usedAt = :now
            where t.user.id = :userId and t.purpose = :purpose and t.usedAt is null
            """)
    int invalidateOutstanding(UUID userId, TokenPurpose purpose, Instant now);
}
