package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.request.wallet.user.TopupCardRequest;
import com.example.bank.dto.request.wallet.user.WithDrawCardRequest;
import com.example.bank.dto.response.wallet.user.BalanceResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.service.wallet.user.CardFundingService;
import com.example.bank.service.wallet.user.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("${api.prefix}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final LocalizationUtils i18n;
    private final CardService cardService;
    private final CardFundingService cardFundingService;


    @PostMapping("/create-card")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> createCard(
            @Valid @RequestBody CreateCardRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {

        cardService.createCard(request, currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_CREATED),
                        null
                )
        );
    }

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
    public ResponseEntity<ApiResponse<CardPageResponse>> getCards(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page
    ) {
        CardPageResponse response =
                cardService.getUserCards(
                        currentUser.getId(),
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

    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {

        BalanceResponse response =
                cardService.getUserBalance(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.BALANCE_FETCH_SUCCESS),
                        response
                )
        );
    }


}
