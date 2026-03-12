package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepositSettingsRepository extends JpaRepository<DepositSettings, Long> {

    Optional<DepositSettings> findByCurrency(String currency);

}