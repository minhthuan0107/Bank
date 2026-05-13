package com.example.bank.service.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.auth.AuthException;
import com.example.bank.dto.response.profile.AdminProfileResponse;
import com.example.bank.dto.request.auth.ChangePasswordRequest;
import com.example.bank.dto.response.cashback.user.UserProfileResponse;
import com.example.bank.entity.user.User;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.user.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .cardOpenLimit(user.getCardOpenLimit())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException(
                    MessageKeys.CURRENT_PASSWORD_INVALID,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!request.isNewPasswordMatched()) {
            throw new AuthException(
                    MessageKeys.PASSWORD_CONFIRM_NOT_MATCH,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AuthException(
                    MessageKeys.NEW_PASSWORD_SAME_AS_OLD,
                    HttpStatus.BAD_REQUEST
            );
        }

        int currentPasswordVersion = user.getPasswordVersion() == null
                ? 0
                : user.getPasswordVersion();

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordVersion(currentPasswordVersion + 1);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProfileResponse getAdminProfile(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AuthException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        return AdminProfileResponse.builder()
                .username(admin.getUsername())
                .email(admin.getEmail())
                .build();
    }
}