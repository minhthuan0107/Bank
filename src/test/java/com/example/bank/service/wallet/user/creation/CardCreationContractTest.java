package com.example.bank.service.wallet.user.creation;

import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.user.CardCreationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CardCreationContractTest {

    @Test
    void defaultsUseOneMerchantEmailAndUsdt() {
        InfiniProperties properties = new InfiniProperties();

        assertNull(properties.getApplicantEmail()); // must come from INFINI_APPLICANT_EMAIL
        assertEquals("USDT", properties.getTokenType());
        assertEquals(2, properties.getProductId());
    }

    @Test
    void responseUsesWalletJsonConvention() {
        ObjectMapper mapper = new ObjectMapper();
        CardCreationResponse response = CardCreationResponse.builder()
                .requestId("request")
                .status("PENDING")
                .amount(new BigDecimal("10.00"))
                .fee(new BigDecimal("1.00"))
                .totalDebit(new BigDecimal("11.00"))
                .build();

        var json = mapper.valueToTree(response);
        assertEquals("request", json.path("request_id").asText());
        assertEquals(0, json.path("total_debit").decimalValue().compareTo(new BigDecimal("11")));
        assertFalse(json.has("requestId"));
        assertFalse(json.has("totalDebit"));
        var requestJson = mapper.valueToTree(new CreateCardRequest());
        assertFalse(requestJson.has("email"));
        assertFalse(requestJson.has("userEmail"));
        assertFalse(requestJson.has("tokenType"));
        assertFalse(requestJson.has("productId"));
    }
}
