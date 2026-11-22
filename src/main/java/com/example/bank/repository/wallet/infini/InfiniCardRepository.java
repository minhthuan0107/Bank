package com.example.bank.repository.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InfiniCardRepository extends JpaRepository<InfiniCard, Long> {

    long countByUserId(Long userId);

    // Admin search across all users. cardNumber matches the last four digits.
    @Query("""
        SELECT c
        FROM InfiniCard c
        JOIN User u ON u.id = c.userId
        WHERE (:cardNumber IS NULL OR c.last4 LIKE CONCAT('%', :cardNumber, '%'))
          AND (:cardName IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :cardName, '%')))
          AND (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:status IS NULL OR c.status = :status)
          AND (:fromTime IS NULL OR c.createdAt >= :fromTime)
          AND (:toTime IS NULL OR c.createdAt <= :toTime)
        """)
    Page<InfiniCard> searchAdmin(@Param("cardNumber") String cardNumber,
                                 @Param("cardName") String cardName,
                                 @Param("username") String username,
                                 @Param("status") CardStatus status,
                                 @Param("fromTime") java.time.Instant fromTime,
                                 @Param("toTime") java.time.Instant toTime,
                                 Pageable pageable);

    @Query("select c.id from InfiniCard c where c.userId = :userId and c.status = :status")
    java.util.List<Long> findIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CardStatus status);

    long countByUserIdAndStatus(Long userId, CardStatus status);

    Page<InfiniCard> findByUserId(Long userId, Pageable pageable);

    Optional<InfiniCard> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InfiniCard c where c.id = :id and c.userId = :userId")
    Optional<InfiniCard> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InfiniCard c where c.id = :id")
    Optional<InfiniCard> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(c.remainingAmount), 0) from InfiniCard c " +
            "where c.userId = :userId and c.status = 'ACTIVE'")
    BigDecimal sumRemainingAmountByUserId(@Param("userId") Long userId);
}
