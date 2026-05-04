package com.example.bank.service.wallet.admin;

public interface AdminUserCardStatusSyncService {
    void lockAllCardsOfUser(Long userId);

    void unlockAllCardsOfUser(Long userId);
}
