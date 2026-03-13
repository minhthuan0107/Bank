package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.admin.CreateDepositSettingsRequest;
import com.example.bank.dto.response.wallet.admin.DepositSettingsResponse;
import com.example.bank.entity.wallet.DepositSettings;
import com.example.bank.repository.wallet.DepositSettingsRepository;
import com.example.bank.service.wallet.admin.DepositSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepositSettingsServiceImpl implements DepositSettingsService {

    private final DepositSettingsRepository repository;

    @Override
    public DepositSettingsResponse createDepositSettings(CreateDepositSettingsRequest request) {

        // check duplicate currency
        repository.findByCurrency(request.getCurrency())
                .ifPresent(existing -> {
                    throw new WalletException(
                            MessageKeys.DEPOSIT_SETTINGS_ALREADY_EXISTS,
                            HttpStatus.CONFLICT
                    );
                });

        DepositSettings settings = new DepositSettings();

        settings.setCurrency(request.getCurrency());
        settings.setFeePercent(request.getFeePercent());
        settings.setMinAmount(request.getMinAmount());
        settings.setMaxAmount(request.getMaxAmount());
        settings.setStatus("ACTIVE");

        DepositSettings saved = repository.save(settings);

        return DepositSettingsResponse.builder()
                .id(saved.getId())
                .currency(saved.getCurrency())
                .feePercent(saved.getFeePercent())
                .minAmount(saved.getMinAmount())
                .maxAmount(saved.getMaxAmount())
                .status(saved.getStatus())
                .build();
    }
}