package com.example.bank.controller.admin.profile;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.auth.ChangePasswordRequest;
import com.example.bank.dto.response.profile.AdminProfileResponse;
import com.example.bank.service.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final UserProfileService profileService;
    private final LocalizationUtils i18n;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileResponse>> getAdminProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        AdminProfileResponse response =
                profileService.getAdminProfile(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_PROFILE_SUCCESS),
                        response
                )
        );
    }

    @PutMapping("/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(currentUser.getId(), request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.CHANGE_PASSWORD_SUCCESS),
                        null
                )
        );
    }
}
