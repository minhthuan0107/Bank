package com.example.bank.dto.response.wallet.admin;

import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class DepositOrderListAdminResponse {

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("user_id")
    private Long userId;

    private String username;

    private Stablecoin currency;

    private String network;

    private BigDecimal amount;

    private BigDecimal fee;

    @JsonProperty("expected_amount")
    private BigDecimal expectedAmount;

    @JsonProperty("deposit_address")
    private String depositAddress;

    private DepositOrderStatus status;

    @JsonProperty("admin_note")
    private String adminNote;

    @JsonProperty("image_urls")
    private List<String> imageUrls;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public static DepositOrderListAdminResponse from(
            DepositOrder order,
            String username,
            List<String> imageUrls
    ) {
        return DepositOrderListAdminResponse.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .username(username)
                .currency(order.getCurrency())
                .network(order.getNetwork())
                .amount(order.getAmount())
                .fee(order.getFee())
                .expectedAmount(order.getExpectedAmount())
                .depositAddress(order.getAddress())
                .status(order.getStatus())
                .adminNote(order.getAdminNote())
                .imageUrls(imageUrls)
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
