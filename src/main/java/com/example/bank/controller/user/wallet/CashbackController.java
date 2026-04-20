package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.CashbackDashboardResponse;
import com.example.bank.service.wallet.user.CashbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/cashback")
@RequiredArgsConstructor
public class CashbackController {
    private final CashbackService cashbackService;
    private final LocalizationUtils i18n;


    /**
     * Dashboard cashback cho user
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CashbackDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        CashbackDashboardResponse data =
                cashbackService.getDashboard(currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_DASHBOARD_SUCCESS),
                        data
                )
        );
    }
}

