package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.auth.UpdateWalletCurrencySettingsRequest;
import com.example.bank.dto.response.wallet.admin.WalletCurrencySettingsResponse;

import java.util.List;

public interface WalletCurrencySettingsService {

    List<WalletCurrencySettingsResponse> getAllSettings();

    WalletCurrencySettingsResponse updateSettings(Long id, UpdateWalletCurrencySettingsRequest request);
}
