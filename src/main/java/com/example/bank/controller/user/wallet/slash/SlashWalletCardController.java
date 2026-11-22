package com.example.bank.controller.user.wallet.slash;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.slash.TopupCardRequest;
import com.example.bank.dto.request.wallet.slash.WithDrawCardRequest;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.service.wallet.slash.CardFundingService;
import com.example.bank.service.wallet.slash.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Legacy Slash card endpoints. Paths stay under /wallet for client compatibility. */
@RestController
@RequestMapping("${api.prefix}/wallet")
@RequiredArgsConstructor
public class SlashWalletCardController {

    private final LocalizationUtils i18n;
    private final CardService cardService;
    private final CardFundingService cardFundingService;

    @PostMapping("/cards/{cardId}/topup")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> topupCard(
            @PathVariable Long cardId,
            @Valid @RequestBody TopupCardRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        cardFundingService.topupCard(
                currentUser.getId(),
                cardId,
                request.getAmount(),
                request.getReferenceId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_TOPUP_SUCCESS),
                        null
                )
        );
    }

    @PostMapping("/cards/{cardId}/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> withDrawCard(
            @PathVariable Long cardId,
            @Valid @RequestBody WithDrawCardRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        cardFundingService.withdrawCard(
                currentUser.getId(),
                cardId,
                request.getAmount(),
                request.getReferenceId()
        );
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_WITHDRAW_SUCCESS),
                        null
                )
        );
    }

    @GetMapping("/cards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardPageResponse>> getCardDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page
    ) {
        CardPageResponse response = cardService.getDashboard(currentUser.getId(), page);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_LIST_SUCCESS),
                        response
                )
        );
    }
}
