package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;

import com.example.bank.dto.request.wallet.admin.UpdateWalletCurrencySettingsRequest;
import com.example.bank.dto.response.wallet.admin.WalletCurrencySettingsResponse;
import com.example.bank.entity.wallet.WalletCurrencySettings;
import com.example.bank.repository.wallet.WalletCurrencySettingsRepository;
import com.example.bank.service.wallet.admin.WalletCurrencySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletCurrencySettingsServiceImpl implements WalletCurrencySettingsService {

    private final WalletCurrencySettingsRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<WalletCurrencySettingsResponse> getAllSettings() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WalletCurrencySettingsResponse updateSettings(
            Long id,
            UpdateWalletCurrencySettingsRequest request
    ) {
        WalletCurrencySettings settings = repository.findById(id)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_CURRENCY_SETTINGS_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (repository.existsByCurrencyAndIdNot(request.getCurrency(), id)) {
            throw new WalletException(
                    MessageKeys.WALLET_CURRENCY_SETTINGS_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        settings.setDepositFeePercent(request.getDepositFeePercent());
        settings.setMinDepositAmount(request.getMinDepositAmount());
        settings.setMinWithdrawAmount(request.getMinWithdrawAmount());
        settings.setMinCardFundingAmount(request.getMinCardFundingAmount());
        WalletCurrencySettings saved = repository.save(settings);
        return toResponse(saved);
    }

    private WalletCurrencySettingsResponse toResponse(WalletCurrencySettings settings) {
        return WalletCurrencySettingsResponse.builder()
                .id(settings.getId())
                .currency(settings.getCurrency())
                .depositFeePercent(settings.getDepositFeePercent())
                .minDepositAmount(settings.getMinDepositAmount())
                .minWithdrawAmount(settings.getMinWithdrawAmount())
                .minCardFundingAmount(settings.getMinCardFundingAmount())
                .status(settings.getStatus())
                .build();
    }
}