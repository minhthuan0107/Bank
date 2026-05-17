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
     * Chạy mỗi 3 tiếng
     */
    //@Scheduled(cron = "0 0 */3 * * ?")
    @Scheduled(cron = "0 * * * * ?")
    public void generateCashbackMonthly() {
        log.info("Start cashback batch job");

        try {
            cashbackBatchService.generateMonthlyCashback();
        } catch (Exception e) {
            log.error("Cashback batch failed: {}", e.getMessage(), e);
        }

        log.info("End cashback batch job");
    }
}