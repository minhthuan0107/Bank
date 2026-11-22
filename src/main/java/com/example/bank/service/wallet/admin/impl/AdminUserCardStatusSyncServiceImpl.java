package com.example.bank.service.wallet.admin.impl;

import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.slash.CardRepository;
import com.example.bank.service.wallet.admin.AdminUserCardSingleStatusService;
import com.example.bank.service.wallet.admin.AdminUserCardStatusSyncService;
import com.example.bank.service.wallet.infini.InfiniCardLockService;
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
    private final InfiniCardLockService infiniCardLockService;

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

        // Infini cards: errors are logged per card inside the service.
        infiniCardLockService.lockAllCardsOfUser(userId);
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

        infiniCardLockService.unlockAllCardsOfUser(userId);
    }
}
