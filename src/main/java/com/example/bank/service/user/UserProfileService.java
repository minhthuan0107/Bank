package com.example.bank.service.user;

import com.example.bank.dto.response.profile.AdminProfileResponse;
import com.example.bank.dto.request.auth.ChangePasswordRequest;
import com.example.bank.dto.response.cashback.user.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);

    AdminProfileResponse getAdminProfile(Long adminId);
}
