package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.Stablecoin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "deposit_orders")
@Getter
@Setter
public class DepositOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Stablecoin currency;

    @Column(name = "network", nullable = false, length = 20)
    private String network;

    // Địa chỉ của Bank nhận tiền.
    @Column(name = "destination_address", nullable = false, length = 255)
    private String destinationAddress;

    // Địa chỉ external wallet của user dùng để gửi tiền.
    // Nullable tạm thời vì các DepositOrder cũ chưa có dữ liệu này.
    @Column(name = "source_address", length = 255)
    private String sourceAddress;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DepositOrderStatus status;

    @Column(name = "admin_note")
    private String adminNote;

    public static DepositOrder create(
            Long userId,
            String orderNo,
            Stablecoin currency,
            String network,
            String address,
            String sourceAddress,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal expectedAmount
    ) {
        DepositOrder order = new DepositOrder();

        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setCurrency(currency);
        order.setNetwork(network);
        order.setDestinationAddress(address);
        order.setSourceAddress(sourceAddress);
        order.setAmount(amount);
        order.setFee(fee);
        order.setExpectedAmount(expectedAmount);
        order.setStatus(DepositOrderStatus.PENDING);

        return order;
    }

    public void markSuccess() {
        this.status = DepositOrderStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = DepositOrderStatus.FAILED;
    }
}