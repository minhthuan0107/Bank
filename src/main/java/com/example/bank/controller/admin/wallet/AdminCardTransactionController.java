package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.service.wallet.admin.CardTransactionAdminService;
import com.example.bank.service.wallet.user.CardTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/card-transactions")
public class AdminCardTransactionController {
    private final CardTransactionAdminService cardTransactionService;
    private final LocalizationUtils i18n;

    /**
     * Admin xem danh sách giao dịch thẻ của từng user
     */
    @GetMapping("/users/{userId}/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CardTransactionPageResponse>> getUserCardTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        CardTransactionPageResponse response =
                cardTransactionService.getUserCardTransactions(
                        userId,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_TRANSACTION_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/users/{userId}/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CardTransactionPageResponse>> getUserCardTransactions(
            @PathVariable Long userId,
            @RequestParam(name = "slash_transaction_id", required = false) String slashTransactionId,
            @RequestParam(name = "merchant_description", required = false) String merchantDescription,
            @RequestParam(required = false) CardTransactionStatus status,
            @RequestParam(name = "from_time", required = false) Instant fromTime,
            @RequestParam(name = "to_time", required = false) Instant toTime,

            @RequestParam(defaultValue = "0") int page
    ) {
        CardTransactionPageResponse response =
                cardTransactionService.getAdminUserCardTransactions(
                        userId,
                        slashTransactionId,
                        merchantDescription,
                        status,
                        fromTime,
                        toTime,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_TRANSACTION_LIST_SUCCESS),
                        response
                )
        );
    }
}
