package com.example.bank.service.wallet;

import com.example.bank.dto.request.wallet.CreateDepositSettingsRequest;
import com.example.bank.dto.response.wallet.DepositSettingsResponse;

public interface DepositSettingsService {
    DepositSettingsResponse createDepositSettings(CreateDepositSettingsRequest request);
}
