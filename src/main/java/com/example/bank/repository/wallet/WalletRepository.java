package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserIdAndCurrency(Long userId, Stablecoin currency);

}
