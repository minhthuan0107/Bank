package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.WalletSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface WalletSettingsRepository extends JpaRepository<WalletSettings, Long> {

    Optional<WalletSettings> findByIdAndStatus(Long id, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM WalletSettings s
            WHERE s.id = :id
            """)
    Optional<WalletSettings> findByIdForUpdate(
            @Param("id") Long id
    );

}