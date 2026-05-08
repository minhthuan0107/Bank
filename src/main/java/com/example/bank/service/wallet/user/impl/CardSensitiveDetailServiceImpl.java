package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.CardSensitiveDetailResponse;
import com.example.bank.dto.response.wallet.user.SlashCardSensitiveDetailResponse;
import com.example.bank.entity.wallet.Card;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.service.wallet.user.CardSensitiveDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardSensitiveDetailServiceImpl implements CardSensitiveDetailService {

    private final CardRepository cardRepository;
    private final SlashClient slashClient;

    public CardSensitiveDetailResponse getSensitiveDetail(Long userId, Long cardId) {

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CARD_NOT_FOUND_OR_FORBIDDEN,
                        HttpStatus.NOT_FOUND
                ));

        if (card.getSlashCardId() == null || card.getSlashCardId().isBlank()) {
            throw new WalletException(
                    MessageKeys.CARD_SLASH_ID_NOT_FOUND,
                    HttpStatus.BAD_REQUEST
            );
        }

        SlashCardSensitiveDetailResponse slashCard =
                slashClient.getCardSensitiveDetail(card.getSlashCardId());

        return CardSensitiveDetailResponse.builder()
                .pan(slashCard.pan())
                .expiryYear(slashCard.expiryYear())
                .expiryMonth(slashCard.expiryMonth())
                .createdAt(slashCard.createdAt())
                .cvv(slashCard.cvv())
                .build();
    }
}
