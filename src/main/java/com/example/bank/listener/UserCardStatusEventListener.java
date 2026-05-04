package com.example.bank.listener;

import com.example.bank.event.UserLockedEvent;
import com.example.bank.event.UserUnlockedEvent;
import com.example.bank.service.wallet.admin.AdminUserCardStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCardStatusEventListener {

    private final AdminUserCardStatusSyncService syncService;

    @Async("userCardSyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLocked(UserLockedEvent event) {
        log.info("Start locking all cards for locked userId={}", event.userId());

        try {
            syncService.lockAllCardsOfUser(event.userId());
        } catch (Exception e) {
            log.error("Lock all cards failed userId={}", event.userId(), e);
        }
    }

    @Async("userCardSyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserUnlocked(UserUnlockedEvent event) {
        log.info("Start unlocking all cards for unlocked userId={}", event.userId());

        try {
            syncService.unlockAllCardsOfUser(event.userId());
        } catch (Exception e) {
            log.error("Unlock all cards failed userId={}", event.userId(), e);
        }
    }
}
