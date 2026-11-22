package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.config.infini.InfiniCardClient;
import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.service.wallet.infini.InfiniCardLockService;
import com.example.bank.service.wallet.infini.InfiniCardLockTransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Lock: block locally first (stops top-ups/withdrawals), then freeze at Infini; undo the local block only
 * when Infini still reports the card active.
 * Unlock: unfreeze at Infini first; unblock locally only once Infini confirms or reports the card active.
 * Retrying either operation converges because the provider status is checked when a call fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InfiniCardLockServiceImpl implements InfiniCardLockService {

    // Card status values from the Infini webhook docs; freeze/unfreeze docs do not name the frozen status.
    private static final String ACTIVE = "active";
    private static final String FROZEN = "suspend";

    private final InfiniProperties properties;
    private final InfiniCardClient client;
    private final InfiniCardLockTransactionService ledger;
    private final InfiniCardRepository cards;

    @Override
    public void lockCard(Long userId, Long cardId) {
        client.validateConfiguration();
        InfiniCard card = ledger.block(userId, cardId);
        if (confirmed(() -> client.freeze(card.getInfiniCardId()), card, FROZEN)) {
            log.info("Card locked userId={} cardId={}", userId, cardId);
            return;
        }
        // Infini still has the card active (or its status is unknown): give the user the card back.
        ledger.unblock(cardId);
        throw new WalletException(MessageKeys.INFINI_CARD_LOCK_FAILED, HttpStatus.BAD_GATEWAY);
    }

    @Override
    public void unlockCard(Long userId, Long cardId) {
        client.validateConfiguration();
        InfiniCard card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new WalletException(MessageKeys.CARD_ALREADY_ACTIVE, HttpStatus.CONFLICT);
        }
        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_CARD_NOT_ACTIVE, HttpStatus.CONFLICT);
        }
        if (!confirmed(() -> client.unfreeze(card.getInfiniCardId()), card, ACTIVE)) {
            // Still frozen at Infini: keep it blocked here as well.
            throw new WalletException(MessageKeys.INFINI_CARD_UNLOCK_FAILED, HttpStatus.BAD_GATEWAY);
        }
        ledger.unblock(cardId);
        log.info("Card unlocked userId={} cardId={}", userId, cardId);
    }

    @Override
    public void lockAllCardsOfUser(Long userId) {
        if (!properties.isEnabled()) return;
        for (Long cardId : cards.findIdsByUserIdAndStatus(userId, CardStatus.ACTIVE)) {
            try {
                lockCard(userId, cardId);
            } catch (Exception e) {
                log.error("Lock Infini card failed userId={} cardId={} reason={}", userId, cardId,
                        e.getClass().getSimpleName());
            }
        }
    }

    @Override
    public void unlockAllCardsOfUser(Long userId) {
        if (!properties.isEnabled()) return;
        for (Long cardId : cards.findIdsByUserIdAndStatus(userId, CardStatus.BLOCKED)) {
            try {
                unlockCard(userId, cardId);
            } catch (Exception e) {
                log.error("Unlock Infini card failed userId={} cardId={} reason={}", userId, cardId,
                        e.getClass().getSimpleName());
            }
        }
    }

    private interface ProviderCall {
        JsonNode call() throws Exception;
    }

    /**
     * True when the provider accepted the action, or when the card already has the wanted status
     * (e.g. it was frozen by an earlier attempt whose response was lost).
     */
    private boolean confirmed(ProviderCall action, InfiniCard card, String wantedStatus) {
        try {
            JsonNode result = action.call();
            if (result.path("success").asBoolean(false)) return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Card {} call failed cardId={} reason={}", wantedStatus, card.getId(), e.getClass().getSimpleName());
        }
        try {
            JsonNode status = client.status(card.getInfiniCardId());
            return card.getInfiniCardId().equals(status.path("id").asText()) &&
                    wantedStatus.equals(status.path("status").asText().toLowerCase(Locale.ROOT));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Card status check failed cardId={} reason={}", card.getId(), e.getClass().getSimpleName());
            return false;
        }
    }
}
