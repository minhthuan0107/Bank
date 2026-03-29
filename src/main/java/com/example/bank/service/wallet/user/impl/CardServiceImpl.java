package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import com.example.bank.entity.wallet.*;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.event.CardCreatedEvent;
import com.example.bank.repository.wallet.CardBinRepository;
import com.example.bank.repository.wallet.CardHolderRepository;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.redis.LockService;
import com.example.bank.service.wallet.user.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final WalletRepository walletRepository;
    private final CardBinRepository cardBinRepository;
    private final CardHolderRepository cardHolderRepository;
    private final LockService lockService;
    private final WalletProperties walletProperties;
    private final SlashClient slashClient;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Transactional
    public void createCard(CreateCardRequest request, Long userId) {

        String lockKey = "lock:create_card:" + userId;

        if (!lockService.tryLock(lockKey)) {
            throw new WalletException(
                    MessageKeys.TOO_MANY_REQUESTS,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        try {
            // ===== WALLET =====
            Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            BigDecimal amount = request.getAmount();
            BigDecimal available = wallet.getAvailableBalance();

            if (available.compareTo(amount) < 0) {
                throw new WalletException(
                        MessageKeys.INSUFFICIENT_BALANCE,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== LIMIT CARD =====
            long totalCard = cardRepository.countByUserId(userId);
            if (totalCard >= walletProperties.getLimit()) {
                throw new WalletException(
                        MessageKeys.CARD_LIMIT_EXCEEDED,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== BIN =====
            CardBin bin = cardBinRepository.findByBin(request.getBin())
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.INVALID_BIN,
                            HttpStatus.BAD_REQUEST
                    ));

            // ===== CALL SLASH (QUAN TRỌNG) =====
            SlashCreateCardResponse slashCard = slashClient.createCard(
                    request.getName(),
                    bin.getCardProductId(),
                    amount
            );

            // ===== UPDATE WALLET (sau khi Slash OK) =====
            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().subtract(amount)
            );

            wallet.setAllocatedBalance(
                    wallet.getAllocatedBalance().add(amount)
            );

            // ===== SAVE CARD =====
            Card card = Card.builder()
                    .userId(userId)
                    .slashCardId(slashCard.getId())
                    .name(request.getName())
                    .type("virtual")
                    .allocatedAmount(amount)
                    .spentAmount(BigDecimal.ZERO)
                    .remainingAmount(amount)
                    .status(CardStatus.ACTIVE)
                    .bin(request.getBin())
                    .last4(slashCard.getLast4())
                    .expMonth(
                            slashCard.getExpiration() != null
                                    ? slashCard.getExpiration().getMonth()
                                    : null
                    )
                    .expYear(
                            slashCard.getExpiration() != null
                                    ? slashCard.getExpiration().getYear()
                                    : null
                    )
                    .build();

            cardRepository.save(card);

            applicationEventPublisher.publishEvent(
                    new CardCreatedEvent(
                            card.getId(),
                            slashCard.getId()
                    )
            );
            // ===== SAVE HOLDER =====
            var h = request.getHolder();

            CardHolder holder = CardHolder.builder()
                    .cardId(card.getId())
                    .firstName(h.getFirstName())
                    .lastName(h.getLastName())
                    .addressLine(h.getAddressLine())
                    .city(h.getCity())
                    .state(h.getState())
                    .postalCode(h.getPostalCode())
                    .country(h.getCountry())
                    .build();

            cardHolderRepository.save(holder);
        } finally {
            lockService.release(lockKey);
        }
    }
}

