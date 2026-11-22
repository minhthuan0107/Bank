package com.example.bank.service.wallet.infini;

import com.example.bank.common.config.infini.InfiniCardClient;
import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.service.wallet.infini.InfiniFundingTransactionService;
import com.example.bank.service.wallet.infini.impl.InfiniFundingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.UUID;

import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.REDEEM;
import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.TOP_UP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfiniFundingServiceTest {
    @Mock InfiniProperties properties;
    @Mock InfiniCardClient client;
    @Mock InfiniFundingTransactionService ledger;
    @Mock InfiniFundingOrderRepository orders;
    @Mock InfiniCardRepository cards;
    @InjectMocks InfiniFundingServiceImpl service;

    InfiniFundingRequest request;
    InfiniFundingOrder order;

    @BeforeEach void setup() {
        request = new InfiniFundingRequest();
        request.setRequestId(UUID.randomUUID());
        request.setAmount(new BigDecimal("10.00"));
        order = new InfiniFundingOrder();
        order.setRequestId(request.getRequestId().toString());
        order.setType(TOP_UP);
        order.setStatus("PENDING");
        lenient().when(properties.getTokenType()).thenReturn("USDT");
        lenient().when(orders.findByRequestIdAndUserId(order.getRequestId(), 1L)).thenReturn(Optional.of(order));
    }

    @Test void successfulTopUpSettles() throws Exception {
        var result = new ObjectMapper().readTree("{\"tx_id\":\"tx_1\",\"card_balance\":\"20\"}");
        when(ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null)).thenReturn(new InfiniFundingTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(true);
        when(client.topUp(order)).thenReturn(result);
        service.topUp(1L, 9L, request);
        verify(ledger).succeeded(order.getRequestId(), result);
    }

    @Test void timeoutKeepsTheHoldAndNeverResends() throws Exception {
        when(ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null)).thenReturn(new InfiniFundingTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(true);
        when(client.topUp(order)).thenThrow(new HttpTimeoutException("timeout"));
        service.topUp(1L, 9L, request);
        verify(client, times(1)).topUp(order);
        verify(ledger).review(order.getRequestId(), "SUBMIT_RESULT_UNKNOWN");
        verify(ledger, never()).rejected(any(), any());
    }

    @Test void definiteRejectionReleases() throws Exception {
        when(ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null)).thenReturn(new InfiniFundingTransactionService.Reservation(order, true));
        when(ledger.markSubmitting(order.getRequestId())).thenReturn(true);
        when(client.topUp(order)).thenThrow(new InfiniCardClient.DefiniteRejection("PROVIDER_ERROR_1"));
        service.topUp(1L, 9L, request);
        verify(ledger).rejected(order.getRequestId(), "PROVIDER_ERROR_1");
    }

    @Test void retryOfAStoredRequestIsNotSentAgain() throws Exception {
        when(ledger.reserve(TOP_UP, request, 1L, 9L, "USDT", null)).thenReturn(new InfiniFundingTransactionService.Reservation(order, false));
        service.topUp(1L, 9L, request);
        verify(client, never()).topUp(any());
    }

    @Test void withdrawReadsTheRealCardBalanceFirst() throws Exception {
        order.setType(REDEEM);
        var card = InfiniCard.builder().id(9L).userId(1L).infiniCardId("infini-9").status(CardStatus.ACTIVE).build();
        when(cards.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(card));
        when(client.status("infini-9")).thenReturn(new ObjectMapper().readTree(
                "{\"id\":\"infini-9\",\"status\":\"active\",\"available_balance\":\"33.50\"}"));
        when(ledger.reserve(eq(REDEEM), eq(request), eq(1L), eq(9L), eq("USDT"), any()))
                .thenReturn(new InfiniFundingTransactionService.Reservation(order, false));
        service.withdraw(1L, 9L, request);
        verify(ledger).reserve(REDEEM, request, 1L, 9L, "USDT", new BigDecimal("33.50"));
    }

    @Test void withdrawFailsSafelyWhenTheBalanceCannotBeRead() throws Exception {
        var card = InfiniCard.builder().id(9L).userId(1L).infiniCardId("infini-9").status(CardStatus.ACTIVE).build();
        when(cards.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(card));
        when(client.status("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        assertThrows(WalletException.class, () -> service.withdraw(1L, 9L, request));
        verify(ledger, never()).reserve(any(), any(), any(), any(), any(), any());
    }

    @Test void belowMinimumIsRejectedBeforeAnything() {
        request.setAmount(new BigDecimal("9.99"));
        assertThrows(WalletException.class, () -> service.topUp(1L, 9L, request));
        verifyNoInteractions(ledger);
    }
}
