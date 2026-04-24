package com.example.bank.dto.response.cashback.user;

import com.example.bank.enums.wallet.CashbackStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CashbackHistoryResponse {

    @JsonProperty("user_id")
    private Long userId;

    /**
     * Tháng cashback dạng yyyy-MM
     */
    private String month;

    /**
     * Tổng chi tiêu trong tháng
     */
    @JsonProperty("total_spent")
    private BigDecimal totalSpent;

    /**
     * Số tiền cashback
     */
    @JsonProperty("cashback_amount")
    private BigDecimal cashbackAmount;

    /**
     * Phần trăm cashback được áp dụng
     */
    private BigDecimal percent;

    /**
     * Trạng thái cashback
     */
    @JsonProperty("cashback_status")
    private CashbackStatus cashbackStatus;

    @JsonProperty("approved_at")
    private Instant approvedAt;


}