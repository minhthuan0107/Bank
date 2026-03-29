package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.Stablecoin;
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



}
