package com.example.bank.common.config.infini;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InfiniCardClientTest {
    @Test void errorMessagesKeepTheReasonButHideSensitiveValues() {
        assertEquals("minimum balance is 10.00 USD", InfiniCardClient.maskMessage("minimum balance is 10.00 USD"));
        assertEquals("card <digits> of <email> <id>", InfiniCardClient.maskMessage(
                "card 4111 1111 1111 1335 of a.b@example.com 805abbd2-1402-4e83-8953-3e7c76b9464e"));
        assertEquals(200, InfiniCardClient.maskMessage("x".repeat(500)).length());
    }
}
