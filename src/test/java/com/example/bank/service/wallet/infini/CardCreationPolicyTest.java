package com.example.bank.service.wallet.infini;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CardCreationPolicyTest {
    @Test void minimumPrecisionAndMaximumIncludeCreationFee() {
        assertThrows(RuntimeException.class, () -> CardCreationPolicy.totalDebit(new BigDecimal("9.99")));
        assertThrows(RuntimeException.class, () -> CardCreationPolicy.totalDebit(new BigDecimal("10.001")));
        assertEquals(new BigDecimal("11.00"), CardCreationPolicy.totalDebit(new BigDecimal("10.00")));
        assertEquals(new BigDecimal("49.00"), CardCreationPolicy.maxAmount(new BigDecimal("50")));
        assertEquals(new BigDecimal("0.00"), CardCreationPolicy.maxAmount(BigDecimal.ZERO));
    }
}
