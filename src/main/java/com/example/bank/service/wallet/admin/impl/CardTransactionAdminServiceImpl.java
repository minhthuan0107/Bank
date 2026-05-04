package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.CardTransactionListResponse;
import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;
import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.service.wallet.admin.CardTransactionAdminService;
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


@Service
@RequiredArgsConstructor
public class CardTransactionAdminServiceImpl implements CardTransactionAdminService {

    private final UserRepository userRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final WalletProperties walletProperties;


    @Override
    @Transactional(readOnly = true)
    public CardTransactionPageResponse getAdminUserCardTransactions(
            Long userId,
            String slashTransactionId,
            String merchantDescription,
            CardTransactionStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {

        // check user exists
        if (!userRepository.existsById(userId)) {
            throw new WalletException(
                    MessageKeys.USER_NOT_FOUND,
                    HttpStatus.NOT_FOUND
            );
        }


        int size = walletProperties.getDefaultPageSize();

        // normalize
        slashTransactionId = normalize(slashTransactionId);
        merchantDescription = normalize(merchantDescription);

        // validate time
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(
                    MessageKeys.INVALID_TIME_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        page = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<CardTransaction> result = cardTransactionRepository.search(
                userId,
                slashTransactionId,
                merchantDescription,
                status,
                fromTime,
                toTime,
                pageable
        );

        List<CardTransactionListResponse> items = result.getContent()
                .stream()
                .map(CardTransactionListResponse::from)
                .toList();

        return CardTransactionPageResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalSize(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public CardTransactionPageResponse getUserCardTransactions(
            Long userId,
            int page
    ) {
        // check user exists
        if (!userRepository.existsById(userId)) {
            throw new WalletException(
                    MessageKeys.USER_NOT_FOUND,
                    HttpStatus.NOT_FOUND
            );
        }

        if (page < 0) {
            page = 0;
        }

        int size = walletProperties.getDefaultPageSize();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<CardTransaction> transactions =
                cardTransactionRepository.findByUserId(userId, pageable);

        if (transactions.isEmpty()) {
            return CardTransactionPageResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(pageable.getPageSize())
                    .totalSize(0)
                    .hasNext(false)
                    .build();
        }

        List<CardTransactionListResponse> items =
                transactions.getContent()
                        .stream()
                        .map(CardTransactionListResponse::from)
                        .toList();

        return CardTransactionPageResponse.builder()
                .items(items)
                .page(page)
                .size(pageable.getPageSize())
                .totalSize(transactions.getTotalElements())
                .hasNext(transactions.hasNext())
                .build();
    }
}
