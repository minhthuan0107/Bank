package com.example.bank.scheduler.infini;

import com.example.bank.service.wallet.infini.CardCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardCreationScheduler {

    private final CardCreationService cardCreationService;

    @Scheduled(fixedDelayString = "${infini.creation-poll-delay-ms:10000}")
    public void reconcilePendingOrders() {
        try {
            cardCreationService.reconcilePendingOrders();
        } catch (Exception exception) {
            log.error("CARD_CREATION_RECONCILIATION_FAILED", exception);
        }
    }
}
