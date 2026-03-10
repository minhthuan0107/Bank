package com.example.bank.service.otp;

import com.example.bank.dto.request.otp.OtpRequest;
import com.example.bank.dto.response.otp.OtpEnqueuedResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface OtpService {
    OtpEnqueuedResponse handleOtpRequest(OtpRequest req, HttpServletRequest httpReq);

}
