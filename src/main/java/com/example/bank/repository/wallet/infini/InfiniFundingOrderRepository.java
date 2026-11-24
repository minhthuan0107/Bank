package com.example.bank.repository.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InfiniFundingOrderRepository extends JpaRepository<InfiniFundingOrder, String> {

    Optional<InfiniFundingOrder> findByRequestIdAndUserId(String requestId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from InfiniFundingOrder o where o.requestId = :id")
    Optional<InfiniFundingOrder> findForUpdate(@Param("id") String id);

    Optional<InfiniFundingOrder> findFirstByCardIdAndStatusInOrderByCreatedAtAsc(Long cardId, Collection<String> statuses);

    Optional<InfiniFundingOrder> findFirstByCardIdAndTypeAndStatusOrderBySubmitStartedAtDesc(
            Long cardId, String type, String status);

    Optional<InfiniFundingOrder> findByProviderTxId(String providerTxId);

    // Sent, but the result was never recorded: candidates for a provider webhook.
    @Query("select o from InfiniFundingOrder o where o.providerCardId = :providerCardId and o.type = :type " +
           "and o.status in ('PENDING', 'REVIEW_REQUIRED') and o.providerTxId is null " +
           "and o.submitStartedAt is not null order by o.createdAt")
    List<InfiniFundingOrder> findUnresolvedSent(@Param("providerCardId") String providerCardId,
                                                @Param("type") String type);

    // Reserved but the provider request was never sent.
    @Query("select o.requestId from InfiniFundingOrder o where o.status = 'PENDING' " +
           "and o.submitStartedAt is null and o.createdAt < :before order by o.createdAt")
    List<String> findUnsent(@Param("before") Instant before, Pageable pageable);

    // Sent, but the process stopped before recording the result.
    @Query("select o.requestId from InfiniFundingOrder o where o.status = 'PENDING' " +
           "and o.providerTxId is null and o.submitStartedAt is not null and o.submitStartedAt < :before " +
           "order by o.submitStartedAt")
    List<String> findInterrupted(@Param("before") Instant before, Pageable pageable);
}
