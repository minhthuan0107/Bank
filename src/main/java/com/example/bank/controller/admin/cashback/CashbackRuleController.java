package com.example.bank.controller.admin.cashback;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.cashback.admin.ApproveCashbackBatchRequest;
import com.example.bank.dto.request.cashback.admin.CreateCashbackRuleRequest;
import com.example.bank.dto.request.cashback.admin.UpdateCashbackRuleRequest;
import com.example.bank.dto.response.cashback.admin.CashbackRefundPageResponse;
import com.example.bank.dto.response.cashback.admin.CashbackRuleResponse;
import com.example.bank.dto.response.cashback.user.CashbackHistoryPageResponse;
import com.example.bank.service.cashback.admin.CashbackRuleService;
import com.example.bank.service.cashback.user.CashbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/cashback-rules")
@RequiredArgsConstructor
public class CashbackRuleController {

    private final CashbackRuleService service;
    private final LocalizationUtils i18n;
    private final CashbackService cashbackService;

    @PostMapping("create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CashbackRuleResponse>> createCashbackRule(
            @Valid @RequestBody CreateCashbackRuleRequest request
    ) {
        CashbackRuleResponse data = service.createCashbackRule(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_RULE_CREATED),
                        data
                )
        );
    }

    @PatchMapping("update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CashbackRuleResponse>> updateCashbackRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCashbackRuleRequest request
    ) {
        CashbackRuleResponse data = service.updateCashbackRule(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_RULE_UPDATED),
                        data
                )
        );
    }

    @DeleteMapping("delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCashbackRule(
            @PathVariable Long id
    ) {
        service.deleteCashbackRule(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_RULE_DELETED),
                        null
                )
        );
    }

    @GetMapping("list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CashbackRuleResponse>>> getCashbackRules() {
        List<CashbackRuleResponse> data = service.getCashbackRules();
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_RULE_LIST_SUCCESS),
                        data
                )
        );
    }

    @PostMapping("cashback/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveCashbackBatch(
            @RequestParam @NotBlank(message = "{" + MessageKeys.INVALID_MONTH + "}") String month,
            @Valid @RequestBody ApproveCashbackBatchRequest request
    ) {
        service.approveCashbackBatch(request.userIds(),month);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_BATCH_APPROVED),
                        null
                )
        );
    }

    @GetMapping("cashback/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CashbackRefundPageResponse>> getPendingCashback(
            @RequestParam String month,
            @RequestParam(defaultValue = "0") int page
    ) {

        CashbackRefundPageResponse data =
                service.getPendingCashback(month, page);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_REFUND_LIST_SUCCESS),
                        data
                )
        );
    }

    /**
     * Admin xem lịch sử cashback của user bất kỳ
     */
    @GetMapping("history/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CashbackHistoryPageResponse>> getUserCashbackHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        CashbackHistoryPageResponse data =
                cashbackService.getUserCashbackHistory(
                        userId,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CASHBACK_HISTORY_LIST_SUCCESS),
                        data
                )
        );
    }
}
