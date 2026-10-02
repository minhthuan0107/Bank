package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateWalletExternalAddressRequest;

public interface WalletExternalAddressService {

    boolean isConfigured(Long userId);

    void createAddress(
            Long userId,
            CreateWalletExternalAddressRequest request
    );
}
