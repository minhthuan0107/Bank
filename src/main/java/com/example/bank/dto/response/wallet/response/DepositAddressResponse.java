package com.example.bank.dto.response.wallet.response;

import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepositAddressResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("currency")
    private Stablecoin currency;

    @JsonProperty("network")
    private String network;

    @JsonProperty("address")
    private String address;

    @JsonProperty("display_order")
    private Integer displayOrder;

    @JsonProperty("status")
    private String status;
}