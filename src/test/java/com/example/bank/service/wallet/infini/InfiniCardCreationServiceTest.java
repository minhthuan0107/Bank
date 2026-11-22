package com.example.bank.service.wallet.infini;

import com.example.bank.service.wallet.infini.impl.CardCreationServiceImpl;
import com.example.bank.service.wallet.infini.CardCreationTransactionService;
import com.example.bank.common.config.infini.*;
import com.example.bank.dto.request.wallet.infini.CreateCardRequest;
import com.example.bank.entity.wallet.infini.CardCreationOrder;
import com.example.bank.repository.wallet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.bank.repository.wallet.infini.CardCreationOrderRepository;

@ExtendWith(MockitoExtension.class)
class InfiniCardCreationServiceTest {
    @Mock InfiniProperties properties;
    @Mock InfiniCardClient client;
    @Mock CardCreationTransactionService ledger;
    @Mock CardCreationOrderRepository orders;
    @Mock WalletRepository wallets;
    @InjectMocks CardCreationServiceImpl service;
    CreateCardRequest request;
    CardCreationOrder order;
    @BeforeEach void setup() {
        request = new CreateCardRequest();
        request.setRequestId(UUID.randomUUID());
        request.setAmount(new BigDecimal("10.00"));
        order = new CardCreationOrder();
        order.setRequestId(request.getRequestId().toString());
        order.setStatus("PENDING");
        lenient().when(properties.getProductId()).thenReturn(2);
        lenient().when(properties.getTokenType()).thenReturn("USDT");
        lenient().when(properties.getApplicantEmail()).thenReturn("member@example.com");
        lenient().when(orders.findByRequestIdAndUserId(order.getRequestId(), 1L)).thenReturn(Optional.of(order));
    }
    @Test void retryDoesNotSubmitApplicationAgain() throws Exception {
        when(ledger.reserve(request, 1L, "USDT", "member@example.com", 2)).thenReturn(new CardCreationTransactionService.Reservation(order, false));
        service.createCard(request, 1L);
        verify(client, never()).apply(any());
    }
    @Test void timeoutHoldsFundsAndDoesNotRetryPost() throws Exception {
        when(ledger.reserve(request, 1L, "USDT", "member@example.com", 2)).thenReturn(new CardCreationTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(true);
        when(client.apply(order)).thenThrow(new HttpTimeoutException("timeout"));
        service.createCard(request, 1L);
        verify(client, times(1)).apply(order);
        verify(ledger).review(order.getRequestId(), "APPLY_RESULT_UNKNOWN");
        verify(ledger, never()).rejected(any(), any());
    }
    @Test void definiteRejectionReleasesReservation() throws Exception {
        when(ledger.reserve(request, 1L, "USDT", "member@example.com", 2)).thenReturn(new CardCreationTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(true);
        when(client.apply(order)).thenThrow(new InfiniCardClient.DefiniteRejection("PROVIDER_REJECTED"));
        service.createCard(request, 1L);
        verify(ledger).rejected(order.getRequestId(), "PROVIDER_REJECTED");
        verify(ledger, never()).review(any(), any());
    }
    @Test void doesNotSendWhenOrderIsNoLongerPending() throws Exception {
        when(ledger.reserve(request, 1L, "USDT", "member@example.com", 2)).thenReturn(new CardCreationTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(false);
        service.createCard(request, 1L);
        verify(client, never()).apply(any());
    }
    @Test void createDoesNotCheckStatusInline() throws Exception {
        when(ledger.reserve(request, 1L, "USDT", "member@example.com", 2)).thenReturn(new CardCreationTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(true);
        service.createCard(request, 1L);
        verify(client, never()).status(any());
    }
    @Test void schedulerExpiresUnsentOrdersAndLooksUpSubmittedOnes() throws Exception {
        var card = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"id\":\"infini-9\"}");
        when(properties.isEnabled()).thenReturn(true);
        when(orders.findDue(any(), any())).thenReturn(List.of());
        when(orders.findUnsent(any(), any())).thenReturn(List.of("unsent"));
        when(orders.findAwaitingProviderId(any(), any(), any())).thenReturn(List.of("sent"));
        when(client.findByAlias("sent")).thenReturn(List.of(card));
        service.reconcilePendingOrders();
        verify(ledger).expireUnsent("unsent");
        verify(ledger).providerLookup("sent", List.of("infini-9"));
    }

    @Test void failedLookupKeepsOrderAndTriesLater() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(orders.findDue(any(), any())).thenReturn(List.of());
        when(orders.findUnsent(any(), any())).thenReturn(List.of());
        when(orders.findAwaitingProviderId(any(), any(), any())).thenReturn(List.of("sent"));
        when(client.findByAlias("sent")).thenThrow(new HttpTimeoutException("timeout"));
        service.reconcilePendingOrders();
        verify(ledger).statusUnavailable("sent");
        verify(ledger, never()).providerLookup(any(), any());
    }

    @Test void statusFailureDoesNotMoveOrderToReviewDirectly() throws Exception {
        order.setProviderCardId("infini-1");
        when(orders.findById(order.getRequestId())).thenReturn(Optional.of(order));
        when(client.status("infini-1")).thenThrow(new HttpTimeoutException("timeout"));
        service.reconcile(order.getRequestId());
        verify(ledger).statusUnavailable(order.getRequestId());
        verify(ledger, never()).review(any(), any());
    }
}
