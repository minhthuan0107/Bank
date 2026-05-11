package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.projection.CashFlowProjection;
import com.example.bank.repository.projection.DepositDashboardProjection;
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
public interface DepositOrderRepository extends JpaRepository<DepositOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DepositOrder> findByOrderNoAndUserId(String orderNo, Long userId);


    Page<DepositOrder> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT d
            FROM DepositOrder d
            WHERE d.userId = :userId
            
              AND (:orderNo IS NULL OR d.orderNo LIKE %:orderNo%)
            
              AND (:address IS NULL OR LOWER(d.address) LIKE LOWER(CONCAT('%', :address, '%')))
            
              AND (:status IS NULL OR d.status = :status)
            
              AND (:fromTime IS NULL OR d.createdAt >= :fromTime)
            
              AND (:toTime IS NULL OR d.createdAt <= :toTime)
            """)
    Page<DepositOrder> search(
            Long userId,
            String orderNo,
            String address,
            DepositOrderStatus status,
            Instant fromTime,
            Instant toTime,
            Pageable pageable
    );

    @Query("""
            SELECT 
                COALESCE(SUM(d.amount), 0) as totalAmount,
                COALESCE(SUM(CASE WHEN d.status = 'PENDING' THEN 1 ELSE 0 END), 0) as pendingCount,
                COALESCE(SUM(CASE WHEN d.status = 'SUCCESS' THEN 1 ELSE 0 END), 0) as successCount,
                COALESCE(SUM(CASE WHEN d.status = 'FAILED' THEN 1 ELSE 0 END), 0) as failedCount
            FROM DepositOrder d
            WHERE d.userId = :userId
            """)
    DepositDashboardProjection getDashboard(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT d
            FROM DepositOrder d
            WHERE d.orderNo = :orderNo
            """)
    Optional<DepositOrder> findByOrderNoForUpdate(String orderNo);


    @Query("""
        SELECT d
        FROM DepositOrder d
        JOIN User u ON u.id = d.userId
        WHERE (
                :orderNo IS NULL
                OR LOWER(d.orderNo) LIKE LOWER(CONCAT('%', :orderNo, '%'))
        )
        AND (
                :username IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
        )
        AND (
                :status IS NULL
                OR d.status = :status
        )
        AND (
                :fromTime IS NULL
                OR d.createdAt >= :fromTime
        )
        AND (
                :toTime IS NULL
                OR d.createdAt <= :toTime
        )
        """)
    Page<DepositOrder> searchAdminDepositOrders(
            @Param("orderNo") String orderNo,
            @Param("username") String username,
            @Param("status") DepositOrderStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(d)
        FROM DepositOrder d
        WHERE d.status = com.example.bank.enums.wallet.DepositOrderStatus.PENDING
        """)
    long countPendingDeposits();

    @Query("""
        SELECT COALESCE(SUM(d.expectedAmount), 0)
        FROM DepositOrder d
        WHERE d.status = com.example.bank.enums.wallet.DepositOrderStatus.PENDING
        """)
    BigDecimal sumPendingDepositAmount();

    @Query(value = """
        SELECT DAYOFWEEK(d.updated_at) AS groupKey,
               COALESCE(SUM(d.expected_amount), 0) AS amount
        FROM deposit_orders d
        WHERE d.status = 'SUCCESS'
          AND d.updated_at >= :start
          AND d.updated_at < :end
        GROUP BY DAYOFWEEK(d.updated_at)
        """, nativeQuery = true)
    List<CashFlowProjection> sumDepositByWeek(
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(value = """
        SELECT DAY(d.updated_at) AS groupKey,
               COALESCE(SUM(d.expected_amount), 0) AS amount
        FROM deposit_orders d
        WHERE d.status = 'SUCCESS'
          AND d.updated_at >= :start
          AND d.updated_at < :end
        GROUP BY DAY(d.updated_at)
        """, nativeQuery = true)
    List<CashFlowProjection> sumDepositByMonth(
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(value = """
        SELECT MONTH(d.updated_at) AS groupKey,
               COALESCE(SUM(d.expected_amount), 0) AS amount
        FROM deposit_orders d
        WHERE d.status = 'SUCCESS'
          AND d.updated_at >= :start
          AND d.updated_at < :end
        GROUP BY MONTH(d.updated_at)
        """, nativeQuery = true)
    List<CashFlowProjection> sumDepositByYear(
            @Param("start") Instant start,
            @Param("end") Instant end
    );


    @Query(value = """
    SELECT DAYOFWEEK(d.updated_at) AS groupKey,
           COALESCE(SUM(d.expected_amount), 0) AS amount
    FROM deposit_orders d
    WHERE d.user_id = :userId
      AND d.status = 'SUCCESS'
      AND d.updated_at >= :start
      AND d.updated_at < :end
    GROUP BY DAYOFWEEK(d.updated_at)
    """, nativeQuery = true)
    List<CashFlowProjection> sumUserDepositByWeek(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(value = """
    SELECT DAY(d.updated_at) AS groupKey,
           COALESCE(SUM(d.expected_amount), 0) AS amount
    FROM deposit_orders d
    WHERE d.user_id = :userId
      AND d.status = 'SUCCESS'
      AND d.updated_at >= :start
      AND d.updated_at < :end
    GROUP BY DAY(d.updated_at)
    """, nativeQuery = true)
    List<CashFlowProjection> sumUserDepositByMonth(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(value = """
    SELECT MONTH(d.updated_at) AS groupKey,
           COALESCE(SUM(d.expected_amount), 0) AS amount
    FROM deposit_orders d
    WHERE d.user_id = :userId
      AND d.status = 'SUCCESS'
      AND d.updated_at >= :start
      AND d.updated_at < :end
    GROUP BY MONTH(d.updated_at)
    """, nativeQuery = true)
    List<CashFlowProjection> sumUserDepositByYear(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

}
