package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "deposit_order_images")
@Getter
@Setter
public class DepositOrderImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "deposit_order_id", nullable = false)
    private Long depositOrderId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order")
    private Integer displayOrder;
}
