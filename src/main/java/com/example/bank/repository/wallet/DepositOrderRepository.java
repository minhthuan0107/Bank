package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositOrderRepository extends JpaRepository<DepositOrder, Long> {
}
