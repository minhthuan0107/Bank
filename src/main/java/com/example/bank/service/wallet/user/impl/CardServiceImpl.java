package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import com.example.bank.dto.response.wallet.user.CardListResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.entity.wallet.*;
import com.example.bank.enums.wallet.CardBrand;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
    private static final int DEFAULT_PAGE_SIZE = 10;


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
                    .note(request.getNote())
                    .currency("USD")
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

    public CardPageResponse getUserCards(Long userId, int page) {
        Pageable pageable = PageRequest.of(
                page,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<Card> cardPage = cardRepository.findByUserId(userId, pageable);

        List<CardListResponse> items = cardPage.getContent()
                .stream()
                .map(c -> CardListResponse.builder()
                        .id(c.getId())
                        .maskedCard(maskCard(c.getBin(), c.getLast4()))
                        .brand(detectBrand(c.getBin()))
                        .name(c.getName())
                        .note(c.getNote())
                        .currency(c.getCurrency())
                        .status(c.getStatus())
                        .remainingAmount(c.getRemainingAmount())
                        .createdAt(c.getCreatedAt())
                        .build()
                )
                .toList();

        return CardPageResponse.builder()
                .items(items)
                .page(page)
                .size(DEFAULT_PAGE_SIZE)
                .totalSize(cardPage.getTotalElements())
                .hasNext(cardPage.hasNext())
                .build();
    }
    private String maskCard(String bin, String last4) {
        if (bin == null || last4 == null) return "****";
        return bin + "******" + last4;
    }
    private CardBrand detectBrand(String bin) {
        if (bin == null || bin.isEmpty()) return CardBrand.UNKNOWN;
        if (bin.startsWith("4")) return CardBrand.VISA;
        if (bin.startsWith("5")) return CardBrand.MASTERCARD;
        if (bin.startsWith("34") || bin.startsWith("37")) return CardBrand.AMEX;
        if (bin.startsWith("6")) return CardBrand.DISCOVER;
        if (bin.startsWith("35")) return CardBrand.JCB;
        return CardBrand.UNKNOWN;
    }
}

