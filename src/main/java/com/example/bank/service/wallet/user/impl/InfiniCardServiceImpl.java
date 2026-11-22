package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.InfiniCardPageResponse;
import com.example.bank.dto.response.wallet.user.InfiniCardResponse;
import com.example.bank.entity.wallet.InfiniCard;
import com.example.bank.repository.wallet.InfiniCardRepository;
import com.example.bank.service.wallet.user.InfiniCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InfiniCardServiceImpl implements InfiniCardService {

    private static final int PAGE_SIZE = 10;

    private final InfiniCardRepository infiniCardRepository;

    @Override
    @Transactional(readOnly = true)
    public InfiniCardPageResponse getCards(Long userId, int page) {
        if (page < 0) {
            throw new WalletException(MessageKeys.INVALID_PAGE_NUMBER, HttpStatus.BAD_REQUEST);
        }
        Page<InfiniCard> result = infiniCardRepository.findByUserId(userId,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));

        return InfiniCardPageResponse.builder()
                .items(result.getContent().stream().map(InfiniCardResponse::from).toList())
                .page(page)
                .size(PAGE_SIZE)
                .totalSize(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InfiniCardResponse getCard(Long userId, Long cardId) {
        InfiniCard card = infiniCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND, HttpStatus.NOT_FOUND));

        return InfiniCardResponse.from(card);
    }
}
