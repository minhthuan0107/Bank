package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CardCreationOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface CardCreationOrderRepository extends JpaRepository<CardCreationOrder, String> {
    Optional<CardCreationOrder> findByRequestIdAndUserId(String requestId, Long userId);
    long countByUserIdAndStatusIn(Long userId, Collection<String> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CardCreationOrder o where o.requestId = :id")
    Optional<CardCreationOrder> findForUpdate(@Param("id") String id);
    @Query("select o.requestId from CardCreationOrder o where o.status in ('PENDING', 'REVIEW_REQUIRED') " +
           "and o.providerCardId is not null and o.nextCheckAt <= :now order by o.nextCheckAt")
    List<String> findDue(@Param("now") Instant now, Pageable pageable);

    Optional<CardCreationOrder> findFirstByUserIdAndStatusInOrderByCreatedAtAsc(Long userId, Collection<String> statuses);

    boolean existsByUserIdAndStatusIn(Long userId, Collection<String> statuses);

    // Reserved but the apply request was never sent.
    @Query("select o.requestId from CardCreationOrder o where o.status = 'PENDING' " +
           "and o.providerCardId is null and o.applyStartedAt is null and o.createdAt < :before order by o.createdAt")
    List<String> findUnsent(@Param("before") Instant before, Pageable pageable);

    // The apply request was sent but no provider card ID was stored.
    @Query("select o.requestId from CardCreationOrder o where o.status in ('PENDING', 'REVIEW_REQUIRED') " +
           "and o.providerCardId is null and o.applyStartedAt is not null " +
           "and o.applyStartedAt < :before and o.nextCheckAt <= :now order by o.nextCheckAt")
    List<String> findAwaitingProviderId(@Param("before") Instant before, @Param("now") Instant now, Pageable pageable);
}
