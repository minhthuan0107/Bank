package com.example.bank.repository.auth;

import com.example.bank.entity.auth.AuthSession;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    // Tìm phiên theo sessionId và userId
    Optional<AuthSession> findByIdAndUserId (Long sessionId, Long userId);

    // Update lastUsedAt khi dùng refresh token
    @Modifying
    @Query("UPDATE AuthSession s SET s.lastUsedAt = :now WHERE s.id = :id")
    void updateLastUsedAtById(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AuthSession s
        SET s.isRevoked = true,
            s.revokedAt = :revokedAt
        WHERE s.user.id = :userId
          AND (s.isRevoked = false OR s.isRevoked IS NULL)
        """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("revokedAt") Instant revokedAt
    );


}
