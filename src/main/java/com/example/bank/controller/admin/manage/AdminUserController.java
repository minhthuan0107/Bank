package com.example.bank.controller.admin.manage;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.cashback.admin.UpdateCardOpenLimitRequest;
import com.example.bank.dto.response.cashback.admin.AdminUserPageResponse;
import com.example.bank.dto.response.cashback.admin.AdminUserResponse;
import com.example.bank.service.wallet.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final LocalizationUtils i18n;


    /**
     * Admin lấy danh sách user
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserPageResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page
    ) {
        AdminUserPageResponse data =
                adminUserService.getUsers(page);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_USER_LIST_SUCCESS),
                        data
                )
        );
    }

    /**
     * Admin cập nhật giới hạn mở thẻ của user
     */
    @PatchMapping("/{userId}/card-open-limit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateCardOpenLimit(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateCardOpenLimitRequest request
    ) {
        AdminUserResponse data =
                adminUserService.updateCardOpenLimit(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CARD_OPEN_LIMIT_UPDATE_SUCCESS),
                        data
                )
        );
    }

    /**
     * Admin khóa user.
     * Đồng thời revoke toàn bộ refresh token/session.
     * Sau commit sẽ async khóa toàn bộ thẻ của user trên Slash.
     */
    @PatchMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @PathVariable Long userId
    ) {
        adminUserService.lockUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.USER_LOCKED_SUCCESS),
                        null
                )
        );
    }

    /**
     * Admin mở khóa user.
     * Sau commit sẽ async mở lại toàn bộ thẻ BLOCKED của user trên Slash.
     */
    @PatchMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable Long userId
    ) {
        adminUserService.unlockUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.USER_UNLOCKED_SUCCESS),
                        null
                )
        );
    }
}
