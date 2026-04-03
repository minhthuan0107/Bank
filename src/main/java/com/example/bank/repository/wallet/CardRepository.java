package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.Card;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    long countByUserId(Long userId);

    @Modifying
    @Query("""
                UPDATE Card c
                SET c.last4 = :last4,
                    c.expMonth = :expMonth,
                    c.expYear = :expYear
                WHERE c.id = :cardId
            """)
    void updateCardBasicInfo(
            @Param("cardId") Long cardId,
            @Param("last4") String last4,
            @Param("expMonth") Integer expMonth,
            @Param("expYear") Integer expYear
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE Card c
                SET c.allocatedAmount = c.allocatedAmount + :amount,
                    c.remainingAmount = c.remainingAmount + :amount
                WHERE c.id = :cardId
            """)
    int increaseLimit(@Param("cardId") Long cardId,
                      @Param("amount") BigDecimal amount);

    @Modifying
    @Query("""
            UPDATE Card c
            SET c.allocatedAmount = c.allocatedAmount - :amount,
                c.remainingAmount = c.remainingAmount - :amount
            WHERE c.id = :cardId
            """)
    int decreaseLimit(Long cardId, BigDecimal amount);

    Optional<Card> findByIdAndUserId(Long id, Long userId);

    Page<Card> findByUserId(Long userId, Pageable pageable);

    @Query("""
    SELECT COALESCE(SUM(c.remainingAmount), 0)
    FROM Card c
    WHERE c.userId = :userId
      AND c.status = 'ACTIVE'
    """)
    BigDecimal sumRemainingAmountByUserId(@Param("userId") Long userId);
}
