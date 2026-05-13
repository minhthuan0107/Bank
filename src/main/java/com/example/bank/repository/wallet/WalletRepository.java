package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.projection.WalletAssetAllocationProjection;
import com.example.bank.repository.projection.WalletSummaryProjection;
import com.example.bank.repository.projection.WithdrawSummaryProjection;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserIdAndCurrency(Long userId, Stablecoin currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM Wallet w
            WHERE w.userId = :userId
            """)
    Optional<Wallet> findByUserIdForUpdate(
            @Param("userId") Long userId
    );


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Wallet w
            SET w.availableBalance = w.availableBalance - :amount,
                w.allocatedBalance = w.allocatedBalance + :amount
            WHERE w.userId = :userId
              AND w.availableBalance >= :amount
            """)
    int decreaseBalance(@Param("userId") Long userId,
                        @Param("amount") BigDecimal amount);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Wallet w
            SET w.availableBalance = w.availableBalance + :amount,
                w.allocatedBalance = w.allocatedBalance - :amount
            WHERE w.userId = :userId
            """)
    int increaseBalance(@Param("userId") Long userId,
                        @Param("amount") BigDecimal amount
    );

    @Query("""
            SELECT w.availableBalance
            FROM Wallet w
            WHERE w.userId = :userId
            """)
    Optional<BigDecimal> findAvailableBalanceByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT 
                w.totalBalance as totalBalance,
                w.allocatedBalance as allocatedBalance,
                w.frozenBalance as frozenBalance,
                w.availableBalance as availableBalance
            FROM Wallet w
            WHERE w.userId = :userId
            """)
    WalletSummaryProjection getWalletSummary(Long userId);

    @Query("""
        SELECT COALESCE(SUM(w.totalBalance), 0)
        FROM Wallet w
        """)
    BigDecimal sumTotalWalletBalance();


    @Query(value = """
        SELECT 
            COALESCE(w.total_balance, 0) AS totalBalance,
            COALESCE(w.allocated_balance, 0) AS allocatedBalance,
            COALESCE(w.frozen_balance, 0) AS frozenBalance,
            COALESCE(w.available_balance, 0) AS availableBalance
        FROM wallets w
        WHERE w.user_id = :userId
          AND w.status = 'ACTIVE'
        LIMIT 1
        """, nativeQuery = true)
    Optional<WalletAssetAllocationProjection> findAssetAllocationByUserId(
            @Param("userId") Long userId
    );

    @Query(value = """
    SELECT 
        w.id AS walletId,
        COALESCE(w.available_balance, 0) AS balance,
        COALESCE(s.min_withdraw_amount, 0) AS minimumWithdrawalAmount,
        w.currency AS currency
    FROM wallets w
    JOIN wallet_currency_settings s 
        ON s.currency = w.currency
       AND s.status = 'ACTIVE'
    WHERE w.user_id = :userId
      AND w.status = 'ACTIVE'
    LIMIT 1
    """, nativeQuery = true)
    Optional<WithdrawSummaryProjection>  findWithdrawSummaryByUserId(
            @Param("userId") Long userId
    );
}
