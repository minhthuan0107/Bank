package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.CreateDepositAddressRequest;
import com.example.bank.dto.response.wallet.response.DepositAddressResponse;
import com.example.bank.service.wallet.admin.DepositAddressService;
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
@RequestMapping("${api.prefix}/admin/deposit-addresses")
@RequiredArgsConstructor
public class DepositAddressController {

    private final DepositAddressService service;
    private final LocalizationUtils i18n;

    @PostMapping("/created")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepositAddressResponse>> createDepositAddress(
            @Valid @RequestBody CreateDepositAddressRequest request
    ) {

        DepositAddressResponse response = service.createDepositAddress(request);

        return ResponseEntity.ok(
                ApiResponse.created(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_ADDRESS_CREATED),
                        response
                )
        );
    }
}