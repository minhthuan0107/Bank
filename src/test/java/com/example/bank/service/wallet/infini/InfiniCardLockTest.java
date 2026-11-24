package com.example.bank.service.wallet.infini;

import com.example.bank.common.config.infini.InfiniCardClient;
import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.enums.wallet.CardLockedBy;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.service.wallet.infini.impl.InfiniCardLockServiceImpl;
import com.example.bank.service.wallet.infini.impl.InfiniCardLockTransactionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Runs the real ledger against in-memory wallet/card objects, with Infini mocked. */
@ExtendWith(MockitoExtension.class)
class InfiniCardLockTest {
    @Mock InfiniProperties properties;
    @Mock InfiniCardClient client;
    @Mock WalletRepository wallets;
    @Mock InfiniCardRepository cards;
    @Mock InfiniFundingOrderRepository fundingOrders;

    InfiniCardLockTransactionServiceImpl ledger;
    InfiniCardLockServiceImpl service;
    Wallet wallet;
    InfiniCard card;
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach void setup() {
        ledger = new InfiniCardLockTransactionServiceImpl(wallets, cards, fundingOrders);
        service = new InfiniCardLockServiceImpl(properties, client, ledger, cards);
        // total 100 = available 60 + allocated 40 (the card)
        wallet = Wallet.builder().id(5L).userId(1L).currency(Stablecoin.USD)
                .totalBalance(new BigDecimal("100")).availableBalance(new BigDecimal("60"))
                .allocatedBalance(new BigDecimal("40")).frozenBalance(BigDecimal.ZERO).build();
        card = InfiniCard.builder().id(9L).userId(1L).infiniCardId("infini-9").status(CardStatus.ACTIVE)
                .remainingAmount(new BigDecimal("40")).lockedAmount(BigDecimal.ZERO).build();
        lenient().when(wallets.findByUserIdAndCurrencyForUpdate(1L, Stablecoin.USD)).thenReturn(Optional.of(wallet));
        lenient().when(cards.findByIdAndUserIdForUpdate(9L, 1L)).thenReturn(Optional.of(card));
        lenient().when(cards.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(card));
        lenient().when(cards.findOwnerId(9L)).thenReturn(Optional.of(1L));
        lenient().when(cards.findByIdForUpdate(9L)).thenReturn(Optional.of(card));
    }

