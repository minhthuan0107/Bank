package com.example.bank.entity.wallet;

import com.example.bank.enums.wallet.CardBinStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.Instant;

@Entity
@Table(name = "card_bins")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * BIN (prefix)
     */
    @Column(name = "bin", nullable = false, length = 10, unique = true)
    private String bin;

    /**
     * mapping tới Slash
     */
    @Column(name = "card_product_id", nullable = false, length = 100)
    private String cardProductId;

    /**
     * active / inactive
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardBinStatus status;

    @CurrentTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
}
