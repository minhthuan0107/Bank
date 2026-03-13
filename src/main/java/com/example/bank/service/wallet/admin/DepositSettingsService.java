package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.CreateDepositSettingsRequest;
import com.example.bank.dto.response.wallet.admin.DepositSettingsResponse;

public interface DepositSettingsService {
    DepositSettingsResponse createDepositSettings(CreateDepositSettingsRequest request);


}
