package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.CreateDepositSettingsRequest;
import com.example.bank.dto.response.wallet.response.DepositSettingsResponse;
import com.example.bank.entity.wallet.DepositSettings;

import java.util.Optional;

public interface DepositSettingsService {
    DepositSettingsResponse createDepositSettings(CreateDepositSettingsRequest request);


}
