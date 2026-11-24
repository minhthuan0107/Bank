package com.example.bank.service.wallet.infini;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardLockedBy;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.projection.UserNameProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.service.wallet.infini.impl.AdminInfiniCardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInfiniCardServiceTest {
    @Mock InfiniCardRepository cards;
    @Mock UserRepository users;
    @Mock WalletProperties walletProperties;
    @Mock InfiniCardLockService lockService;
    @InjectMocks AdminInfiniCardServiceImpl service;

    @Test void listShowsOwnerAndMaskedCardWithoutProviderId() {
        var card = InfiniCard.builder().id(9L).userId(1L).infiniCardId("infini-9").last4("1335")
                .status(CardStatus.BLOCKED).remainingAmount(BigDecimal.ZERO).lockedAmount(new BigDecimal("40")).build();
        UserNameProjection owner = mock(UserNameProjection.class);
        when(owner.getId()).thenReturn(1L);
        when(owner.getUsername()).thenReturn("alice");
        when(walletProperties.getDefaultPageSize()).thenReturn(15);
        when(cards.searchAdmin(eq("1335"), isNull(), isNull(), eq(CardStatus.BLOCKED), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(card)));
        when(users.findByIdIn(List.of(1L))).thenReturn(List.of(owner));

        var page = service.getCards(" 1335 ", " ", null, CardStatus.BLOCKED, null, null, -1);

        var item = page.getItems().get(0);
        assertEquals(0, page.getPage());
        assertEquals("alice", item.getUsername());
        assertEquals("**** **** **** 1335", item.getMaskedCard());
        assertEquals(0, item.getLockedAmount().compareTo(new BigDecimal("40")));
    }

    @Test void invalidTimeRangeIsRejected() {
        Instant now = Instant.now();
        assertThrows(WalletException.class,
                () -> service.getCards(null, null, null, null, now, now.minusSeconds(1), 0));
    }

    @Test void adminLockUsesTheCardOwner() {
        when(cards.findById(9L)).thenReturn(Optional.of(InfiniCard.builder().id(9L).userId(1L).build()));
        service.lockCard(100L, 9L);
        verify(lockService).lockCard(1L, 9L, CardLockedBy.ADMIN);
    }

    @Test void adminUnlockUsesTheCardOwner() {
        when(cards.findById(9L)).thenReturn(Optional.of(InfiniCard.builder().id(9L).userId(1L).build()));
        service.unlockCard(100L, 9L);
        verify(lockService).unlockCard(1L, 9L, CardLockedBy.ADMIN);
    }

    @Test void unknownCardIsNotFound() {
        when(cards.findById(9L)).thenReturn(Optional.empty());
        assertThrows(WalletException.class, () -> service.lockCard(100L, 9L));
        verifyNoInteractions(lockService);
    }
}
