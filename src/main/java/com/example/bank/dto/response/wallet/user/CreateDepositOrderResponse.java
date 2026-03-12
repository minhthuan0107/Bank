package com.example.bank.dto.response.wallet.user;

import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;


import java.math.BigDecimal;

@Getter
@Builder
public class CreateDepositOrderResponse {

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

    @JsonProperty("fee")
    private BigDecimal fee;

    @JsonProperty("expected_amount")
    private BigDecimal expectedAmount;

    @JsonProperty("status")
    private String status;

    public static CreateDepositOrderResponse from(DepositOrder order) {
        return CreateDepositOrderResponse.builder()
                .orderNo(order.getOrderNo())
                .currency(order.getCurrency())
                .network(order.getNetwork())
                .address(order.getAddress())
                .amount(order.getAmount())
                .fee(order.getFee().stripTrailingZeros())
                .expectedAmount(order.getExpectedAmount().stripTrailingZeros())
                .status(order.getStatus().name())
                .build();
    }

}
