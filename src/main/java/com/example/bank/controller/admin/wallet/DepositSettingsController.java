package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.CreateDepositSettingsRequest;
import com.example.bank.dto.response.wallet.DepositSettingsResponse;
import com.example.bank.service.wallet.DepositSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/admin/deposit-settings")
@RequiredArgsConstructor
public class DepositSettingsController {

    private final DepositSettingsService service;
    private final LocalizationUtils i18n;

    @PostMapping("/created")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepositSettingsResponse>> createDepositSettings(
            @Valid @RequestBody CreateDepositSettingsRequest request
    ) {
        DepositSettingsResponse response = service.createDepositSettings(request);

        return ResponseEntity.ok(
                ApiResponse.created(
                        HttpStatus.CREATED.value(),
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_SETTINGS_CREATED),
                        response
                )
        );
    }
}