package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateWalletExternalAddressRequest;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.WalletExternalAddress;
import com.example.bank.enums.wallet.CryptoNetwork;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.WalletExternalAddressRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.user.WalletExternalAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletExternalAddressServiceImpl
        implements WalletExternalAddressService {

    private final WalletRepository walletRepository;
    private final WalletExternalAddressRepository walletExternalAddressRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isConfigured(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.WALLET_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        return walletExternalAddressRepository.existsByWalletId(
                wallet.getId()
        );
    }

    @Override
    @Transactional
    public void createAddress(
            Long userId,
            CreateWalletExternalAddressRequest request
    ) {
        // ===== LOCK WALLET =====
        // Tránh hai request của cùng user cùng tạo external address.
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== CHECK ALREADY CONFIGURED =====
        // Mỗi Wallet chỉ được có duy nhất một external address.
        // Dù đổi currency hoặc network cũng không được tạo thêm.
        if (walletExternalAddressRepository.existsByWalletId(wallet.getId())) {
            throw new WalletException(
                    MessageKeys.WALLET_EXTERNAL_ADDRESS_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        // ===== VALIDATE CURRENCY + NETWORK =====
        Stablecoin currency = request.getCurrency();
        CryptoNetwork network = request.getNetwork();

        boolean validNetwork =
                (currency == Stablecoin.USDT &&
                        (network == CryptoNetwork.TRC20 || network == CryptoNetwork.BEP20))
                        ||
                        (currency == Stablecoin.USDC && network == CryptoNetwork.BASE);

        if (!validNetwork) {
            throw new WalletException(
                    MessageKeys.WALLET_EXTERNAL_ADDRESS_INVALID_NETWORK,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== NORMALIZE ADDRESS =====
        // Chỉ trim, không thay đổi chữ hoa/chữ thường.
        String address = request.getAddress().trim();

        // ===== CREATE EXTERNAL ADDRESS =====
        WalletExternalAddress entity = WalletExternalAddress.builder()
                .walletId(wallet.getId())
                .currency(currency)
                .network(network)
                .address(address)
                .build();

        walletExternalAddressRepository.save(entity);
    }
}