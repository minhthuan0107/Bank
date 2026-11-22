package com.example.bank.service.wallet.infini;

import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.service.wallet.infini.impl.InfiniCardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfiniCardServiceTest {

    @Mock InfiniCardRepository cards;
    @InjectMocks InfiniCardServiceImpl service;

    @Test void missingCardOrDifferentOwnerReturnsNotFound() {
        when(cards.findByIdAndUserId(9L, 2L)).thenReturn(Optional.empty());
        assertThrows(WalletException.class, () -> service.getCard(2L, 9L));
    }

    @Test void listsOnlyInfiniCardsOfAuthenticatedUser() {
        InfiniCard card = InfiniCard.builder().id(9L).userId(1L).name("Infini")
                .last4("1234").productId(2).remainingAmount(new BigDecimal("10"))
                .status(CardStatus.ACTIVE).currency("USD").build();
        when(cards.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card)));

        var result = service.getCards(1L, 0);

        assertEquals(1, result.getItems().size());
        assertEquals("INFINI", result.getItems().get(0).getProvider());
        assertEquals("**** **** **** 1234", result.getItems().get(0).getMaskedCard());
        verify(cards).findByUserId(eq(1L), any(Pageable.class));
    }

    @Test void emptyPageKeepsTotalCount() {
        when(cards.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 10), 20));
        assertEquals(20, service.getCards(1L, 3).getTotalSize());
    }
}
