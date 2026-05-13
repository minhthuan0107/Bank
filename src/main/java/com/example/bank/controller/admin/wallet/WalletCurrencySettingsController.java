package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.UpdateWalletCurrencySettingsRequest;
import com.example.bank.dto.response.wallet.admin.WalletCurrencySettingsResponse;
import com.example.bank.service.wallet.admin.WalletCurrencySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/wallet-currency-settings")
@RequiredArgsConstructor
public class WalletCurrencySettingsController {

    private final WalletCurrencySettingsService service;
    private final LocalizationUtils i18n;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WalletCurrencySettingsResponse>>> getAllSettings() {
        List<WalletCurrencySettingsResponse> response = service.getAllSettings();

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WALLET_CURRENCY_SETTINGS_LIST_SUCCESS),
                        response
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WalletCurrencySettingsResponse>> updateSettings(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWalletCurrencySettingsRequest request
    ) {
        WalletCurrencySettingsResponse response = service.updateSettings(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WALLET_CURRENCY_SETTINGS_UPDATED),
                        response
                )
        );
    }
}