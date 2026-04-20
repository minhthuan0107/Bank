package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.Card;
import com.example.bank.entity.wallet.CardFundingTransaction;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;
import com.example.bank.repository.wallet.CardFundingTransactionRepository;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.redis.LockService;
import com.example.bank.service.wallet.user.CardFundingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardFundingServiceImpl implements CardFundingService {

    private final WalletRepository walletRepository;
    private final CardRepository cardRepository;
    private final CardFundingTransactionRepository txnRepo;
    private final SlashClient slashClient;

    @Transactional
    @Override
    public void topupCard(Long userId,
                          Long cardId,
                          BigDecimal amount,
                          String referenceId) {

        // ===== IDEMPOTENCY =====
        if (referenceId != null) {
            Optional<CardFundingTransaction> existing =
                    txnRepo.findByReferenceId(referenceId);
            if (existing.isPresent()) {
                log.warn("TOPUP DUPLICATE ref={}", referenceId);
                return;
            }
        }

        // ===== CREATE TXN (PENDING) =====
        CardFundingTransaction txn = CardFundingTransaction.builder()
                .cardId(cardId)
                .userId(userId)
                .amount(amount)
                .type(CardTxnType.TOPUP)
                .status(CardTxnStatus.PENDING)
                .referenceId(referenceId)
                .build();

        try {
            txnRepo.save(txn); //  rely on UNIQUE PENDING constraint
        } catch (DataIntegrityViolationException e) {
            throw new WalletException(
                    MessageKeys.CARD_TOPUP_IN_PROGRESS,
                    HttpStatus.BAD_REQUEST
            );
        }

        try {
            // ===== LOCK WALLET =====
            Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new WalletException(
                        MessageKeys.INSUFFICIENT_BALANCE,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== GET CARD =====
            Card card = cardRepository.findByIdAndUserId(cardId, userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.CARD_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));
            BigDecimal newLimit = card.getCardLimit().add(amount);
            // ===== CALL SLASH =====
            slashClient.setLimit(card.getSlashCardId(), newLimit);

            // ===== UPDATE WALLET =====
            int walletUpdated = walletRepository.decreaseBalance(userId, amount);
            if (walletUpdated == 0) {
                throw new WalletException(
                        MessageKeys.INSUFFICIENT_BALANCE,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== UPDATE CARD =====
            int cardUpdated = cardRepository.increaseLimit(cardId, amount);
            if (cardUpdated == 0) {
                throw new WalletException(
                        MessageKeys.CARD_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                );
            }

            // ===== SUCCESS =====
            txn.setStatus(CardTxnStatus.SUCCESS);
            txnRepo.save(txn);

            log.info("TOPUP SUCCESS userId={} cardId={} amount={}",
                    userId, cardId, amount);

        } catch (Exception e) {

            log.error("TOPUP FAIL userId={} cardId={} err={}",
                    userId, cardId, e.getMessage());

            txn.setStatus(CardTxnStatus.FAILED);
            txnRepo.save(txn);

            throw new WalletException(
                    MessageKeys.TOPUP_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    @Transactional
    @Override
    public void withdrawCard(Long userId,
                             Long cardId,
                             BigDecimal amount,
                             String referenceId) {

        // ===== IDEMPOTENCY =====
        if (referenceId != null) {
            Optional<CardFundingTransaction> existing =
                    txnRepo.findByReferenceId(referenceId);
            if (existing.isPresent()) {
                log.warn("WITHDRAW DUPLICATE ref={}", referenceId);
                return;
            }
        }

        // ===== CREATE TXN (PENDING) =====
        CardFundingTransaction txn = CardFundingTransaction.builder()
                .cardId(cardId)
                .userId(userId)
                .amount(amount)
                .type(CardTxnType.WITHDRAW)
                .status(CardTxnStatus.PENDING)
                .referenceId(referenceId)
                .build();

        try {
            txnRepo.save(txn); // rely UNIQUE constraint
        } catch (DataIntegrityViolationException e) {
            throw new WalletException(
                    MessageKeys.CARD_WITHDRAW_IN_PROGRESS,
                    HttpStatus.BAD_REQUEST
            );
        }

        try {
            // ===== LOCK WALLET =====
            Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== GET CARD (ownership check) =====
            Card card = cardRepository.findByIdAndUserId(cardId, userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.CARD_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== VALIDATE REMAINING =====
            BigDecimal remaining = card.getRemainingAmount();
            if (remaining.compareTo(amount) < 0) {
                throw new WalletException(
                        MessageKeys.INSUFFICIENT_CARD_BALANCE,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== CALCULATE NEW LIMIT =====
            BigDecimal newLimit = card.getCardLimit().subtract(amount);
            // ===== CALL SLASH =====
            slashClient.setLimit(card.getSlashCardId(), newLimit);

            // ===== UPDATE CARD =====
            int cardUpdated = cardRepository.decreaseLimit(cardId, amount);
            if (cardUpdated == 0) {
                throw new WalletException(
                        MessageKeys.CARD_UPDATE_FAILED,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== UPDATE WALLET =====
            int walletUpdated = walletRepository.increaseBalance(userId, amount);
            if (walletUpdated == 0) {
                throw new WalletException(
                        MessageKeys.WALLET_UPDATE_FAILED,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== SUCCESS =====
            txn.setStatus(CardTxnStatus.SUCCESS);
            txnRepo.save(txn);
            log.info("WITHDRAW SUCCESS userId={} cardId={} amount={}",
                    userId, cardId, amount);

        } catch (Exception e) {
            log.error("WITHDRAW FAIL userId={} cardId={} err={}",
                    userId, cardId, e.getMessage());

            txn.setStatus(CardTxnStatus.FAILED);
            txnRepo.save(txn);
            throw e;
        }
    }
}

