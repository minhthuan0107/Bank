package com.example.bank.service.wallet.infini;

import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.infini.impl.InfiniFundingTransactionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfiniFundingLedgerTest {
    @Mock InfiniFundingOrderRepository orders;
    @Mock WalletRepository wallets;
    @Mock InfiniCardRepository cards;
    @InjectMocks InfiniFundingTransactionServiceImpl ledger;

    Wallet wallet;
    InfiniCard card;
    InfiniFundingRequest request;

    @BeforeEach void setup() {
        // total 100 = available 50 + allocated 50 (one card with 50)
        wallet = Wallet.builder().id(5L).userId(1L).currency(Stablecoin.USD).status("ACTIVE").isLocked(false)
                .totalBalance(new BigDecimal("100")).availableBalance(new BigDecimal("50"))
                .allocatedBalance(new BigDecimal("50")).frozenBalance(BigDecimal.ZERO).build();
        card = InfiniCard.builder().id(9L).userId(1L).infiniCardId("infini-9").status(CardStatus.ACTIVE)
                .remainingAmount(new BigDecimal("50")).build();
        request = new InfiniFundingRequest();
        request.setRequestId(UUID.randomUUID());
        request.setAmount(new BigDecimal("20.00"));
        lenient().when(wallets.findByUserIdAndCurrencyForUpdate(1L, Stablecoin.USD)).thenReturn(Optional.of(wallet));
        lenient().when(cards.findByIdAndUserIdForUpdate(9L, 1L)).thenReturn(Optional.of(card));
        lenient().when(cards.findByIdForUpdate(9L)).thenReturn(Optional.of(card));
    }

    private InfiniFundingOrder reserve(String type, BigDecimal providerBalance) {
        var order = ledger.reserve(type, request, 1L, 9L, "USDT", providerBalance).order();
        lenient().when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        return order;
    }

    private void assertTotalUnchanged() {
        assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("100")));
        assertEquals(0, wallet.getAvailableBalance().compareTo(wallet.getTotalBalance()
                .subtract(wallet.getAllocatedBalance()).subtract(wallet.getFrozenBalance())));
    }

    @Test void topUpHoldsThenSettlesOnce() throws Exception {
        var order = reserve(TOP_UP, null);
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("30")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("20")));
        assertTrue(ledger.markSubmitting(order.getRequestId()));
        var result = new ObjectMapper().readTree("{\"tx_id\":\"tx_1\",\"card_balance\":\"70.00\"}");
        ledger.succeeded(order.getRequestId(), result);
        ledger.succeeded(order.getRequestId(), result);
        assertEquals(SUCCEEDED, order.getStatus());
        assertEquals("tx_1", order.getProviderTxId());
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("70")));
        assertEquals(0, card.getRemainingAmount().compareTo(new BigDecimal("70")));
        assertEquals(0, order.getProviderCardBalance().compareTo(new BigDecimal("70")));
        assertTotalUnchanged();
    }

    @Test void topUpRejectionReleasesHoldOnce() {
        var order = reserve(TOP_UP, null);
        ledger.rejected(order.getRequestId(), "PROVIDER_ERROR_30003");
        ledger.rejected(order.getRequestId(), "PROVIDER_ERROR_30003");
        assertEquals(FAILED, order.getStatus());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("50")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
        assertTotalUnchanged();
    }

    @Test void topUpNeedsEnoughWalletBalance() {
        request.setAmount(new BigDecimal("50.01"));
        assertThrows(WalletException.class, () -> ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null));
        verify(orders, never()).saveAndFlush(any());
    }

    @Test void redeemSettlesIntoWallet() throws Exception {
        var order = reserve(REDEEM, new BigDecimal("40"));
        // Nothing is held for a withdrawal.
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("50")));
        ledger.markSubmitting(order.getRequestId());
        ledger.succeeded(order.getRequestId(), new ObjectMapper().readTree("{\"tx_id\":\"tx_2\",\"card_balance\":\"20\"}"));
        assertEquals(SUCCEEDED, order.getStatus());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("70")));
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("30")));
        assertEquals(0, card.getRemainingAmount().compareTo(new BigDecimal("30")));
        assertTotalUnchanged();
    }

    @Test void redeemUsesTheRealProviderBalance() {
        // Local card shows 50, but Infini reports 15 after spending.
        assertThrows(WalletException.class, () -> ledger.reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("15")));
        verify(orders, never()).saveAndFlush(any());
    }

    @Test void redeemRejectionMovesNothing() {
        var order = reserve(REDEEM, new BigDecimal("40"));
        ledger.rejected(order.getRequestId(), "PROVIDER_REJECTED");
        assertEquals(FAILED, order.getStatus());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("50")));
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("50")));
    }

    @Test void blockedCardIsRejected() {
        card.setStatus(CardStatus.BLOCKED);
        assertThrows(WalletException.class, () -> ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null));
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
    }

    @Test void secondOrderOnTheSameCardIsRejectedWithTheFirstId() {
        var first = new InfiniFundingOrder();
        first.setRequestId("first");
        when(orders.findFirstByCardIdAndStatusInOrderByCreatedAtAsc(eq(9L), any())).thenReturn(Optional.of(first));
        var ex = assertThrows(WalletException.class, () -> ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null));
        assertEquals(Map.of("request_id", "first"), ex.getData());
    }

    @Test void retryReturnsTheStoredOrderAndChangedContentConflicts() {
        var order = reserve(TOP_UP, null);
        when(orders.findById(order.getRequestId())).thenReturn(Optional.of(order));
        assertFalse(ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null).submit());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("20")));
        request.setAmount(new BigDecimal("21"));
        assertThrows(WalletException.class, () -> ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null));
    }

    @Test void unknownResultKeepsHoldUntilProviderReportsCompletion() {
        var order = reserve(TOP_UP, null);
        ledger.markSubmitting(order.getRequestId());
        ledger.review(order.getRequestId(), "SUBMIT_RESULT_UNKNOWN");
        assertEquals(REVIEW_REQUIRED, order.getStatus());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("20")));
        ledger.resolveFromProvider(order.getRequestId(), "tx_9", true);
        assertEquals(SUCCEEDED, order.getStatus());
        assertEquals("tx_9", order.getProviderTxId());
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("70")));
        assertTotalUnchanged();
    }

    @Test void providerFailureReleasesAnUnknownTopUp() {
        var order = reserve(TOP_UP, null);
        ledger.markSubmitting(order.getRequestId());
        ledger.review(order.getRequestId(), "SUBMIT_RESULT_UNKNOWN");
        ledger.resolveFromProvider(order.getRequestId(), "tx_9", false);
        assertEquals(FAILED, order.getStatus());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("50")));
    }

    @Test void neverSentOrderIsReleasedAndSentOrderIsNot() {
        var order = reserve(TOP_UP, null);
        ledger.markSubmitting(order.getRequestId());
        ledger.expireUnsent(order.getRequestId()); // may have reached Infini
        assertEquals(PENDING, order.getStatus());
        ledger.reviewInterrupted(order.getRequestId());
        assertEquals(REVIEW_REQUIRED, order.getStatus());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("20")));

        request.setRequestId(UUID.randomUUID());
        var unsent = reserve(REDEEM, new BigDecimal("40"));
        ledger.expireUnsent(unsent.getRequestId());
        assertEquals(FAILED, unsent.getStatus());
        assertFalse(ledger.markSubmitting(unsent.getRequestId()));
    }

    @Test void providerFailureAfterSuccessOnlyFlagsTheOrder() throws Exception {
        var order = reserve(TOP_UP, null);
        ledger.markSubmitting(order.getRequestId());
        ledger.succeeded(order.getRequestId(), new ObjectMapper().readTree("{\"tx_id\":\"tx_1\"}"));
        ledger.flagProviderFailure(order.getRequestId());
        assertEquals(SUCCEEDED, order.getStatus());
        assertEquals("PROVIDER_REPORTED_FAILED", order.getReviewReason());
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("70")));
    }

    @Test void withdrawalMayBeSmallButTheCardKeepsOneDollar() {
        request.setAmount(new BigDecimal("0.50"));
        assertTrue(ledger.reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("1.50")).submit());

        request.setRequestId(UUID.randomUUID());
        request.setAmount(new BigDecimal("9.00"));
        assertTrue(ledger.reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("10")).submit());

        request.setRequestId(UUID.randomUUID());
        request.setAmount(new BigDecimal("9.01"));
        var ex = assertThrows(WalletException.class,
                () -> ledger.reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("10")));
        assertEquals("infini.funding.min_card_balance", ex.getMessageKey());
    }

    @Test void topUpBelowTenIsRejected() {
        request.setAmount(new BigDecimal("9.99"));
        var ex = assertThrows(WalletException.class, () -> ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null));
        assertEquals("infini.funding.topup_min", ex.getMessageKey());
        assertEquals(0, wallet.getFrozenBalance().signum());
    }

    @Test void withdrawalWaitsThirtyMinutesAfterATopUp() {
        var topUp = new InfiniFundingOrder();
        topUp.setSubmitStartedAt(java.time.Instant.now().minusSeconds(29 * 60));
        when(orders.findFirstByCardIdAndTypeAndStatusOrderBySubmitStartedAtDesc(9L, TOP_UP, SUCCEEDED))
                .thenReturn(Optional.of(topUp));
        var ex = assertThrows(WalletException.class,
                () -> ledger.reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("40")));
        assertEquals("infini.funding.withdraw_cooldown", ex.getMessageKey());
        assertEquals(Map.of("available_at", topUp.getSubmitStartedAt().plusSeconds(31 * 60).toString()), ex.getData());
        verify(orders, never()).saveAndFlush(any());

        topUp.setSubmitStartedAt(java.time.Instant.now().minusSeconds(32 * 60));
        assertTrue(ledger.reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("40")).submit());
    }

    @Test void responseExplainsKnownProviderRejections() {
        var order = reserve(REDEEM, new BigDecimal("40"));
        ledger.rejected(order.getRequestId(), "PROVIDER_ERROR_21026");
        assertEquals("WITHDRAW_COOLDOWN",
                com.example.bank.dto.response.wallet.infini.InfiniFundingResponse.from(order).getFailureReason());
    }
}
