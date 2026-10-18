package com.example.bank.controller.admin.wallet;

import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.UpdateWalletSettingsRequest;
import com.example.bank.dto.response.wallet.admin.WalletSettingsResponse;
import com.example.bank.service.wallet.admin.AdminWalletSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/wallet-settings")
@RequiredArgsConstructor
public class AdminWalletSettingsController {

    private final AdminWalletSettingsService service;
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WalletSettingsResponse>> getSettings() {

        WalletSettingsResponse response = service.getSettings();

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        null,
                        response
                )
        );
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WalletSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateWalletSettingsRequest request
    ) {

        WalletSettingsResponse response = service.updateSettings(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        null,
                        response
                )
        );
    }
}