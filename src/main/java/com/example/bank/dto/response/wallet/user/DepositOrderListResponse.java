package com.example.bank.dto.response.wallet.user;

import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class DepositOrderListResponse {

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("currency")
    private Stablecoin currency;

    @JsonProperty("network")
    private String network;

    @JsonProperty("address")
    private String address;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("expected_amount")
    private BigDecimal expectedAmount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("admin_note")
    private String adminNote;

    @JsonProperty("created_at")
    private Instant createAt;

    public static DepositOrderListResponse from(DepositOrder order) {
        return DepositOrderListResponse.builder()
                .orderNo(order.getOrderNo())
                .currency(order.getCurrency())
                .network(order.getNetwork())
                .address(order.getSourceAddress())
                .amount(order.getAmount().stripTrailingZeros())
                .expectedAmount(order.getExpectedAmount().stripTrailingZeros())
                .status(order.getStatus().name())
                .adminNote(order.getAdminNote())
                .createAt(order.getCreatedAt())
                .build();
    }
}
