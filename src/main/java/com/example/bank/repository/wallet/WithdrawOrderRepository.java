package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.repository.projection.WithdrawDashboardProjection;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawOrderRepository extends JpaRepository<WithdrawOrder, Long> {
    @Query("SELECT w " +
            "FROM WithdrawOrder w " +
            "WHERE w.userId = :userId " +
            "AND w.status IN :statuses " +
            "ORDER BY w.createdAt DESC"
    )
    Optional<WithdrawOrder> findFirstByUserIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") List<WithdrawOrderStatus> statuses
    );

    boolean existsByUserIdAndStatusAndCreatedAtBetween(
            Long userId,
            WithdrawOrderStatus status,
            Instant start,
            Instant end
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM WithdrawOrder w
            WHERE w.userId = :userId
            AND w.orderNo = :orderNo
            """)
    Optional<WithdrawOrder> findByOrderNoAndUserIdForUpdate(
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId
    );

    Page<WithdrawOrder> findByUserId(Long userId, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT w
                FROM WithdrawOrder w
                WHERE w.orderNo = :orderNo
            """)
    Optional<WithdrawOrder> findByOrderNoForUpdate(String orderNo);


    @Query("""
                SELECT w FROM WithdrawOrder w
                WHERE w.status IN :statuses
                ORDER BY w.createdAt DESC
            """)
    Page<WithdrawOrder> findAdminOrders(
            @Param("statuses") List<WithdrawOrderStatus> statuses,
            Pageable pageable
    );

    Optional<WithdrawOrder> findByUserIdAndStatus(
            Long userId,
            WithdrawOrderStatus status
    );

    @Query("""
            SELECT w
            FROM WithdrawOrder w
            WHERE w.userId = :userId
              AND (:orderNo IS NULL OR w.orderNo LIKE %:orderNo%)
              AND (:address IS NULL OR LOWER(w.toAddress) LIKE LOWER(CONCAT('%', :address, '%')))
              AND (:status IS NULL OR w.status = :status)
              AND (:fromTime IS NULL OR w.createdAt >= :fromTime)
              AND (:toTime IS NULL OR w.createdAt <= :toTime)
            """)
    Page<WithdrawOrder> search(
            Long userId,
            String orderNo,
            String address,
            WithdrawOrderStatus status,
            Instant fromTime,
            Instant toTime,
            Pageable pageable
    );

    @Query("""
            SELECT 
                COALESCE(SUM(w.amount), 0) as totalAmount,
                COALESCE(SUM(CASE WHEN w.status = 'SUCCESS' THEN 1 ELSE 0 END), 0) as successCount,
                COALESCE(SUM(CASE WHEN w.status = 'FAILED' THEN 1 ELSE 0 END), 0) as failedCount
            FROM WithdrawOrder w
            WHERE w.userId = :userId
            """)
    WithdrawDashboardProjection getDashboard(Long userId);
}
