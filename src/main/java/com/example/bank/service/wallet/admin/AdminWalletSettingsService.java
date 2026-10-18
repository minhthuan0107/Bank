package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.UpdateWalletSettingsRequest;
import com.example.bank.dto.response.wallet.admin.WalletSettingsResponse;

public interface AdminWalletSettingsService {

    WalletSettingsResponse getSettings();

    WalletSettingsResponse updateSettings(
            UpdateWalletSettingsRequest request
    );
}
