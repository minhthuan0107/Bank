package com.example.bank.dto.response.wallet.user;

import com.example.bank.enums.dashboard.DashboardPeriod;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record UserCashFlowResponse(

        @JsonProperty("period")
        DashboardPeriod period,

        @JsonProperty("items")
        List<UserCashFlowPointResponse> items
) {
}
