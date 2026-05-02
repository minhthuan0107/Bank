package com.example.bank.service.wallet.user.impl;

import com.example.bank.dto.response.wallet.user.SlashTransactionResponse;
import com.example.bank.entity.wallet.Card;
import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.repository.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;


@Service
@RequiredArgsConstructor
@Slf4j
public class CardTransactionPersistenceService {

    private final CardTransactionRepository repository;
    private final CardRepository cardRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public void upsertTransaction(
            SlashTransactionResponse tx,
            String eventId,
            String payload,
            CardTransactionStatus newStatus
    ) {
        if (newStatus.equals(CardTransactionStatus.UNKNOWN)){
            return;
        }

        // Deduplicate event
        if (repository.existsBySlashEventId(eventId)) {
            log.info("Duplicate event skipped eventId={}", eventId);
            return;
        }

        // lock card
        Card card = cardRepository.findBySlashCardIdForUpdate(tx.getCardId())
                .orElse(null);
        if (card == null) {
            log.warn("Card not found for slashCardId={} txId={}", tx.getCardId(), tx.getId());
            return;
        }

        BigDecimal amount = BigDecimal
                .valueOf(Math.abs(tx.getAmountCents()))
                .movePointLeft(2);

        // Insert-first để chống race
        CardTransaction entity;
        try {
            entity = new CardTransaction();
            entity.setSlashTransactionId(tx.getId());
            entity.setSlashEventId(eventId);
            // FIX QUAN TRỌNG
            entity.setAmount(amount);
            repository.saveAndFlush(entity); // quan trọng
        } catch (DataIntegrityViolationException e) {
            entity = repository.findBySlashTransactionIdForUpdate(tx.getId())
                    .orElseThrow();
        }

        CardTransactionStatus oldStatus = entity.getStatus();


        // set fields (idempotent)
        entity.setCardId(tx.getCardId());
        entity.setUserId(card.getUserId());
        entity.setMerchantDescription(tx.getMerchantDescription());
        entity.setAmount(amount);
        entity.setCurrency("USD");

        // STATE MACHINE
        if (!shouldUpdate(oldStatus, newStatus)) {
            return;
        }

        // UPDATE STATUS TRƯỚC
        entity.setStatus(newStatus);
        entity.setDetailedStatus(tx.getDetailedStatus());

        // SAU ĐÓ MỚI ĐỤNG TIỀN
        applyBalanceChange(card, oldStatus, newStatus, amount);

        // metadata
        entity.setDescription(tx.getDescription());
        entity.setDeclineReason(tx.getDeclineReason());

        if (tx.getAuthorizedAt() != null && !tx.getAuthorizedAt().isBlank()) {
            try {
                entity.setAuthorizedAt(Instant.parse(tx.getAuthorizedAt()));
            } catch (Exception e) {
                log.warn("Invalid authorizedAt format txId={}", tx.getId());
            }
        }

        if (tx.getDate() != null && !tx.getDate().isBlank()) {
            try {
                entity.setTransactionDate(Instant.parse(tx.getDate()));
            } catch (Exception e) {
                log.warn("Invalid date format txId={}", tx.getId());
            }
        }

        entity.setRawPayload(payload);

        repository.save(entity);
    }

    private boolean shouldUpdate(CardTransactionStatus current, CardTransactionStatus incoming) {
        if (incoming == null) return false;
        // lần đầu luôn cho update
        if (current == null) return true;

        return switch (current) {
            // đang pending → chỉ cho đi tới trạng thái cuối
            case PENDING -> incoming == CardTransactionStatus.POSTED
                    || incoming == CardTransactionStatus.FAILED
                    || incoming == CardTransactionStatus.REVERSED;

            // trạng thái cuối → không cho update nữa
            case POSTED, FAILED, REVERSED, UNKNOWN -> false;
        };
    }

    private void applyBalanceChange(Card card,
                                    CardTransactionStatus oldStatus,
                                    CardTransactionStatus newStatus,
                                    BigDecimal amount) {

        Wallet wallet = walletRepository.findByUserIdForUpdate(card.getUserId())
                .orElseThrow();

        // CASE 1: null → PENDING (lần đầu)
        if (oldStatus == null && newStatus == CardTransactionStatus.PENDING) {
            card.setSpentAmount(card.getSpentAmount().add(amount));
            card.setRemainingAmount(card.getRemainingAmount().subtract(amount));
            wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
            wallet.setAllocatedBalance(wallet.getAllocatedBalance().subtract(amount));
            return;
        }

        // CASE 2: PENDING → REVERSED / FAILED
        if (oldStatus == CardTransactionStatus.PENDING &&
                (newStatus == CardTransactionStatus.REVERSED
                        || newStatus == CardTransactionStatus.FAILED)) {

            card.setSpentAmount(card.getSpentAmount().subtract(amount));
            card.setRemainingAmount(card.getRemainingAmount().add(amount));
            wallet.setTotalBalance(wallet.getTotalBalance().add(amount));
            wallet.setAllocatedBalance(wallet.getAllocatedBalance().add(amount));
            return;
        }

        // CASE 3: PENDING → POSTED
        if (oldStatus == CardTransactionStatus.PENDING &&
                newStatus == CardTransactionStatus.POSTED) {
            // không làm gì
            return;
        }

        // CASE 4: null → POSTED (miss pending)
        if (oldStatus == null && newStatus == CardTransactionStatus.POSTED) {
            card.setSpentAmount(card.getSpentAmount().add(amount));
            card.setRemainingAmount(card.getRemainingAmount().subtract(amount));
            wallet.setTotalBalance(wallet.getTotalBalance().subtract(amount));
            wallet.setAllocatedBalance(wallet.getAllocatedBalance().subtract(amount));
        }
    }
}