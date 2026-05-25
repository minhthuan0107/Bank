package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.Card;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.repository.wallet.CardFundingTransactionRepository;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.admin.AdminUserCardSingleStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserCardSingleStatusServiceImpl implements AdminUserCardSingleStatusService {

    private final CardRepository cardRepository;
    private final WalletRepository walletRepository;
    private final CardFundingTransactionRepository transactionRepository;
    private final SlashClient slashClient;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockSingleCard(Long cardId) {
        Card card = cardRepository.findByIdForUpdate(cardId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CARD_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (card.getStatus() == CardStatus.BLOCKED) {
            return;
        }

        boolean hasPending = transactionRepository.existsByCardIdAndStatus(
                cardId,
                CardTxnStatus.PENDING
        );

        if (hasPending) {
            log.warn("Skip lock card because funding transaction pending cardId={}", cardId);
            return;
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(card.getUserId())
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        BigDecimal amount = card.getRemainingAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Skip lock card because remaining amount invalid cardId={}", cardId);
            return;
        }

        // Khóa trên Slash trước
        slashClient.lockCard(card.getSlashCardId());

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setFrozenBalance(
                    wallet.getFrozenBalance().add(amount)
            );

            wallet.setAllocatedBalance(
                    wallet.getAllocatedBalance().subtract(amount)
            );

            card.setLockedAmount(amount);
            card.setRemainingAmount(BigDecimal.ZERO);

            walletRepository.save(wallet);
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockSingleCard(Long cardId) {
        Card card = cardRepository.findByIdForUpdate(cardId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CARD_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (card.getStatus() == CardStatus.ACTIVE) {
            return;
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(card.getUserId())
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        BigDecimal amount = card.getLockedAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Skip unlock card because locked amount invalid cardId={}", cardId);
            return;
        }

        // Mở trên Slash trước
        slashClient.unblockCard(card.getSlashCardId());

        // Nếu locked_amount > 0 thì mới move balance frozen -> allocated
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setFrozenBalance(
                    wallet.getFrozenBalance().subtract(amount)
            );

            wallet.setAllocatedBalance(
                    wallet.getAllocatedBalance().add(amount)
            );

            card.setRemainingAmount(amount);
            card.setLockedAmount(BigDecimal.ZERO);

            walletRepository.save(wallet);
        }

        // amount = 0 vẫn mở thẻ local
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);

    }
}