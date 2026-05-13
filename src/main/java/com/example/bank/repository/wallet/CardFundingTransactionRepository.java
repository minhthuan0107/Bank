package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CardFundingTransaction;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;
import com.example.bank.repository.projection.CardFundingTransactionProjection;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CardFundingTransactionRepository extends JpaRepository<CardFundingTransaction, Long> {
    Optional<CardFundingTransaction> findByReferenceId(String referenceId);

    boolean existsByCardIdAndStatus(Long cardId, CardTxnStatus status);

    @Query("""
            SELECT 
                t.id AS id,
                c.bin AS bin,
                c.last4 AS last4,
                t.type AS type,
                t.amount AS amount,
                t.status AS status,
                t.createdAt AS createdAt
            FROM CardFundingTransaction t
            JOIN Card c ON c.id = t.cardId
            WHERE t.userId = :userId
              AND (:last4 IS NULL OR c.last4 LIKE CONCAT('%', :last4, '%'))
              AND (:type IS NULL OR t.type = :type)
              AND (:status IS NULL OR t.status = :status)
              AND (:fromTime IS NULL OR t.createdAt >= :fromTime)
              AND (:toTime IS NULL OR t.createdAt <= :toTime)
            ORDER BY t.createdAt DESC, t.id DESC
            """)
    Page<CardFundingTransactionProjection> searchUserFundingTransactions(
            @Param("userId") Long userId,
            @Param("last4") String last4,
            @Param("type") CardTxnType type,
            @Param("status") CardTxnStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );

}
