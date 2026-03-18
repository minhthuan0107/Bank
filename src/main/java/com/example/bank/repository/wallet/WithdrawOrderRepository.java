package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawOrderRepository extends JpaRepository<WithdrawOrder, Long> {
    boolean existsByUserIdAndStatusIn(
            Long userId,
            List<WithdrawOrderStatus> statuses
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


}