    private void assertLocked() {
        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertEquals(0, card.getRemainingAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, card.getLockedAmount().compareTo(new BigDecimal("40")));
        assertEquals(0, wallet.getAllocatedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("40")));
        assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("100")));
    }

    private void assertActive() {
        assertEquals(CardStatus.ACTIVE, card.getStatus());
        assertEquals(0, card.getRemainingAmount().compareTo(new BigDecimal("40")));
        assertEquals(0, card.getLockedAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("40")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
    }

    @Test void lockMovesTheCardBalanceToFrozen() throws Exception {
        when(client.freeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.lockCard(1L, 9L, CardLockedBy.USER);
        assertLocked();
        verify(client, never()).status(any());
    }

    @Test void lockIsKeptWhenTheResponseWasLostButTheCardIsFrozen() throws Exception {
        when(client.freeze("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        when(client.status("infini-9")).thenReturn(json.readTree("{\"id\":\"infini-9\",\"status\":\"suspend\"}"));
        service.lockCard(1L, 9L, CardLockedBy.USER);
        assertLocked();
    }

    @Test void lockIsUndoneWhenInfiniKeepsTheCardActive() throws Exception {
        when(client.freeze("infini-9")).thenThrow(new InfiniCardClient.DefiniteRejection("PROVIDER_ERROR_1"));
        when(client.status("infini-9")).thenReturn(json.readTree("{\"id\":\"infini-9\",\"status\":\"active\"}"));
        assertThrows(WalletException.class, () -> service.lockCard(1L, 9L, CardLockedBy.USER));
        assertActive();
    }

    @Test void lockIsUndoneWhenTheStatusIsUnknown() throws Exception {
        when(client.freeze("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        when(client.status("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        assertThrows(WalletException.class, () -> service.lockCard(1L, 9L, CardLockedBy.USER));
        assertActive();
    }

    @Test void cardWithAPendingTopUpCannotBeLocked() throws Exception {
        when(fundingOrders.findFirstByCardIdAndStatusInOrderByCreatedAtAsc(eq(9L), any()))
                .thenReturn(Optional.of(new InfiniFundingOrder()));
        assertThrows(WalletException.class, () -> service.lockCard(1L, 9L, CardLockedBy.USER));
        verify(client, never()).freeze(any());
        assertActive();
    }

    @Test void alreadyBlockedCardCannotBeLockedAgain() throws Exception {
        card.setStatus(CardStatus.BLOCKED);
        assertThrows(WalletException.class, () -> service.lockCard(1L, 9L, CardLockedBy.USER));
        verify(client, never()).freeze(any());
    }

    @Test void unlockMovesTheBalanceBack() throws Exception {
        when(client.freeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.lockCard(1L, 9L, CardLockedBy.USER);
        when(client.unfreeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.unlockCard(1L, 9L, CardLockedBy.USER);
        assertActive();
    }

    @Test void unlockKeepsTheCardBlockedWhileInfiniStillHasItFrozen() throws Exception {
        when(client.freeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.lockCard(1L, 9L, CardLockedBy.USER);
        when(client.unfreeze("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        when(client.status("infini-9")).thenReturn(json.readTree("{\"id\":\"infini-9\",\"status\":\"suspend\"}"));
        assertThrows(WalletException.class, () -> service.unlockCard(1L, 9L, CardLockedBy.USER));
        assertLocked();
    }

    @Test void unlockSucceedsWhenTheResponseWasLostButTheCardIsActive() throws Exception {
        when(client.freeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.lockCard(1L, 9L, CardLockedBy.USER);
        when(client.unfreeze("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        when(client.status("infini-9")).thenReturn(json.readTree("{\"id\":\"infini-9\",\"status\":\"active\"}"));
        service.unlockCard(1L, 9L, CardLockedBy.USER);
        assertActive();
    }

    @Test void activeCardCannotBeUnlocked() throws Exception {
        assertThrows(WalletException.class, () -> service.unlockCard(1L, 9L, CardLockedBy.USER));
        verify(client, never()).unfreeze(any());
    }

    @Test void adminAccountLockLocksEveryActiveCardAndContinuesOnFailure() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(cards.findIdsByUserIdAndStatus(1L, CardStatus.ACTIVE)).thenReturn(List.of(8L, 9L));
        when(cards.findByIdAndUserIdForUpdate(8L, 1L)).thenReturn(Optional.empty()); // first card fails
        when(client.freeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.lockAllCardsOfUser(1L);
        assertLocked();
    }

    private void lockedAs(CardLockedBy actor) throws Exception {
        when(client.freeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.lockCard(1L, 9L, actor);
        assertLocked();
        assertEquals(actor, card.getLockedBy());
    }

    private void userCannotUnlock() throws Exception {
        var ex = assertThrows(WalletException.class, () -> service.unlockCard(1L, 9L, CardLockedBy.USER));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(client, never()).unfreeze(any());
        assertLocked();
    }

    @Test void userCannotUnlockACardLockedByAdminButAdminCan() throws Exception {
        lockedAs(CardLockedBy.ADMIN);
        userCannotUnlock();
        when(client.unfreeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.unlockCard(1L, 9L, CardLockedBy.ADMIN);
        assertActive();
        assertNull(card.getLockedBy());
    }

    @Test void userCannotUnlockACardLockedWithTheAccount() throws Exception {
        lockedAs(CardLockedBy.ACCOUNT);
        userCannotUnlock();
    }

    @Test void adminTakesOverAUserLockWithoutMovingMoneyAgain() throws Exception {
        lockedAs(CardLockedBy.USER);
        lockedAs(CardLockedBy.ADMIN);
        userCannotUnlock();
    }

    @Test void failedAdminTakeoverStaysLockedByAdmin() throws Exception {
        lockedAs(CardLockedBy.USER);
        when(client.freeze("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        when(client.status("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        assertThrows(WalletException.class, () -> service.lockCard(1L, 9L, CardLockedBy.ADMIN));
        userCannotUnlock();
    }

    @Test void adminLockDuringAUserUnlockWinsAndFreezesAgain() throws Exception {
        lockedAs(CardLockedBy.USER);
        when(client.unfreeze("infini-9")).thenAnswer(i -> {
            card.setLockedBy(CardLockedBy.ADMIN);
            return json.readTree("{\"success\":true}");
        });
        var ex = assertThrows(WalletException.class, () -> service.unlockCard(1L, 9L, CardLockedBy.USER));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(client, times(2)).freeze("infini-9");
        assertLocked();
    }

    @Test void accountUnlockOnlyReleasesAccountLocks() throws Exception {
        assertFalse(CardLockedBy.ACCOUNT.mayRelease(CardLockedBy.USER));
        assertFalse(CardLockedBy.ACCOUNT.mayRelease(CardLockedBy.ADMIN));
        lockedAs(CardLockedBy.ACCOUNT);
        when(properties.isEnabled()).thenReturn(true);
        when(cards.findIdsByUserIdAndLockedBy(1L, CardStatus.BLOCKED, CardLockedBy.ACCOUNT)).thenReturn(List.of(9L));
        when(client.unfreeze("infini-9")).thenReturn(json.readTree("{\"success\":true}"));
        service.unlockAllCardsOfUser(1L);
        assertActive();
    }
}
