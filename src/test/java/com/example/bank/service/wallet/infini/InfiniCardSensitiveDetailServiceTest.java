package com.example.bank.service.wallet.infini;

import com.example.bank.common.config.infini.InfiniCardClient;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.service.wallet.infini.impl.InfiniCardSensitiveDetailServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.net.http.HttpTimeoutException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfiniCardSensitiveDetailServiceTest {
    @Mock InfiniCardClient client;
    @Mock InfiniCardRepository cards;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @InjectMocks InfiniCardSensitiveDetailServiceImpl service;

    InfiniCard card;

    @BeforeEach void setup() {
        card = InfiniCard.builder().id(9L).userId(1L).infiniCardId("infini-9")
                .status(CardStatus.ACTIVE).last4("1335").build();
        lenient().when(cards.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(card));
        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(values.increment(anyString())).thenReturn(1L);
    }

    private void reveal(String json) throws Exception {
        when(client.reveal("infini-9")).thenReturn(new ObjectMapper().readTree(json));
    }

    @Test void returnsTheCardDetailsInTheExistingFormat() throws Exception {
        reveal("{\"card_number\":\"4111 1111 1111 1335\",\"cvv\":\"123\",\"expiration_mmyy\":\"0829\",\"card_currency\":\"USD\"}");
        var result = service.getSensitiveDetail(1L, 9L);
        assertEquals("4111111111111335", result.pan());
        assertEquals("123", result.cvv());
        assertEquals(8, result.expiryMonth());
        assertEquals(2029, result.expiryYear());
    }

    @Test void anotherUsersCardIsNotFound() throws Exception {
        assertThrows(WalletException.class, () -> service.getSensitiveDetail(2L, 9L));
        verify(client, never()).reveal(any());
    }

    @Test void inactiveCardIsRejectedBeforeCallingInfini() throws Exception {
        card.setStatus(CardStatus.BLOCKED);
        var ex = assertThrows(WalletException.class, () -> service.getSensitiveDetail(1L, 9L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(client, never()).reveal(any());
    }

    @Test void tooManyRevealsAreRejectedBeforeCallingInfini() throws Exception {
        when(values.increment(anyString())).thenReturn(11L);
        var ex = assertThrows(WalletException.class, () -> service.getSensitiveDetail(1L, 9L));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        verify(client, never()).reveal(any());
    }

    @Test void rateLimitOutageFailsClosed() throws Exception {
        when(values.increment(anyString())).thenThrow(new IllegalStateException("redis down"));
        var ex = assertThrows(WalletException.class, () -> service.getSensitiveDetail(1L, 9L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        verify(client, never()).reveal(any());
    }

    @Test void detailsOfAnotherCardAreNeverReturned() throws Exception {
        reveal("{\"card_number\":\"4111111111119999\",\"cvv\":\"123\",\"expiration_mmyy\":\"0829\"}");
        assertThrows(WalletException.class, () -> service.getSensitiveDetail(1L, 9L));
    }

    @Test void malformedProviderDataIsRejected() throws Exception {
        reveal("{\"card_number\":\"4111111111111335\",\"cvv\":\"12\",\"expiration_mmyy\":\"1329\"}");
        assertThrows(WalletException.class, () -> service.getSensitiveDetail(1L, 9L));
    }

    @Test void providerErrorDoesNotLeakDetails() throws Exception {
        when(client.reveal("infini-9")).thenThrow(new HttpTimeoutException("timeout"));
        var ex = assertThrows(WalletException.class, () -> service.getSensitiveDetail(1L, 9L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }
}
