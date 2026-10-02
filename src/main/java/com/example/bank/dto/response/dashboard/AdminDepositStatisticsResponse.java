package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
@Getter
@Builder
public class AdminDepositStatisticsResponse {

    // Số lần nạp tiền thành công
    @JsonProperty("success_count")
    private long successCount;

    // Tổng số tiền của các giao dịch nạp thành công
    @JsonProperty("success_amount")
    private BigDecimal successAmount;

    // Số lần nạp tiền đang chờ xử lý
    @JsonProperty("pending_count")
    private long pendingCount;

    // Tổng số tiền của các giao dịch nạp đang chờ xử lý
    @JsonProperty("pending_amount")
    private BigDecimal pendingAmount;

    // Số lần nạp tiền thất bại
    @JsonProperty("failed_count")
    private long failedCount;

    // Tổng số tiền của các giao dịch nạp thất bại
    @JsonProperty("failed_amount")
    private BigDecimal failedAmount;

    // Tỷ lệ nạp tiền thành công (%)
    // Công thức: SUCCESS / (SUCCESS + FAILED) * 100
    // Không tính PENDING vì giao dịch chưa có kết quả cuối cùng
    @JsonProperty("success_rate")
    private BigDecimal successRate;
}