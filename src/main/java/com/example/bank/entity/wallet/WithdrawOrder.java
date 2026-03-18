package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "withdraw_orders")
@Getter
@Setter
public class WithdrawOrder extends BaseEntity {

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

    @Column(name = "to_address", nullable = false, length = 255)
    private String toAddress;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WithdrawOrderStatus status;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "tx_hash", length = 120)
    private String txHash;

    public static WithdrawOrder create(
            Long userId,
            String orderNo,
            Stablecoin currency,
            String network,
            String toAddress,
            BigDecimal amount
    ) {
        WithdrawOrder order = new WithdrawOrder();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setCurrency(currency);
        order.setNetwork(network);
        order.setToAddress(toAddress);
        order.setAmount(amount);
        order.setStatus(WithdrawOrderStatus.PENDING_OTP);
        return order;
    }

    public void markPendingAdmin() {
        this.status = WithdrawOrderStatus.PENDING_ADMIN;
    }

    public void markSuccess(String txHash) {
        this.status = WithdrawOrderStatus.SUCCESS;
        this.txHash = txHash;
    }

    public void markFailed(String adminNote) {
        this.status = WithdrawOrderStatus.FAILED;
        this.adminNote = adminNote;
    }

    public void markCancelled(String adminNote) {
        this.status = WithdrawOrderStatus.CANCELLED;
        this.adminNote = adminNote;
    }

    public void markExpired() {
        this.status = WithdrawOrderStatus.EXPIRED;
    }
}