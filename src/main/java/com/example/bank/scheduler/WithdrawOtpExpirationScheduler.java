package com.example.bank.scheduler;

import com.example.bank.service.sync.WithdrawOtpExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawOtpExpirationScheduler {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_BATCHES = 20;

    private final WithdrawOtpExpirationService expirationService;

    /**
     * Chạy lúc phút 00 và 30 mỗi giờ, theo UTC.
     *
     * Chỉ chuyển PENDING_OTP -> EXPIRED.
     * Không thay đổi số dư Wallet.
     */
    @Scheduled(cron = "0 0,30 * * * *", zone = "UTC")
    public void expirePendingOtpOrders() {

        long startedAt = System.nanoTime();

        int totalExpired = 0;
        int processedBatches = 0;

        try {
            for (int i = 0; i < MAX_BATCHES; i++) {

                int expiredCount = expirationService.expireBatch(BATCH_SIZE);

                totalExpired += expiredCount;
                processedBatches++;

                if (expiredCount < BATCH_SIZE) {
                    break;
                }
            }

            if (processedBatches == MAX_BATCHES) {
                log.warn(
                        "WITHDRAW_OTP_EXPIRATION_BATCH_LIMIT_REACHED batches={} totalExpired={}",
                        processedBatches,
                        totalExpired
                );
            }

            if (totalExpired > 0) {
                long durationMs =
                        (System.nanoTime() - startedAt) / 1_000_000;

                log.info(
                        "WITHDRAW_OTP_EXPIRATION_COMPLETED totalExpired={} batches={} durationMs={}",
                        totalExpired,
                        processedBatches,
                        durationMs
                );
            }

        } catch (Exception ex) {
            log.error(
                    "WITHDRAW_OTP_EXPIRATION_FAILED totalExpired={} batches={}",
                    totalExpired,
                    processedBatches,
                    ex
            );
        }
    }
}