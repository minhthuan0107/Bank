package com.example.bank.controller.user.wallet;


import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.BalanceResponse;
import com.example.bank.dto.response.wallet.user.DashboardResponse;
import com.example.bank.service.wallet.user.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Wallet endpoints shared by all card providers.
 * Card creation lives in InfiniCardCreationController; legacy Slash card endpoints in SlashWalletCardController.
 */
@RestController
@RequestMapping("${api.prefix}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final LocalizationUtils i18n;
    private final WalletService walletService;

    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {

        BalanceResponse response =
                walletService.getUserBalance(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.BALANCE_FETCH_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        DashboardResponse response =
                walletService.getDashboard(currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.DASHBOARD_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/qualified")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> isWalletQualified(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        boolean qualified =
                walletService.isWalletQualified(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        null,
                        qualified
                )
        );
    }
}
