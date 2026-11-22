package com.example.bank.service.wallet.user.creation;

import com.example.bank.service.wallet.user.impl.CardCreationTransactionServiceImpl;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.entity.user.User;
import com.example.bank.entity.wallet.*;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardCreationLedgerTest {
    @Mock CardCreationOrderRepository orders;
    @Mock WalletRepository wallets;
    @Mock UserRepository users;
    @Mock CardRepository legacyCards;
    @Mock InfiniCardRepository infiniCards;
    @InjectMocks CardCreationTransactionServiceImpl ledger;
    Wallet wallet;
    CreateCardRequest request;

    @BeforeEach void setup() {
        wallet = Wallet.builder().id(5L).userId(1L).currency(Stablecoin.USD)
                .status("ACTIVE").isLocked(false).availableBalance(new BigDecimal("51"))
                .totalBalance(new BigDecimal("51")).frozenBalance(BigDecimal.ZERO)
                .allocatedBalance(BigDecimal.ZERO).build();
        request = new CreateCardRequest();
        request.setRequestId(UUID.randomUUID());
        request.setName("My card");
        request.setHolderName("Nguyen Van An");
        request.setAmount(new BigDecimal("50.00"));
        when(wallets.findByUserIdAndCurrencyForUpdate(1L, Stablecoin.USD)).thenReturn(Optional.of(wallet));
    }
    private void member() {
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L)
                .cardOpenLimit(10).build()));
    }
    @Test void fiftyRequiresFiftyOne() {
        wallet.setAvailableBalance(new BigDecimal("50"));
        assertThrows(RuntimeException.class, () -> ledger.reserve(request, 1L, "USDT", "member@example.com", 2));
        assertEquals(new BigDecimal("50"), wallet.getAvailableBalance());
        verify(orders, never()).saveAndFlush(any());
    }
    @Test void reserveThenSettleChargesFeeOnceAndPreservesAccounting() throws Exception {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        assertEquals(0, wallet.getAvailableBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
        assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("51")));
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        order.setProviderCardId("infini-123");
        when(infiniCards.saveAndFlush(any())).thenAnswer(invocation -> {
            InfiniCard card = invocation.getArgument(0);
            assertEquals("infini-123", card.getInfiniCardId());
            assertEquals(new BigDecimal("1.00"), card.getCreationFee());
            card.setId(9L);
            return card;
        });
        var snapshot = new ObjectMapper().readTree(
                "{\"id\":\"infini-123\",\"status\":\"active\",\"currency\":\"USD\",\"available_balance\":\"50.00\"}");
        ledger.synchronize(order.getRequestId(), snapshot);
        ledger.synchronize(order.getRequestId(), snapshot);
        assertEquals("SUCCEEDED", order.getStatus());
        assertEquals("member@example.com", order.getApplicantEmail());
        assertEquals(2, order.getProductId());
        assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("50")));
        assertEquals(0, wallet.getAllocatedBalance().compareTo(new BigDecimal("50")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, wallet.getAvailableBalance().compareTo(
                wallet.getTotalBalance().subtract(wallet.getAllocatedBalance()).subtract(wallet.getFrozenBalance())));
        verify(infiniCards, times(1)).saveAndFlush(any());
        verify(legacyCards, never()).saveAndFlush(any());
    }
    @Test void walletFiftyAllowsFortyNine() {
        member();
        wallet.setAvailableBalance(new BigDecimal("50"));
        request.setAmount(new BigDecimal("49"));
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        assertEquals(0, order.getTotalDebit().compareTo(new BigDecimal("50")));
        assertEquals(0, wallet.getAvailableBalance().compareTo(BigDecimal.ZERO));
    }
    @Test void minimumTenRequiresEleven() {
        request.setAmount(new BigDecimal("10"));
        wallet.setAvailableBalance(new BigDecimal("10"));
        assertThrows(RuntimeException.class, () -> ledger.reserve(request, 1L, "USDT", "member@example.com", 2));
        wallet.setAvailableBalance(new BigDecimal("11"));
        member();
        assertEquals(0, ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order().getTotalDebit()
                .compareTo(new BigDecimal("11")));
    }
    @Test void repeatedRequestDoesNotReserveAgain() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        when(orders.findById(order.getRequestId())).thenReturn(Optional.of(order));
        assertFalse(ledger.reserve(request, 1L, "USDT", "member@example.com", 1).submit());
        assertEquals(2, order.getProductId());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
        verify(orders, times(1)).saveAndFlush(any());
    }
    @Test void changedRequestIdPayloadIsRejected() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        when(orders.findById(order.getRequestId())).thenReturn(Optional.of(order));
        request.setAmount(new BigDecimal("49"));
        assertThrows(RuntimeException.class, () -> ledger.reserve(request, 1L, "USDT", "member@example.com", 2));
    }
    @Test void unknownResultKeepsReservation() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.review(order.getRequestId(), "APPLY_RESULT_UNKNOWN");
        assertEquals("REVIEW_REQUIRED", order.getStatus());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
    }
    @Test void definitiveRejectionReleasesHoldOnlyOnce() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.rejected(order.getRequestId(), "PROVIDER_REJECTED");
        ledger.rejected(order.getRequestId(), "PROVIDER_REJECTED");
        assertEquals("FAILED", order.getStatus());
        assertEquals("PROVIDER_REJECTED", order.getReviewReason());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("51")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, wallet.getTotalBalance().compareTo(new BigDecimal("51")));
    }

    @Test void submittedOrderIsNotReleasedAsUnsent() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        assertTrue(ledger.markSubmitting(order.getRequestId()));
        ledger.expireUnsent(order.getRequestId()); // request may have been sent: must not release
        assertEquals("PENDING", order.getStatus());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
    }

    private CardCreationOrder submitted(java.time.Instant startedAt) {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        order.setApplyStartedAt(startedAt);
        order.setStatus("REVIEW_REQUIRED");
        order.setReviewReason("APPLY_RESULT_UNKNOWN");
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        return order;
    }

    @Test void lookupFindingOneCardResumesNormalFlow() {
        var order = submitted(java.time.Instant.now().minusSeconds(300));
        ledger.providerLookup(order.getRequestId(), List.of("infini-123"));
        assertEquals("infini-123", order.getProviderCardId());
        assertEquals("PENDING", order.getStatus());
        assertNull(order.getReviewReason());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
    }

    @Test void lookupFindingSeveralCardsNeedsReview() {
        var order = submitted(java.time.Instant.now().minusSeconds(300));
        ledger.providerLookup(order.getRequestId(), List.of("a", "b"));
        assertNull(order.getProviderCardId());
        assertEquals("REVIEW_REQUIRED", order.getStatus());
        assertEquals("PROVIDER_LOOKUP_AMBIGUOUS", order.getReviewReason());
    }

    @Test void lookupFindingNothingSoonAfterSendingKeepsFunds() {
        var order = submitted(java.time.Instant.now().minusSeconds(300));
        ledger.providerLookup(order.getRequestId(), List.of());
        assertEquals("REVIEW_REQUIRED", order.getStatus());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
    }

    @Test void lookupFindingNothingLongAfterSendingReleasesFundsOnce() {
        var order = submitted(java.time.Instant.now().minusSeconds(2 * 3600));
        ledger.providerLookup(order.getRequestId(), List.of());
        ledger.providerLookup(order.getRequestId(), List.of());
        assertEquals("FAILED", order.getStatus());
        assertEquals("PROVIDER_CARD_NOT_FOUND", order.getReviewReason());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("51")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(BigDecimal.ZERO));
    }

    @Test void temporaryStatusFailureKeepsOrderPending() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        order.setProviderCardId("infini-123");
        order.setCreatedAt(java.time.Instant.now().minusSeconds(60));
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.statusUnavailable(order.getRequestId());
        assertEquals("PENDING", order.getStatus());
        assertTrue(order.getNextCheckAt().isAfter(java.time.Instant.now()));
    }

    @Test void longStatusOutageAsksForReviewButKeepsFunds() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        order.setProviderCardId("infini-123");
        order.setCreatedAt(java.time.Instant.now().minusSeconds(3600));
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.statusUnavailable(order.getRequestId());
        assertEquals("REVIEW_REQUIRED", order.getStatus());
        assertEquals("STATUS_UNAVAILABLE", order.getReviewReason());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
    }

    @Test void neverSentOrderIsReleasedAndCannotBeSentAfterwards() {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.expireUnsent(order.getRequestId());
        assertEquals("FAILED", order.getStatus());
        assertEquals(0, wallet.getAvailableBalance().compareTo(new BigDecimal("51")));
        assertFalse(ledger.markSubmitting(order.getRequestId()));
    }

    @Test void undocumentedProviderStatusKeepsFundsForReview() throws Exception {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        order.setProviderCardId("infini-123");
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.synchronize(order.getRequestId(),
                new ObjectMapper().readTree("{\"id\":\"infini-123\",\"status\":\"failed\"}"));
        assertEquals("REVIEW_REQUIRED", order.getStatus());
        assertEquals("UNEXPECTED_PROVIDER_STATUS", order.getReviewReason());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
        verify(infiniCards, never()).saveAndFlush(any());
    }

    @Test void longPendingProviderOrderMovesToReview() throws Exception {
        member();
        var order = ledger.reserve(request, 1L, "USDT", "member@example.com", 2).order();
        order.setProviderCardId("infini-123");
        order.setCreatedAt(java.time.Instant.now().minusSeconds(3600));
        when(orders.findForUpdate(order.getRequestId())).thenReturn(Optional.of(order));
        ledger.synchronize(order.getRequestId(),
                new ObjectMapper().readTree("{\"id\":\"infini-123\",\"status\":\"pending\"}"));
        assertEquals("REVIEW_REQUIRED", order.getStatus());
        assertEquals("PROVIDER_TIMEOUT", order.getReviewReason());
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("51")));
    }

    @Test void secondOrderIsRejectedWhileOneIsPending() {
        member();
        var first = new CardCreationOrder();
        first.setRequestId("existing-request");
        when(orders.findFirstByUserIdAndStatusInOrderByCreatedAtAsc(eq(1L), any()))
                .thenReturn(Optional.of(first));
        var ex = assertThrows(com.example.bank.common.exception.wallet.WalletException.class,
                () -> ledger.reserve(request, 1L, "USDT", "member@example.com", 2));
        assertEquals(java.util.Map.of("request_id", "existing-request"), ex.getData());
        verify(orders, never()).saveAndFlush(any());
    }

    @Test void limitIncludesBothLegacyAndInfiniCards() {
        member();
        when(legacyCards.countByUserId(1L)).thenReturn(4L);
        when(infiniCards.countByUserId(1L)).thenReturn(6L);
        assertThrows(RuntimeException.class,
                () -> ledger.reserve(request, 1L, "USDT", "member@example.com", 2));
        verify(orders, never()).saveAndFlush(any());
    }
}
