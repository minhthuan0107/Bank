package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositAddress;
import com.example.bank.enums.wallet.Stablecoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface DepositAddressRepository extends JpaRepository<DepositAddress, Long> {

    Optional<DepositAddress> findByCurrencyAndNetwork(Stablecoin currency, String network);

    Optional<DepositAddress> findFirstByCurrencyAndNetworkAndStatus(
            Stablecoin currency,
            String network,
            String status
    );
}
