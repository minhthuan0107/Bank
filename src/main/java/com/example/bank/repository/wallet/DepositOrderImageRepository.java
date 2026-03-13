package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositOrderImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositOrderImageRepository extends JpaRepository<DepositOrderImage, Long> {

    boolean existsByDepositOrderId(Long depositOrderId);

}
