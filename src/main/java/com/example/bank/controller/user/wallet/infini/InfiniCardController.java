package com.example.bank.controller.user.wallet.infini;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.dto.response.wallet.infini.InfiniCardPageResponse;
import com.example.bank.dto.response.wallet.infini.InfiniFundingResponse;
import com.example.bank.service.wallet.infini.InfiniFundingService;
import com.example.bank.service.wallet.infini.InfiniCardSensitiveDetailService;
import com.example.bank.service.wallet.infini.InfiniCardLockService;
import com.example.bank.dto.response.wallet.user.CardSensitiveDetailResponse;
import org.springframework.http.CacheControl;
import jakarta.validation.Valid;
import java.util.UUID;
import com.example.bank.dto.response.wallet.infini.InfiniCardResponse;
import com.example.bank.service.wallet.infini.InfiniCardService;
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
    private final InfiniFundingService infiniFundingService;
    private final InfiniCardSensitiveDetailService sensitiveDetailService;
    private final InfiniCardLockService lockService;
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

    // Full PAN/CVV: must never be cached by browsers or proxies.
    @GetMapping("/{cardId}/sensitive-details")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CardSensitiveDetailResponse>> getSensitiveDetail(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {
        CardSensitiveDetailResponse response =
                sensitiveDetailService.getSensitiveDetail(currentUser.getId(), cardId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.INFINI_REVEAL_SUCCESS),
                        response
                ));
    }

    @PostMapping("/{cardId}/lock")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> lockCard(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {
        lockService.lockCard(currentUser.getId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), i18n.getLocalizedMessage(MessageKeys.CARD_LOCK_SUCCESS), null));
    }

    @PostMapping("/{cardId}/unlock")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> unlockCard(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId
    ) {
        lockService.unlockCard(currentUser.getId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), i18n.getLocalizedMessage(MessageKeys.CARD_UNLOCK_SUCCESS), null));
    }

    @PostMapping("/{cardId}/topup")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InfiniFundingResponse>> topUp(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId,
            @Valid @RequestBody InfiniFundingRequest request
    ) {
        return fundingResponse(infiniFundingService.topUp(currentUser.getId(), cardId, request),
                MessageKeys.INFINI_FUNDING_TOPUP_SUCCESS);
    }

    @PostMapping("/{cardId}/withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InfiniFundingResponse>> withdraw(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long cardId,
            @Valid @RequestBody InfiniFundingRequest request
    ) {
        return fundingResponse(infiniFundingService.withdraw(currentUser.getId(), cardId, request),
                MessageKeys.INFINI_FUNDING_WITHDRAW_SUCCESS);
    }

    @GetMapping("/funding/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InfiniFundingResponse>> getFundingStatus(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.INFINI_FUNDING_STATUS_SUCCESS),
                        infiniFundingService.getStatus(requestId, currentUser.getId())
                )
        );
    }

    // PENDING / REVIEW_REQUIRED -> 202; SUCCEEDED / FAILED -> 200. Clients must read data.status.
    private ResponseEntity<ApiResponse<InfiniFundingResponse>> fundingResponse(
            InfiniFundingResponse response, String successKey) {
        boolean unresolved = "PENDING".equals(response.getStatus()) ||
                "REVIEW_REQUIRED".equals(response.getStatus());
        HttpStatus status = unresolved ? HttpStatus.ACCEPTED : HttpStatus.OK;
        String messageKey = "SUCCEEDED".equals(response.getStatus()) ? successKey
                : ("FAILED".equals(response.getStatus()) ? MessageKeys.INFINI_FUNDING_FAILED
                : MessageKeys.INFINI_FUNDING_PENDING);
        return ResponseEntity.status(status).body(
                ApiResponse.success(status.value(), i18n.getLocalizedMessage(messageKey), response));
    }
}
