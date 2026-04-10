package com.example.bank.dto.response.wallet.user;

import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class WithdrawOrderListResponse {
    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("currency")
    private Stablecoin currency;

    @JsonProperty("network")
    private String network;

    @JsonProperty("to_address")
    private String toAddress;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("status")
    private WithdrawOrderStatus status;

    @JsonProperty("created_at")
    private Instant createdAt;

    @Column(name = "admin_note")
    private String adminNote;

    public static WithdrawOrderListResponse from(WithdrawOrder order) {
        return WithdrawOrderListResponse.builder()
                .orderNo(order.getOrderNo())
                .currency(order.getCurrency())
                .network(order.getNetwork())
                .toAddress(order.getToAddress())
                .amount(order.getAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .adminNote(order.getAdminNote())
                .build();
    }
}
