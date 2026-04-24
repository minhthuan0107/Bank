package com.example.bank.service.cashback.admin;


import com.example.bank.dto.request.cashback.admin.UpdateCardOpenLimitRequest;
import com.example.bank.dto.response.cashback.admin.AdminUserPageResponse;
import com.example.bank.dto.response.cashback.admin.AdminUserResponse;

public interface AdminUserService {

    AdminUserPageResponse getUsers(
            int page
    );

    AdminUserResponse updateCardOpenLimit(
            Long userId,
            UpdateCardOpenLimitRequest request
    );
}
