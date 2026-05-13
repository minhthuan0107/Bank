package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.WalletCurrencySettings;
import com.example.bank.enums.wallet.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletCurrencySettingsRepository extends JpaRepository<WalletCurrencySettings, Long> {

    Optional<WalletCurrencySettings> findByCurrency(Stablecoin currency);

    Optional<WalletCurrencySettings> findByCurrencyAndStatus(Stablecoin currency, String status);

    boolean existsByCurrencyAndIdNot(Stablecoin currency, Long id);

}