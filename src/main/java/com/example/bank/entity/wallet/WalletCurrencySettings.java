package com.example.bank.entity.wallet;


import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.Stablecoin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_currency_settings")
@Getter
@Setter
public class WalletCurrencySettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10, unique = true)
    private Stablecoin currency;

    /**
     * Phí nạp tiền theo phần trăm.
     * Ví dụ: 0.10 nghĩa là 0.10%
     */
    @Column(name = "deposit_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal depositFeePercent;

    /**
     * Số tiền nạp tối thiểu.
     */
    @Column(name = "min_deposit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minDepositAmount;

    /**
     * Số tiền rút tối thiểu.
     */
    @Column(name = "min_withdraw_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minWithdrawAmount;

    /**
     * Số tiền tối thiểu để nạp vào thẻ / tạo thẻ.
     */
    @Column(name = "min_card_funding_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minCardFundingAmount;

    /**
     * ACTIVE / INACTIVE
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
