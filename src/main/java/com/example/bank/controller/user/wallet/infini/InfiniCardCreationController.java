package com.example.bank.controller.user.wallet.infini;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.infini.CreateCardRequest;
import com.example.bank.dto.response.wallet.infini.CardCreationConfigResponse;
import com.example.bank.dto.response.wallet.infini.CardCreationResponse;
import com.example.bank.service.wallet.infini.CardCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Infini card creation. Paths stay under /wallet for client compatibility. */
@RestController
@RequestMapping("${api.prefix}/wallet")
@RequiredArgsConstructor
public class InfiniCardCreationController {

    private final LocalizationUtils i18n;
    private final CardCreationService cardCreationService;

    @PostMapping("/create-card")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardCreationResponse>> createCard(
            @Valid @RequestBody CreateCardRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        CardCreationResponse response = cardCreationService.createCard(request, currentUser.getId());

        // PENDING / REVIEW_REQUIRED -> 202; SUCCEEDED / FAILED -> 200. Clients must read data.status.
        HttpStatus status = ("PENDING".equals(response.getStatus()) ||
                "REVIEW_REQUIRED".equals(response.getStatus()))
                ? HttpStatus.ACCEPTED : HttpStatus.OK;

        String messageKey = "SUCCEEDED".equals(response.getStatus())
                ? MessageKeys.CARD_CREATED
                : ("FAILED".equals(response.getStatus())
                    ? MessageKeys.CARD_CREATION_FAILED
                    : MessageKeys.CARD_CREATION_PENDING);

        return ResponseEntity.status(status).body(
                ApiResponse.success(status.value(), i18n.getLocalizedMessage(messageKey), response));
    }

    @GetMapping("/card-creation-config")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardCreationConfigResponse>> getCreationConfig(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_CREATION_CONFIG_SUCCESS),
                        cardCreationService.getCreationConfig(currentUser.getId())
                )
        );
    }

    @GetMapping("/card-creation/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardCreationResponse>> getCreationStatus(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_CREATION_STATUS_SUCCESS),
                        cardCreationService.getCreationStatus(requestId, currentUser.getId())
                )
        );
    }
}
