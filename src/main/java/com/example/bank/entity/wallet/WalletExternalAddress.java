package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CryptoNetwork;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "wallet_external_addresses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_external_addresses_wallet_id",
                        columnNames = "wallet_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletExternalAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Wallet nội bộ của user.
    // Mỗi wallet chỉ có đúng một external address.
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    // Network user đã chọn:
    // TRC20 / BEP20 / ETH
    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false, length = 20)
    private CryptoNetwork network;

    // Địa chỉ blockchain cá nhân của user.
    @Column(name = "address", nullable = false, length = 255)
    private String address;
}