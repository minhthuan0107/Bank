package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.admin.AdminCardPageResponse;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.service.wallet.admin.CardAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/cards")
public class CardAdminController {

    private final CardAdminService cardService;
    private final LocalizationUtils i18n;

    /**
     * Admin xem/tìm kiếm danh sách thẻ của tất cả user
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminCardPageResponse>> getAdminCards(
            @RequestParam(required = false) String cardNumber,
            @RequestParam(required = false) String cardName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,

            @RequestParam(defaultValue = "0") int page
    ) {
        AdminCardPageResponse response =
                cardService.getAdminCards(
                        cardNumber,
                        cardName,
                        username,
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
}