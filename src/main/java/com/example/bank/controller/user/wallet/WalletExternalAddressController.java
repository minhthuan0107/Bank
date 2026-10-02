package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.user.CreateWalletExternalAddressRequest;
import com.example.bank.service.wallet.user.WalletExternalAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/wallet/external-address")
@RequiredArgsConstructor
public class WalletExternalAddressController {

    private final WalletExternalAddressService service;
    private final LocalizationUtils i18n;

    @GetMapping("/configured")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> isConfigured(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        boolean configured =
                service.isConfigured(
                        currentUser.getId()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        null,
                        configured
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> createAddress(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody CreateWalletExternalAddressRequest request
    ) {
        service.createAddress(
                currentUser.getId(),
                request
        );

        return ResponseEntity.status(
                HttpStatus.CREATED
        ).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        null,
                        null
                )
        );
    }
}