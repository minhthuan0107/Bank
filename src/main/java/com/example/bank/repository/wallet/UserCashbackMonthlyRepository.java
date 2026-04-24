package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.UserCashbackMonthly;
import com.example.bank.enums.wallet.CashbackStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCashbackMonthlyRepository extends JpaRepository<UserCashbackMonthly,Long> {
    Optional<UserCashbackMonthly> findByUserIdAndMonth(Long userId, String month);

    List<UserCashbackMonthly> findByUserIdInAndMonth(List<Long> userIds, String month);

    @Query("""
    SELECT m
    FROM UserCashbackMonthly m
    WHERE m.month = :month
    AND m.status = :status
    ORDER BY m.totalSpent DESC, m.userId DESC
    """)
    Page<UserCashbackMonthly> findByMonthAndStatus(
            @Param("month") String month,
            @Param("status") CashbackStatus status,
            Pageable pageable
    );

    Page<UserCashbackMonthly> findByUserIdAndStatus(
            Long userId,
            CashbackStatus status,
            Pageable pageable
    );

}
