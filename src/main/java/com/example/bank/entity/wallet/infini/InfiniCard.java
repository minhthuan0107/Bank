package com.example.bank.entity.wallet.infini;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CardLockedBy;
import com.example.bank.enums.wallet.CardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cards_infini")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfiniCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "infini_card_id", nullable = false, unique = true, length = 100)
    private String infiniCardId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "token_type", nullable = false, length = 10)
    private String tokenType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(name = "holder_email", nullable = false, length = 255)
    private String holderEmail;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "initial_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialAmount;

    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount;

    @Column(name = "locked_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedAmount;

    @Column(name = "creation_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal creationFee;

    @Column(name = "last4", length = 4)
    private String last4;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    // Set only while the card is BLOCKED.
    @Enumerated(EnumType.STRING)
    @Column(name = "locked_by", length = 10)
    private CardLockedBy lockedBy;
}
