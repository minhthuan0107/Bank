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
