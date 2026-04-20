package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CashbackRule;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashbackRuleRepository extends JpaRepository<CashbackRule,Long> {
    boolean existsByCashbackPercentAndIsActiveTrue(BigDecimal cashbackPercent);

    boolean existsByCashbackPercentAndIsActiveTrueAndIdNot(
            BigDecimal cashbackPercent,
            Long id
    );

    List<CashbackRule> findAllByIsActiveTrueOrderByMinSpentAsc();


    @Query("""
        SELECT r FROM CashbackRule r
        WHERE r.isActive = true
        AND :spent >= r.minSpent
        AND (:spent < r.maxSpent OR r.maxSpent IS NULL)
        ORDER BY r.minSpent DESC
    """)
    Optional<CashbackRule> findMatchedRule(@Param("spent") BigDecimal spent);


}
