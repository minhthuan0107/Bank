package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.InfiniCardPageResponse;
import com.example.bank.dto.response.wallet.user.InfiniCardResponse;
import com.example.bank.service.wallet.user.InfiniCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/infini-cards")
@RequiredArgsConstructor
public class InfiniCardController {

    private final InfiniCardService infiniCardService;
    private final LocalizationUtils i18n;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InfiniCardPageResponse>> getCards(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page
    ) {
        InfiniCardPageResponse response = infiniCardService.getCards(currentUser.getId(), page);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InfiniCardResponse>> getCard(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {
        InfiniCardResponse response = infiniCardService.getCard(currentUser.getId(), cardId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_LIST_SUCCESS),
                        response
                )
        );
    }
}
