package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class AdminUserFinancialStatisticsResponse {

    private AdminDepositStatisticsResponse deposit;

    private AdminWithdrawStatisticsResponse withdraw;

    @JsonProperty("card_transaction")
    private AdminCardTransactionStatisticsResponse cardTransaction;
}