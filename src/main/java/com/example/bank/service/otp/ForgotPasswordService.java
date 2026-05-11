package com.example.bank.service.otp;

import com.example.bank.dto.request.wallet.user.ForgotPasswordOtpRequest;
import com.example.bank.dto.request.wallet.user.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface ForgotPasswordService {
    void requestResetOtp(ForgotPasswordOtpRequest request, HttpServletRequest httpRequest);

    void resetPassword(ResetPasswordRequest request);

}
