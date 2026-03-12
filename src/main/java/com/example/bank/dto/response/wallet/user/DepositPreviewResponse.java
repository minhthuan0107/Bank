package com.example.bank.dto.response.wallet.user;

import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.entity.user.User;
import com.example.bank.entity.wallet.DepositAddress;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Builder
public class DepositPreviewResponse {

    @JsonProperty("payee_name")
    private String payeeName;

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

    @JsonProperty("currency")
    private Stablecoin currency;

    public static DepositPreviewResponse from(
            User admin,
            DepositAddress address,
            DepositPreviewRequest request,
            BigDecimal fee,
            BigDecimal expectedAmount
    ) {
        return DepositPreviewResponse.builder()
                .payeeName(admin.getUsername())
                .network(address.getNetwork())
                .address(address.getAddress())
                .amount(request.getAmount())
                .fee(fee.stripTrailingZeros())
                .expectedAmount(expectedAmount.stripTrailingZeros())
                .currency(request.getCurrency())
                .build();
    }

}
