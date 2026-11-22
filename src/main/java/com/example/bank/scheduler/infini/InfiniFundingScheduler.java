package com.example.bank.scheduler.infini;

import com.example.bank.service.wallet.infini.InfiniFundingService;
import com.example.bank.service.wallet.infini.InfiniWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfiniFundingScheduler {

    private final InfiniFundingService fundingService;
    private final InfiniWebhookService webhookService;

    @Scheduled(fixedDelayString = "${infini.funding-poll-delay-ms:30000}")
    public void run() {
        try {
            fundingService.expireStaleOrders();
        } catch (Exception e) {
            log.error("INFINI_FUNDING_EXPIRY_FAILED", e);
        }
        try {
            webhookService.processPending();
        } catch (Exception e) {
            log.error("INFINI_WEBHOOK_RETRY_FAILED", e);
        }
    }
}
