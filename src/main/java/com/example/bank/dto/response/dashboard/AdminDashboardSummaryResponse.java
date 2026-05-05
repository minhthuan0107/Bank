package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminDashboardSummaryResponse {

    @JsonProperty("total_users")
    private long totalUsers;

    @JsonProperty("pending_withdraw_count")
    private long pendingWithdrawCount;

    @JsonProperty("pending_withdraw_amount")
    private BigDecimal pendingWithdrawAmount;

    @JsonProperty("pending_deposit_count")
    private long pendingDepositCount;

    @JsonProperty("pending_deposit_amount")
    private BigDecimal pendingDepositAmount;

    /**
     * Tổng số dư tất cả ví user
     */
    @JsonProperty("total_wallet_balance")
    private BigDecimal totalWalletBalance;
}