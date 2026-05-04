package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.user.UserException;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.cashback.admin.UpdateCardOpenLimitRequest;
import com.example.bank.dto.response.cashback.admin.AdminUserPageResponse;
import com.example.bank.dto.response.cashback.admin.AdminUserResponse;
import com.example.bank.entity.user.User;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.event.UserLockedEvent;
import com.example.bank.event.UserUnlockedEvent;
import com.example.bank.repository.auth.AuthSessionRepository;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.wallet.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final WalletProperties walletProperties;
    private final AuthSessionRepository authSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public AdminUserPageResponse getUsers(
            int page
    ) {
        if (page < 0) {
            page = 0;
        }
       int size = walletProperties.getDefaultPageSize();
        Page<User> pageData = userRepository.findByRoleName(
                "USER",
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );

        List<AdminUserResponse> items = pageData.getContent()
                .stream()
                .map(this::toAdminUserResponse)
                .toList();

        return AdminUserPageResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalSize(pageData.getTotalElements())
                .hasNext(pageData.hasNext())
                .build();
    }

    @Override
    @Transactional
    public AdminUserResponse updateCardOpenLimit(
            Long userId,
            UpdateCardOpenLimitRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new UserException(
                    MessageKeys.USER_CARD_LIMIT_UPDATE_NOT_ALLOWED,
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setCardOpenLimit(request.getCardOpenLimit());
        user.setUpdatedAt(Instant.now());
        return toAdminUserResponse(user);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .cardOpenLimit(user.getCardOpenLimit())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void lockUser(Long userId) {

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new WalletException(
                    MessageKeys.USER_ALREADY_LOCKED,
                    HttpStatus.CONFLICT
            );
        }

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new WalletException(
                    MessageKeys.USER_STATUS_UPDATE_NOT_ALLOWED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // 1. Khóa user
        user.setStatus(AccountStatus.LOCKED);

        // 2. Revoke toàn bộ refresh token/session
        authSessionRepository.revokeAllByUserId(
                userId,
                Instant.now()
        );

        // 3. Sau commit mới async khóa toàn bộ card trên Slash
        eventPublisher.publishEvent(
                new UserLockedEvent(userId)
        );
    }

    @Override
    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new WalletException(
                    MessageKeys.USER_ALREADY_ACTIVE,
                    HttpStatus.CONFLICT
            );
        }

        if (user.getStatus() == AccountStatus.DELETED) {
            throw new WalletException(
                    MessageKeys.USER_STATUS_UPDATE_NOT_ALLOWED,
                    HttpStatus.BAD_REQUEST
            );
        }

        // 1. Mở khóa user
        user.setStatus(AccountStatus.ACTIVE);

        // 2. Sau commit mới async mở lại card trên Slash
        eventPublisher.publishEvent(
                new UserUnlockedEvent(userId)
        );
    }
}