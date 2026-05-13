package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.CardDashboardResponse;
import com.example.bank.dto.response.wallet.user.CardFundingTransactionPageResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.dto.response.wallet.user.CardSensitiveDetailResponse;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;
import com.example.bank.service.wallet.user.CardSensitiveDetailService;
import com.example.bank.service.wallet.user.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("${api.prefix}/card")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final LocalizationUtils i18n;
    private final CardSensitiveDetailService cardSensitiveDetailService;

    @PostMapping("/{cardId}/lock")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> lockCard(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {

        cardService.lockCard(currentUser.getId(), cardId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_LOCK_SUCCESS),
                        null
                )
        );
    }

    @PostMapping("/{cardId}/unlock")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> unlockCard(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {

        cardService.unlockCard(currentUser.getId(), cardId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_UNLOCK_SUCCESS),
                        null
                )
        );
    }


    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardPageResponse>> getCards(
            @AuthenticationPrincipal UserDetailsImpl currentUser,

            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) CardStatus status,

            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,

            @RequestParam(defaultValue = "0") int page
    ) {
        CardPageResponse response =
                cardService.getUserCards(
                        currentUser.getId(),
                        cardNumber,
                        cardName,
                        status,
                        fromTime,
                        toTime,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardDashboardResponse>> getCardDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        CardDashboardResponse response =
                cardService.getCardDashboard(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_DASHBOARD_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/{cardId}/sensitive-details")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<CardSensitiveDetailResponse> getSensitiveDetail(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {
        CardSensitiveDetailResponse data =
                cardSensitiveDetailService.getSensitiveDetail(
                        currentUser.getId(),
                        cardId
                );

        return ApiResponse.success(
                HttpStatus.OK.value(),
                MessageKeys.CARD_SENSITIVE_DETAIL_SUCCESS,
                data
        );
    }


}
