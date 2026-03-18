package com.example.bank.dto.response.wallet.user;

import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreateWithdrawOrderResponse {

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("status")
    private WithdrawOrderStatus status;

}
