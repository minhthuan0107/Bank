package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;

import com.example.bank.dto.request.wallet.admin.UpdateWalletSettingsRequest;
import com.example.bank.dto.response.wallet.admin.WalletSettingsResponse;
import com.example.bank.entity.wallet.WalletSettings;
import com.example.bank.repository.wallet.WalletSettingsRepository;
import com.example.bank.service.wallet.admin.AdminWalletSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminWalletSettingsServiceImpl implements AdminWalletSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final WalletSettingsRepository repository;

    @Override
    @Transactional(readOnly = true)
    public WalletSettingsResponse getSettings() {

        WalletSettings settings = repository.findById(SETTINGS_ID)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_SETTINGS_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        return toResponse(settings);
    }

    @Override
    @Transactional
    public WalletSettingsResponse updateSettings(
            UpdateWalletSettingsRequest request
    ) {

        WalletSettings settings = repository.findByIdForUpdate(SETTINGS_ID)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_SETTINGS_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        settings.setDepositFeePercent(request.getDepositFeePercent());
        settings.setMinDepositAmount(request.getMinDepositAmount());
        settings.setMinWithdrawAmount(request.getMinWithdrawAmount());
        settings.setMinCardFundingAmount(request.getMinCardFundingAmount());

        return toResponse(settings);
    }

    private WalletSettingsResponse toResponse(WalletSettings settings) {
        return WalletSettingsResponse.builder()
                .id(settings.getId())
                .depositFeePercent(settings.getDepositFeePercent())
                .minDepositAmount(settings.getMinDepositAmount())
                .minWithdrawAmount(settings.getMinWithdrawAmount())
                .minCardFundingAmount(settings.getMinCardFundingAmount())
                .status(settings.getStatus())
                .build();
    }
}