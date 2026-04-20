package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.UserCashbackMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCashbackMonthlyRepository extends JpaRepository<UserCashbackMonthly,Long> {
    Optional<UserCashbackMonthly> findByUserIdAndMonth(Long userId, String month);

}
