package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateWalletExternalAddressRequest;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.WalletExternalAddress;
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
        /*
         * Lock Wallet để tránh 2 request đồng thời
         * cùng tạo external address cho một Wallet.
         */
        Wallet wallet = walletRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.WALLET_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        /*
         * Mỗi Wallet chỉ được cấu hình duy nhất
         * một external address.
         */
        if (walletExternalAddressRepository.existsByWalletId(wallet.getId())) {
            throw new WalletException(
                    MessageKeys.WALLET_EXTERNAL_ADDRESS_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        /*
         * Loại bỏ khoảng trắng đầu/cuối.
         *
         * Không lowercase vì address có thể cần giữ
         * nguyên định dạng mà user nhập.
         */
        String address = request.getAddress().trim();

        WalletExternalAddress entity = WalletExternalAddress.builder()
                .walletId(wallet.getId())
                .network(request.getNetwork())
                .address(address)
                .build();

        walletExternalAddressRepository.save(entity);
    }
}