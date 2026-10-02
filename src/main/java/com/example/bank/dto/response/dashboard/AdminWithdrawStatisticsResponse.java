package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminWithdrawStatisticsResponse {

    // Số lần rút tiền thành công
    @JsonProperty("success_count")
    private long successCount;

    // Tổng số tiền của các giao dịch rút thành công
    @JsonProperty("success_amount")
    private BigDecimal successAmount;

    // Số lần rút tiền đang chờ xử lý
    // Bao gồm: PENDING_OTP + PENDING_ADMIN
    @JsonProperty("pending_count")
    private long pendingCount;

    // Tổng số tiền của các giao dịch rút đang chờ xử lý
    // Bao gồm: PENDING_OTP + PENDING_ADMIN
    @JsonProperty("pending_amount")
    private BigDecimal pendingAmount;

    // Số lần rút tiền thất bại
    // Bao gồm: FAILED + EXPIRED
    @JsonProperty("failed_count")
    private long failedCount;

    // Tổng số tiền của các giao dịch rút thất bại
    // Bao gồm: FAILED + EXPIRED
    @JsonProperty("failed_amount")
    private BigDecimal failedAmount;

    // Tỷ lệ rút tiền thành công (%)
    // Công thức: SUCCESS / (SUCCESS + FAILED + EXPIRED) * 100
    // Không tính PENDING_OTP và PENDING_ADMIN vì giao dịch chưa có kết quả cuối cùng
    @JsonProperty("success_rate")
    private BigDecimal successRate;
}