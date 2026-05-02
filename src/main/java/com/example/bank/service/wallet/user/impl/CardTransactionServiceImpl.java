package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.dto.response.wallet.user.CardTransactionListResponse;
import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;
import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.service.wallet.user.CardTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardTransactionServiceImpl implements CardTransactionService {

    private final CardTransactionRepository cardTransactionRepository;
    private final WalletProperties walletProperties;

    @Override
    @Transactional(readOnly = true)
    public CardTransactionPageResponse getUserCardTransactions(
            Long userId,
            int page
    ) {
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
