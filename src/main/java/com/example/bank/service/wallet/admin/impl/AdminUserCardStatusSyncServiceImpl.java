package com.example.bank.service.wallet.admin.impl;

import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.service.wallet.admin.AdminUserCardSingleStatusService;
import com.example.bank.service.wallet.admin.AdminUserCardStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserCardStatusSyncServiceImpl implements AdminUserCardStatusSyncService {

    private final CardRepository cardRepository;
    private final AdminUserCardSingleStatusService singleStatusService;

    @Override
    @Transactional(readOnly = true)
    public void lockAllCardsOfUser(Long userId) {
        List<Long> cardIds = cardRepository.findIdsByUserIdAndStatus(
                userId,
                CardStatus.ACTIVE
        );

        for (Long cardId : cardIds) {
            try {
                singleStatusService.lockSingleCard(cardId);
            } catch (Exception e) {
                log.error("Lock card failed userId={}, cardId={}", userId, cardId, e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void unlockAllCardsOfUser(Long userId) {
        List<Long> cardIds = cardRepository.findIdsByUserIdAndStatus(
                userId,
                CardStatus.BLOCKED
        );

        for (Long cardId : cardIds) {
            try {
                singleStatusService.unlockSingleCard(cardId);
            } catch (Exception e) {
                log.error("Unlock card failed userId={}, cardId={}", userId, cardId, e);
            }
        }
    }
}
