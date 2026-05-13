package com.example.bank.controller.user.profile;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.auth.ChangePasswordRequest;
import com.example.bank.dto.response.cashback.user.UserProfileResponse;
import com.example.bank.service.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final LocalizationUtils i18n;


        @GetMapping
        @PreAuthorize("hasRole('USER')")
        public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
                @AuthenticationPrincipal UserDetailsImpl currentUser
        ) {
            UserProfileResponse response =
                    userProfileService.getProfile(currentUser.getId());

            return ResponseEntity.ok(
                    ApiResponse.success(
                            HttpStatus.OK.value(),
                            i18n.getLocalizedMessage(MessageKeys.USER_PROFILE_SUCCESS),
                            response
                    )
            );
        }

        @PutMapping("/password")
        @PreAuthorize("hasRole('USER')")
        public ResponseEntity<ApiResponse<Void>> changePassword(
                @AuthenticationPrincipal UserDetailsImpl currentUser,
                @Valid @RequestBody ChangePasswordRequest request
        ) {
            userProfileService.changePassword(currentUser.getId(), request);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            HttpStatus.OK.value(),
                            i18n.getLocalizedMessage(MessageKeys.CHANGE_PASSWORD_SUCCESS),
                            null
                    )
            );
        }
    }

