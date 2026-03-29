package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.service.wallet.user.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("${api.prefix}/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final LocalizationUtils i18n;
    private final CardService cardService;


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
}
