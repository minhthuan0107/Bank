package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositSettings;
import com.example.bank.enums.wallet.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepositSettingsRepository extends JpaRepository<DepositSettings, Long> {

    Optional<DepositSettings> findByCurrency(Stablecoin currency);

    Optional<DepositSettings> findByCurrencyAndStatus(Stablecoin currency, String status);

}