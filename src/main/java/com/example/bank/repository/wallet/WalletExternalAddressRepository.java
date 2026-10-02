package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.WalletExternalAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletExternalAddressRepository
        extends JpaRepository<WalletExternalAddress, Long> {

    boolean existsByWalletId(Long walletId);

    Optional<WalletExternalAddress> findByWalletId(Long walletId);

    @Query("""
            SELECT wea
            FROM WalletExternalAddress wea
            JOIN Wallet w
                ON w.id = wea.walletId
            WHERE w.userId = :userId
            """)
    Optional<WalletExternalAddress> findByUserId(
            @Param("userId") Long userId
    );
}