package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import com.example.bank.dto.response.wallet.user.BalanceResponse;
import com.example.bank.dto.response.wallet.user.CardDashboardResponse;
import com.example.bank.dto.response.wallet.user.CardListResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.entity.user.User;
import com.example.bank.entity.wallet.*;
import com.example.bank.enums.wallet.CardBrand;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.event.CardCreatedEvent;
import com.example.bank.repository.projection.CardDashboardProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.*;
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
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final WalletRepository walletRepository;
    private final CardBinRepository cardBinRepository;
    private final CardHolderRepository cardHolderRepository;
    private final LockService lockService;
    private final SlashClient slashClient;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CardFundingTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletCurrencySettingsRepository walletCurrencySettingsRepository;
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

            // ===== CHECK MIN CARD FUNDING AMOUNT =====
            // Số tiền tạo/nạp thẻ không được nhỏ hơn cấu hình tối thiểu.
            WalletCurrencySettings settings = walletCurrencySettingsRepository
                    .findByCurrencyAndStatus(Stablecoin.USDT, "ACTIVE")
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_CURRENCY_SETTINGS_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            if (amount.compareTo(settings.getMinCardFundingAmount()) < 0) {
                throw new WalletException(
                        MessageKeys.CARD_FUNDING_AMOUNT_BELOW_MIN,
                        HttpStatus.BAD_REQUEST
                );
            }

            if (available.compareTo(amount) < 0) {
                throw new WalletException(
                        MessageKeys.INSUFFICIENT_BALANCE,
                        HttpStatus.BAD_REQUEST
                );
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.USER_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== LIMIT CARD =====
            long totalCard = cardRepository.countByUserId(userId);
            if (totalCard >= user.getCardOpenLimit()) {
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
                    .cardLimit(amount)
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

    public CardPageResponse getDashboard(Long userId, int page) {
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

    @Transactional(readOnly = true)
    public BalanceResponse getUserBalance(Long userId) {
        // ===== WALLET =====
        BigDecimal walletBalance = walletRepository
                .findAvailableBalanceByUserId(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== CARD BALANCE (sum remaining) =====
        BigDecimal cardBalance = cardRepository.sumRemainingAmountByUserId(userId);
        if (cardBalance == null) {
            cardBalance = BigDecimal.ZERO;
        }

        return BalanceResponse.builder()
                .walletBalance(walletBalance)
                .cardBalance(cardBalance)
                .build();
    }


    @Override
    @Transactional
    public void lockCard(Long userId, Long cardId) {

        Card card = cardRepository.findByIdAndUserIdForUpdate(cardId, userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CARD_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new WalletException(
                    MessageKeys.CARD_ALREADY_BLOCKED,
                    HttpStatus.CONFLICT
            );
        }

        // Tránh lock khi đang có giao dịch
        boolean hasPending = transactionRepository.existsByCardIdAndStatus(
                cardId,
                CardTxnStatus.PENDING
        );
        if (hasPending) {
            throw new WalletException(
                    MessageKeys.CARD_HAS_PENDING_TRANSACTION,
                    HttpStatus.CONFLICT
            );
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        BigDecimal amount = card.getRemainingAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletException(
                    MessageKeys.INVALID_BALANCE,
                    HttpStatus.CONFLICT
            );
        }

        // 1. Block card bên Slash trước
        slashClient.lockCard(card.getSlashCardId());

        // 2. Nếu remainingAmount > 0 thì mới chuyển allocated -> frozen
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

        // 3. Dù amount = 0 vẫn block thẻ bình thường
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Override
    @Transactional
    public void unlockCard(Long userId, Long cardId) {
        Card card = cardRepository.findByIdAndUserIdForUpdate(cardId, userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CARD_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new WalletException(
                    MessageKeys.CARD_ALREADY_ACTIVE,
                    HttpStatus.CONFLICT
            );
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        BigDecimal amount = card.getLockedAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletException(
                    MessageKeys.INVALID_BALANCE,
                    HttpStatus.CONFLICT
            );
        }

        slashClient.unblockCard(card.getSlashCardId());

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setFrozenBalance(
                    wallet.getFrozenBalance().subtract(amount)
            );

            wallet.setAllocatedBalance(
                    wallet.getAllocatedBalance().add(amount)
            );

            card.setRemainingAmount(amount);

            walletRepository.save(wallet);
        }

        card.setLockedAmount(BigDecimal.ZERO);
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    public CardPageResponse getUserCards(
            Long userId,
            String cardNumber,
            String cardName,
            CardStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {
        // normalize input
        cardNumber = normalize(cardNumber);
        cardName = normalize(cardName);

        // validate time
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(
                    MessageKeys.INVALID_TIME_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }
        // paging
        page = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                page,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // query (search thay vì findByUserId)
        Page<Card> cardPage = cardRepository.searchEntity(
                userId,
                cardNumber,
                cardName,
                status,
                fromTime,
                toTime,
                pageable
        );

        // mapping (giữ style của bạn)
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

        // build response (giữ nguyên format bạn đang dùng)
        return CardPageResponse.builder()
                .items(items)
                .page(page)
                .size(DEFAULT_PAGE_SIZE)
                .totalSize(cardPage.getTotalElements())
                .hasNext(cardPage.hasNext())
                .build();
    }
    private String normalize(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    public CardDashboardResponse getCardDashboard(Long userId) {

        CardDashboardProjection p = cardRepository.getDashboard(userId);
        return CardDashboardResponse.builder()
                .totalBalance(
                        p != null && p.getTotalBalance() != null
                                ? p.getTotalBalance()
                                : BigDecimal.ZERO
                )
                .activeCount(
                        p != null && p.getActiveCount() != null
                                ? p.getActiveCount()
                                : 0L
                )
                .blockedCount(
                        p != null && p.getBlockedCount() != null
                                ? p.getBlockedCount()
                                : 0L
                )
                .build();
    }


}

