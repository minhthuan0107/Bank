package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;
import com.example.bank.service.wallet.user.CardTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/card-transactions")
public class AdminCardTransactionController {

    private final CardTransactionService cardTransactionService;
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
}
