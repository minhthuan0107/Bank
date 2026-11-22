package com.example.bank.controller.admin.wallet.infini;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.infini.AdminInfiniCardPageResponse;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.service.wallet.infini.AdminInfiniCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/** Admin management of Infini cards. Separate from /admin/cards (Slash) so card IDs are never mixed up. */
@RestController
@RequestMapping("${api.prefix}/admin/infini-cards")
@RequiredArgsConstructor
public class AdminInfiniCardController {

    private final AdminInfiniCardService adminCardService;
    private final LocalizationUtils i18n;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminInfiniCardPageResponse>> getCards(
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_LIST_SUCCESS),
                        adminCardService.getCards(cardNumber, cardName, username, status, fromTime, toTime, page)
                )
        );
    }

    @PostMapping("/{cardId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockCard(
            @AuthenticationPrincipal UserDetailsImpl currentAdmin,
            @PathVariable Long cardId
    ) {
        adminCardService.lockCard(currentAdmin.getId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), i18n.getLocalizedMessage(MessageKeys.CARD_LOCKED_SUCCESS), null));
    }

    @PostMapping("/{cardId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockCard(
            @AuthenticationPrincipal UserDetailsImpl currentAdmin,
            @PathVariable Long cardId
    ) {
        adminCardService.unlockCard(currentAdmin.getId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), i18n.getLocalizedMessage(MessageKeys.CARD_UNLOCKED_SUCCESS), null));
    }
}
