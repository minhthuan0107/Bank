package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.service.wallet.user.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/card")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final LocalizationUtils i18n;

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
}
