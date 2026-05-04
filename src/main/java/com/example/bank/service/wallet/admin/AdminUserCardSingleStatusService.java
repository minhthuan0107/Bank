package com.example.bank.service.wallet.admin;

public interface AdminUserCardSingleStatusService {
    void lockSingleCard(Long cardId);

    void unlockSingleCard(Long cardId);
}
