package com.example.bank.scheduler;

import com.example.bank.service.cashback.admin.impl.CashbackBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashbackScheduler {

    private final CashbackBatchService cashbackBatchService;

    /**
     * Chạy mỗi 30 phút
     */
    @Scheduled(cron = "0 */30 * * * ?", zone = "UTC")
    public void generateCashbackMonthly() {
        try {
            cashbackBatchService.generateMonthlyCashback();
        } catch (Exception e) {
            log.error("Cashback batch failed: {}", e.getMessage(), e);
        }
    }
}