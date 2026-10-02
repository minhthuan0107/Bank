package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
@Getter
@Builder
public class AdminCardTransactionStatisticsResponse {

    // Số lần giao dịch thẻ được xem là thành công theo nghiệp vụ hệ thống
    // Bao gồm: POSTED + PENDING
    // PENDING vẫn được tính thành công vì tiền đã bị trừ
    @JsonProperty("success_count")
    private long successCount;

    // Tổng số tiền của các giao dịch thẻ được xem là thành công
    // Bao gồm tổng tiền của POSTED + PENDING
    @JsonProperty("success_amount")
    private BigDecimal successAmount;

    // Số lần giao dịch thẻ đã hoàn tất và được Slash xác nhận POSTED
    @JsonProperty("posted_count")
    private long postedCount;

    // Tổng số tiền của các giao dịch thẻ ở trạng thái POSTED
    @JsonProperty("posted_amount")
    private BigDecimal postedAmount;

    // Số lần giao dịch thẻ đang ở trạng thái PENDING
    // PENDING đã bị trừ tiền nên vẫn được tính vào success_count
    @JsonProperty("pending_count")
    private long pendingCount;

    // Tổng số tiền của các giao dịch thẻ đang ở trạng thái PENDING
    // Số tiền này đã bị trừ nên vẫn được tính vào success_amount
    @JsonProperty("pending_amount")
    private BigDecimal pendingAmount;

    // Số lần giao dịch thẻ thất bại
    @JsonProperty("failed_count")
    private long failedCount;

    // Tổng số tiền của các giao dịch thẻ thất bại
    @JsonProperty("failed_amount")
    private BigDecimal failedAmount;

    // Số lần giao dịch thẻ bị hoàn tiền
    // Tương ứng với trạng thái REVERSED
    @JsonProperty("reversed_count")
    private long reversedCount;

    // Tổng số tiền đã được hoàn lại từ các giao dịch REVERSED
    @JsonProperty("reversed_amount")
    private BigDecimal reversedAmount;

    // Tỷ lệ giao dịch thẻ thành công (%)
    // Công thức:
    // (POSTED + PENDING) / (POSTED + PENDING + FAILED + REVERSED) * 100
    @JsonProperty("success_rate")
    private BigDecimal successRate;

    // Tỷ lệ giao dịch bị hoàn tiền (%)
    // Công thức:
    // REVERSED / (POSTED + PENDING + REVERSED) * 100
    // Không tính FAILED vì giao dịch thất bại không phải giao dịch đã chi tiêu thành công
    @JsonProperty("refund_rate")
    private BigDecimal refundRate;
}
