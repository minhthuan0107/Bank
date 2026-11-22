package com.example.bank.service.wallet.infini;

import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.CardCreationOrder;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.entity.wallet.infini.InfiniWebhookEvent;
import com.example.bank.repository.wallet.infini.CardCreationOrderRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.repository.wallet.infini.InfiniWebhookEventRepository;
import com.example.bank.service.wallet.infini.CardCreationService;
import com.example.bank.service.wallet.infini.CardCreationTransactionService;
import com.example.bank.service.wallet.infini.InfiniFundingTransactionService;
import com.example.bank.service.wallet.infini.impl.InfiniWebhookServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfiniWebhookServiceTest {
    static final String SECRET = "test-secret";

    @Mock InfiniProperties properties;
    @Mock TransactionTemplate tx;
    @Mock InfiniWebhookEventRepository events;
    @Mock InfiniFundingOrderRepository fundingOrders;
    @Mock InfiniFundingTransactionService fundingLedger;
    @Mock CardCreationOrderRepository creationOrders;
    @Mock CardCreationTransactionService creationLedger;
    @Mock CardCreationService creationService;
    InfiniWebhookServiceImpl service;
    InfiniWebhookEvent stored;

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        service = new InfiniWebhookServiceImpl(properties, new ObjectMapper(), tx, events, fundingOrders,
                fundingLedger, creationOrders, creationLedger, creationService);
        lenient().when(properties.getWebhookSecret()).thenReturn(SECRET);
        lenient().when(tx.execute(any())).thenAnswer(i -> ((TransactionCallback<Object>) i.getArgument(0))
                .doInTransaction(mock(TransactionStatus.class)));
        lenient().doAnswer(i -> { ((Consumer<TransactionStatus>) i.getArgument(0)).accept(mock(TransactionStatus.class)); return null; })
                .when(tx).executeWithoutResult(any());
        // Keep the stored event in memory.
        lenient().when(events.saveAndFlush(any())).thenAnswer(i -> {
            stored = i.getArgument(0);
            return stored;
        });
        lenient().when(events.findById(any())).thenAnswer(i -> Optional.ofNullable(stored));
    }

    private static String sign(String ts, String id, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((ts + "." + id + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private void deliver(String id, String body) throws Exception {
        String ts = Long.toString(Instant.now().getEpochSecond());
        service.receive(ts, id, "v1", sign(ts, id, body), body.getBytes(StandardCharsets.UTF_8));
    }

    private static String transaction(String type, String status, String amount) {
        return "{\"id\":\"evt\",\"event\":\"card.transaction\",\"version\":1,\"data\":{" +
                "\"card\":{\"card_id\":\"infini-9\"},\"transaction_id\":\"tx_5\",\"type\":\"" + type +
                "\",\"status\":\"" + status + "\",\"amount\":\"" + amount + "\"}}";
    }

    private InfiniFundingOrder unknownTopUp() {
        var order = new InfiniFundingOrder();
        order.setRequestId("req-1");
        order.setAmount(new BigDecimal("20.0000"));
        when(fundingOrders.findByProviderTxId("tx_5")).thenReturn(Optional.empty());
        when(fundingOrders.findUnresolvedSent("infini-9", "TOP_UP")).thenReturn(List.of(order));
        return order;
    }

    @Test void completedTopUpResolvesTheUnknownOrder() throws Exception {
        unknownTopUp();
        deliver("evt-1", transaction("top_up", "completed", "20.00"));
        verify(fundingLedger).resolveFromProvider("req-1", "tx_5", true);
        assertEquals(InfiniWebhookEvent.PROCESSED, stored.getStatus());
    }

    @Test void failedTopUpReleasesTheUnknownOrder() throws Exception {
        unknownTopUp();
        deliver("evt-1", transaction("top_up", "failed", "20.00"));
        verify(fundingLedger).resolveFromProvider("req-1", "tx_5", false);
    }

    @Test void amountMismatchIsNeverAttached() throws Exception {
        unknownTopUp();
        deliver("evt-1", transaction("top_up", "completed", "25.00"));
        verify(fundingLedger, never()).resolveFromProvider(any(), any(), anyBoolean());
        assertEquals(InfiniWebhookEvent.IGNORED, stored.getStatus());
    }

    @Test void pendingStatusIsNotFinal() throws Exception {
        when(fundingOrders.findByProviderTxId("tx_5")).thenReturn(Optional.empty());
        deliver("evt-1", transaction("top_up", "pending", "20.00"));
        verify(fundingLedger, never()).resolveFromProvider(any(), any(), anyBoolean());
    }

    @Test void duplicateDeliveryIsProcessedOnce() throws Exception {
        unknownTopUp();
        deliver("evt-1", transaction("top_up", "completed", "20.00"));
        when(events.existsById("evt-1")).thenReturn(true);
        deliver("evt-1", transaction("top_up", "completed", "20.00"));
        verify(fundingLedger, times(1)).resolveFromProvider(any(), any(), anyBoolean());
    }

    @Test void badSignatureIsRejectedAndNotStored() {
        String ts = Long.toString(Instant.now().getEpochSecond());
        var ex = assertThrows(WalletException.class, () -> service.receive(ts, "evt-1", "v1", "00ff",
                transaction("top_up", "completed", "20").getBytes(StandardCharsets.UTF_8)));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(events, never()).saveAndFlush(any());
    }

    @Test void oldTimestampIsRejected() throws Exception {
        String ts = Long.toString(Instant.now().minusSeconds(2 * 3600).getEpochSecond());
        String body = transaction("top_up", "completed", "20");
        var ex = assertThrows(WalletException.class, () -> service.receive(ts, "evt-1", "v1",
                sign(ts, "evt-1", body), body.getBytes(StandardCharsets.UTF_8)));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test void missingSecretIsNotAcknowledged() {
        when(properties.getWebhookSecret()).thenReturn("");
        var ex = assertThrows(WalletException.class, () -> deliver("evt-1", "{}"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    @Test void cardStatusChangeSpeedsUpCardCreation() throws Exception {
        var creation = new CardCreationOrder();
        creation.setRequestId("creation-1");
        when(creationOrders.findById("creation-1")).thenReturn(Optional.of(creation));
        deliver("evt-2", "{\"id\":\"evt-2\",\"event\":\"card.status_change\",\"version\":1,\"data\":{" +
                "\"card\":{\"card_id\":\"infini-10\",\"alias\":\"creation-1\",\"status\":\"active\"}}}");
        verify(creationLedger).providerLookup("creation-1", List.of("infini-10"));
        verify(creationService).reconcile("creation-1");
    }

    @Test void processingErrorKeepsTheEventForRetry() throws Exception {
        unknownTopUp();
        doThrow(new IllegalStateException("db")).when(fundingLedger).resolveFromProvider(any(), any(), anyBoolean());
        deliver("evt-1", transaction("top_up", "completed", "20.00"));
        assertEquals(InfiniWebhookEvent.RECEIVED, stored.getStatus());
        assertEquals(1, stored.getAttempts());
    }
}
