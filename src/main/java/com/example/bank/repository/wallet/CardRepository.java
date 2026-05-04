package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.Card;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.projection.CardDashboardProjection;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
                SET c.cardLimit = c.cardLimit + :amount,
                    c.remainingAmount = c.remainingAmount + :amount
                WHERE c.id = :cardId
            """)
    int increaseLimit(@Param("cardId") Long cardId,
                      @Param("amount") BigDecimal amount);

    @Modifying
    @Query("""
            UPDATE Card c
            SET c.cardLimit = c.cardLimit - :amount,
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

    // ===== LẤY CARD + LOCK (QUAN TRỌNG) =====
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT c
                FROM Card c
                WHERE c.id = :cardId
                AND c.userId = :userId
            """)
    Optional<Card> findByIdAndUserIdForUpdate(
            @Param("cardId") Long cardId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT c
            FROM Card c
            WHERE c.userId = :userId
              AND (:cardNumber IS NULL OR c.last4 LIKE %:cardNumber%)
              AND (:cardName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :cardName, '%')))
              AND (:status IS NULL OR c.status = :status)
              AND (:fromTime IS NULL OR c.createdAt >= :fromTime)
              AND (:toTime IS NULL OR c.createdAt <= :toTime)
            """)
    Page<Card> searchEntity(
            Long userId,
            String cardNumber,
            String cardName,
            CardStatus status,
            Instant fromTime,
            Instant toTime,
            Pageable pageable
    );

    @Query("""
            SELECT 
            COALESCE(SUM(c.remainingAmount), 0) as totalBalance,
            COALESCE(SUM(CASE WHEN c.status = 'ACTIVE' THEN 1 ELSE 0 END), 0) as activeCount,
            COALESCE(SUM(CASE WHEN c.status = 'BLOCKED' THEN 1 ELSE 0 END), 0) as blockedCount
            FROM Card c
            WHERE c.userId = :userId
            """)
    CardDashboardProjection getDashboard(@Param("userId") Long userId);


    @Query("""
            SELECT 
                COALESCE(SUM(CASE WHEN c.status = 'ACTIVE' THEN 1 ELSE 0 END), 0)
            FROM Card c
            WHERE c.userId = :userId
            """)
    Long getActivatedCardCount(Long userId);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.slashCardId = :slashCardId")
    Optional<Card> findBySlashCardIdForUpdate(@Param("slashCardId") String slashCardId);


    @Query("""
        SELECT c
        FROM Card c
        JOIN User u ON u.id = c.userId
        WHERE (:cardNumber IS NULL OR c.last4 LIKE CONCAT('%', :cardNumber, '%'))
          AND (:cardName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :cardName, '%')))
          AND (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:status IS NULL OR c.status = :status)
          AND (:fromTime IS NULL OR c.createdAt >= :fromTime)
          AND (:toTime IS NULL OR c.createdAt <= :toTime)
        """)
    Page<Card> searchAdminEntity(
            @Param("cardNumber") String cardNumber,
            @Param("cardName") String cardName,
            @Param("username") String username,
            @Param("status") CardStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Card c
        WHERE c.id = :cardId
        """)
    Optional<Card> findByIdForUpdate(@Param("cardId") Long cardId);

    @Query("""
            SELECT c.id
            FROM Card c
            WHERE c.userId = :userId
              AND c.status = :status
            """)
    List<Long> findIdsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") CardStatus status
    );



}
