package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepositOrderRepository extends JpaRepository<DepositOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DepositOrder> findByOrderNoAndUserId(String orderNo, Long userId);


    Page<DepositOrder> findByUserId(Long userId, Pageable pageable);
}
