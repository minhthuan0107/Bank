package com.example.bank.repository.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniWebhookEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InfiniWebhookEventRepository extends JpaRepository<InfiniWebhookEvent, String> {

    @Query("select e.eventId from InfiniWebhookEvent e where e.status = 'RECEIVED' " +
           "and e.receivedAt < :before and e.attempts < :maxAttempts order by e.receivedAt")
    List<String> findPending(@Param("before") Instant before, @Param("maxAttempts") int maxAttempts,
                             Pageable pageable);
}
