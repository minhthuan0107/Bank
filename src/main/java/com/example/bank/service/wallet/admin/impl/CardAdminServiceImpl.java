package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.admin.AdminCardListResponse;
import com.example.bank.dto.response.wallet.admin.AdminCardPageResponse;
import com.example.bank.entity.wallet.Card;
import com.example.bank.enums.wallet.CardBrand;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.projection.UserNameProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.service.wallet.admin.CardAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardAdminServiceImpl implements CardAdminService {

    private final UserRepository userRepository;
    private final WalletProperties walletProperties;
    private final CardRepository cardRepository;


    @Override
    @Transactional(readOnly = true)
    public AdminCardPageResponse getAdminCards(
            String cardNumber,
            String cardName,
            String username,
            CardStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {
        // normalize input
        cardNumber = normalize(cardNumber);
        cardName = normalize(cardName);
        username = normalize(username);

        // validate time
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(
                    MessageKeys.INVALID_TIME_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        int size = walletProperties.getDefaultPageSize();

        // paging
        page = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // query admin: all cards + optional filters
        Page<Card> cardPage = cardRepository.searchAdminEntity(
                cardNumber,
                cardName,
                username,
                status,
                fromTime,
                toTime,
                pageable
        );

        List<Card> content = cardPage.getContent();

        if (content.isEmpty()) {
            return AdminCardPageResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(size)
                    .totalSize(0)
                    .hasNext(false)
                    .build();
        }

        List<Long> userIds = content.stream()
                .map(Card::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> usernameMap = userIds.isEmpty()
                ? Map.of()
                : userRepository.findByIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserNameProjection::getId,
                        UserNameProjection::getUsername,
                        (oldValue, newValue) -> oldValue
                ));

        List<AdminCardListResponse> items = content.stream()
                .map(c -> AdminCardListResponse.builder()
                        .id(c.getId())
                        .userId(c.getUserId())
                        .username(usernameMap.get(c.getUserId()))
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

        return AdminCardPageResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalSize(cardPage.getTotalElements())
                .hasNext(cardPage.hasNext())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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
