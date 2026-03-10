package com.example.bank.controller.user;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.otp.OtpRequest;
import com.example.bank.dto.response.otp.OtpEnqueuedResponse;
import com.example.bank.service.otp.OtpService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/otp")
@RequiredArgsConstructor
public class OtpController {
    private final LocalizationUtils i18n;
    private final OtpService otpService;
    // Api gửi mã OTP đến tài khoản người dùng
    @PermitAll
    @PostMapping(value = "/request")
    public ResponseEntity<ApiResponse> requestOtp(
            @Valid @RequestBody OtpRequest requestDto,
            HttpServletRequest request
    ) {
        // Gọi service: validate (email/SDT), kiểm tra tồn tại theo purpose,
        OtpEnqueuedResponse otpEnqueuedResponse = otpService.handleOtpRequest(requestDto, request);
        // Trả về thông báo chuẩn hoá
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.OTP_ENQUEUED),
                        otpEnqueuedResponse));

    }


}
