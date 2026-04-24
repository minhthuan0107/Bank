package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.repository.UserSpentProjection;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    boolean existsBySlashEventId(String slashEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM CardTransaction t WHERE t.slashTransactionId = :txId")
    Optional<CardTransaction> findBySlashTransactionIdForUpdate(@Param("txId") String txId);


    @Query("""
                SELECT COALESCE(SUM(t.amount), 0)
                FROM CardTransaction t
                WHERE t.userId = :userId
                AND t.status IN ('PENDING', 'POSTED')
                AND t.createdAt >= :start
                AND t.createdAt < :end
            """)
    BigDecimal sumByUserAndMonth(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
                SELECT t.userId AS userId, SUM(t.amount) AS totalSpent
                FROM CardTransaction t
                WHERE t.userId IN :userIds
                AND t.updatedAt >= :start
                AND t.updatedAt < :end
                AND t.status IN ('PENDING','POSTED')
                GROUP BY t.userId
            """)
    List<UserSpentProjection> sumByUserIds(
            List<Long> userIds,
            Instant start,
            Instant end
    );

    @Query("""
                SELECT t.userId AS userId, SUM(t.amount) AS totalSpent
                FROM CardTransaction t
                WHERE t.updatedAt >= :start
                  AND t.updatedAt < :end
                  AND t.status IN ('PENDING','POSTED')
                GROUP BY t.userId
            """)
    List<UserSpentProjection> sumAllUsers(
            @Param("start") Instant start,
            @Param("end") Instant end
    );

}
