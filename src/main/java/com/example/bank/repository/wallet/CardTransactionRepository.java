package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.projection.UserSpentProjection;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                WHERE t.updatedAt >= :start
                  AND t.updatedAt < :end
                  AND t.status IN ('PENDING','POSTED')
                GROUP BY t.userId
            """)
    List<UserSpentProjection> sumAllUsers(
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    Page<CardTransaction> findByUserId(
            Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT ct
        FROM CardTransaction ct
        WHERE ct.userId = :userId

          AND (
                :slashTransactionId IS NULL
                OR LOWER(ct.slashTransactionId) LIKE LOWER(CONCAT('%', :slashTransactionId, '%'))
          )

          AND (
                :merchantDescription IS NULL
                OR LOWER(ct.merchantDescription) LIKE LOWER(CONCAT('%', :merchantDescription, '%'))
          )

          AND (
                :status IS NULL
                OR ct.status = :status
          )

          AND (
                :fromTime IS NULL
                OR ct.updatedAt >= :fromTime
          )

          AND (
                :toTime IS NULL
                OR ct.updatedAt <= :toTime
          )
        """)
    Page<CardTransaction> search(
            @Param("userId") Long userId,
            @Param("slashTransactionId") String slashTransactionId,
            @Param("merchantDescription") String merchantDescription,
            @Param("status") CardTransactionStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );

}
