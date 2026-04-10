package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.repository.projection.DepositDashboardProjection;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
}
