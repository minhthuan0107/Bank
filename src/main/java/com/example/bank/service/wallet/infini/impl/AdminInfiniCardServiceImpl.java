package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.infini.AdminInfiniCardPageResponse;
import com.example.bank.dto.response.wallet.infini.AdminInfiniCardResponse;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.projection.UserNameProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.service.wallet.infini.AdminInfiniCardService;
import com.example.bank.service.wallet.infini.InfiniCardLockService;
import com.example.bank.enums.wallet.CardLockedBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInfiniCardServiceImpl implements AdminInfiniCardService {

    private final InfiniCardRepository cards;
    private final UserRepository users;
    private final WalletProperties walletProperties;
    private final InfiniCardLockService lockService;

    @Override
    @Transactional(readOnly = true)
    public AdminInfiniCardPageResponse getCards(String cardNumber, String cardName, String username,
                                                CardStatus status, Instant fromTime, Instant toTime, int page) {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(MessageKeys.INVALID_TIME_RANGE, HttpStatus.BAD_REQUEST);
        }
        int size = walletProperties.getDefaultPageSize();
        int safePage = Math.max(page, 0);
        Page<InfiniCard> result = cards.searchAdmin(normalize(cardNumber), normalize(cardName), normalize(username),
                status, fromTime, toTime, PageRequest.of(safePage, size, Sort.by(Sort.Direction.DESC, "id")));

        List<Long> userIds = result.getContent().stream().map(InfiniCard::getUserId).distinct().toList();
        Map<Long, String> usernames = userIds.isEmpty() ? Map.of() : users.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserNameProjection::getId, UserNameProjection::getUsername, (a, b) -> a));

        return AdminInfiniCardPageResponse.builder()
                .items(result.getContent().stream()
                        .map(c -> AdminInfiniCardResponse.from(c, usernames.get(c.getUserId())))
                        .toList())
                .page(safePage)
                .size(size)
                .totalSize(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    // Same lock/unlock flow as the card owner's, including the provider status checks.
    @Override
    public void lockCard(Long adminId, Long cardId) {
        InfiniCard card = find(cardId);
        lockService.lockCard(card.getUserId(), cardId, CardLockedBy.ADMIN);
        log.info("Admin locked Infini card adminId={} cardId={} userId={}", adminId, cardId, card.getUserId());
    }

    @Override
    public void unlockCard(Long adminId, Long cardId) {
        InfiniCard card = find(cardId);
        lockService.unlockCard(card.getUserId(), cardId, CardLockedBy.ADMIN);
        log.info("Admin unlocked Infini card adminId={} cardId={} userId={}", adminId, cardId, card.getUserId());
    }

    private InfiniCard find(Long cardId) {
        return cards.findById(cardId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
