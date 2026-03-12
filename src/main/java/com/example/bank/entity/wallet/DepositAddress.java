package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.Stablecoin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "deposit_addresses")
@Getter
@Setter
public class DepositAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Stablecoin currency;

    @Column(name = "network", nullable = false, length = 20)
    private String network;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}