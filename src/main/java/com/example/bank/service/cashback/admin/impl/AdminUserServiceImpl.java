package com.example.bank.service.cashback.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.user.UserException;
import com.example.bank.dto.request.cashback.admin.UpdateCardOpenLimitRequest;
import com.example.bank.dto.response.cashback.admin.AdminUserPageResponse;
import com.example.bank.dto.response.cashback.admin.AdminUserResponse;
import com.example.bank.entity.user.User;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.cashback.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
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
}