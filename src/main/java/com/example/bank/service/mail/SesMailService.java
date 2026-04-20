package com.example.bank.service.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SesMailService implements MailService {

    private final SesClient sesClient;

    @Value("${ses.from.email}")
    private String fromEmail;

    @Value("${ses.from.name}")
    private String fromName;

    @Override
    @Async("mailTaskExecutor")
    public void sendOtp(String email, String otp) {
        String subject = "Bank OTP Verification";

        String body = """
                Your OTP code is: %s
                This code will expire in 3 minutes.
                If you did not request this, please ignore this email.
                """.formatted(otp);

        SendEmailRequest request = SendEmailRequest.builder()
                .source("%s <%s>".formatted(fromName, fromEmail))
                .destination(
                        Destination.builder()
                                .toAddresses(email)
                                .build()
                )
                .message(
                        Message.builder()
                                .subject(
                                        Content.builder()
                                                .data(subject)
                                                .charset("UTF-8")
                                                .build()
                                )
                                .body(
                                        Body.builder()
                                                .text(
                                                        Content.builder()
                                                                .data(body)
                                                                .charset("UTF-8")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            sesClient.sendEmail(request);
            log.info("OTP email sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", maskEmail(email), e);

        }
    }
    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 2) return email;
        return email.substring(0, 2)
                + "***"
                + email.substring(at);
    }

}