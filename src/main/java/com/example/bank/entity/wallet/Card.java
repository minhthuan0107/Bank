package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * ID từ Slash (mapping webhook)
     */
    @Column(name = "slash_card_id", nullable = false, unique = true)
    private String slashCardId;

    /**
     * Tên hiển thị
     */
    private String name;

    /**
     * virtual / physical
     */
    @Column(nullable = false)
    private String type;

    // ===== MONEY =====

    /**
     * Số tiền allocate vào thẻ
     */
    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedAmount;

    /**
     * Số tiền đã tiêu
     */
    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount;

    /**
     * Số tiền còn lại
     */
    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    @Column(length = 10)
    private String currency;

    // ===== CARD INFO (SAFE) =====
    /**
     * BIN (6 số đầu)
     */
    @Column(length = 10)
    private String bin;

    /**
     * 4 số cuối
     */
    @Column(length = 10)
    private String last4;

    private String brand;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

}
