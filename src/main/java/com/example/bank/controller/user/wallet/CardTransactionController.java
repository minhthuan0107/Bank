package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.CardFundingTransactionPageResponse;
import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;
import com.example.bank.service.wallet.user.CardFundingService;
import com.example.bank.service.wallet.user.CardTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/card-transactions")
public class CardTransactionController {

    private final CardTransactionService cardTransactionService;
    private final LocalizationUtils i18n;
    private final CardFundingService cardFundingService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardTransactionPageResponse>> getCardTransactions(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page
    ) {
        CardTransactionPageResponse response =
                cardTransactionService.getUserCardTransactions(
                        currentUser.getId(),
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

    @GetMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardTransactionPageResponse>> getCardTransactions(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(name = "slash_transaction_id", required = false) String slashTransactionId,
            @RequestParam(name = "merchant_description", required = false) String merchantDescription,
            @RequestParam(required = false) CardTransactionStatus status,
            @RequestParam(name = "from_time", required = false) Instant fromTime,
            @RequestParam(name = "to_time", required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page
    ) {
        CardTransactionPageResponse response =
                cardTransactionService.getCardTransactions(
                        currentUser.getId(),
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

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardFundingTransactionPageResponse>> getFundingTransactions(
            @AuthenticationPrincipal UserDetailsImpl currentUser,

            @RequestParam(required = false) String last4,
            @RequestParam(required = false) CardTxnType type,
            @RequestParam(required = false) CardTxnStatus status,

            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,

            @RequestParam(defaultValue = "0") int page
    ) {
        CardFundingTransactionPageResponse response =
               cardFundingService.getUserFundingTransactions(
                        currentUser.getId(),
                        last4,
                        type,
                        status,
                        fromTime,
                        toTime,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        null,
                        response
                )
        );
    }
}