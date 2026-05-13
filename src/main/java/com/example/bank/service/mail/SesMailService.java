package com.example.bank.service.mail;

import com.example.bank.event.DepositOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

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

        sendTextEmail(email, subject, body, "OTP");
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendDepositOrderCreatedToAdmin(
            String adminEmail,
            DepositOrderCreatedEvent event
    ) {
        String subject = "[Bank] New deposit order needs approval";

        String body = """
                A new deposit order has been created and is waiting for admin review.

                Order ID: %s
                Order No: %s
                User ID: %s

                Currency: %s
                Network: %s
                Amount: %s
                Fee: %s
                Expected Amount: %s
                Deposit Address: %s

                Please log in to the admin dashboard to review and approve this deposit order.
                """.formatted(
                event.orderId(),
                event.orderNo(),
                event.userId(),
                event.currency(),
                event.network(),
                event.amount(),
                event.fee(),
                event.expectedAmount(),
                event.depositAddress()
        );

        sendTextEmail(adminEmail, subject, body, "DEPOSIT_ORDER_CREATED");
    }

    private void sendTextEmail(
            String to,
            String subject,
            String body,
            String mailType
    ) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source("%s <%s>".formatted(fromName, fromEmail))
                .destination(
                        Destination.builder()
                                .toAddresses(to)
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
            log.info("{} email sent to {}", mailType, maskEmail(to));
        } catch (Exception e) {
            log.error("{} email failed to send to {}", mailType, maskEmail(to), e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        int at = email.indexOf("@");

        if (at <= 2) {
            return "***" + email.substring(at);
        }

        return email.substring(0, 2)
                + "***"
                + email.substring(at);
    }
}
