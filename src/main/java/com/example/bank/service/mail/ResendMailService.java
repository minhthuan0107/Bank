package com.example.bank.service.mail;

import com.example.bank.event.DepositOrderCreatedEvent;
import com.example.bank.event.WithdrawOrderPendingAdminEvent;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendMailService implements MailService {

    private final Resend resend;

    @Value("${resend.from.email}")
    private String fromEmail;

    @Value("${resend.from.name}")
    private String fromName;

    @Override
    @Async("mailTaskExecutor")
    public void sendOtp(String email, String otp) {
        String subject = "Your VCC Card Ads verification code";

        String body = """
                Your VCC Card Ads verification code is:

                %s

                This code will expire in 3 minutes.

                If you did not request this code, you can safely ignore this email.
                For your security, do not share this code with anyone.

                VCC Card Ads
                """.formatted(otp);

        sendTextEmail(email, subject, body, "OTP");
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendDepositOrderCreatedToAdmin(
            String adminEmail,
            DepositOrderCreatedEvent event
    ) {
        String subject = "[VCC Card Ads] Deposit order pending review";

        String body = """
                A new deposit order has been created and is waiting for admin review.

                Order details:
                - Order ID: %s
                - Order No: %s
                - User ID: %s

                Payment details:
                - Currency: %s
                - Network: %s
                - Amount: %s
                - Fee: %s
                - Expected Amount: %s
                - Deposit Address: %s

                Please log in to the VCC Card Ads admin dashboard to review this order.

                VCC Card Ads
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

    @Override
    @Async("mailTaskExecutor")
    public void sendWithdrawOrderPendingAdminToAdmin(
            String adminEmail,
            WithdrawOrderPendingAdminEvent event
    ) {
        String subject = "[VCC Card Ads] Withdrawal request pending approval";

        String body = """
                A withdrawal request has been verified by OTP and is waiting for admin approval.

                Order details:
                - Order ID: %s
                - Order No: %s
                - User ID: %s

                Withdrawal details:
                - Currency: %s
                - Network: %s
                - Amount: %s
                - To Address: %s

                Please log in to the VCC Card Ads admin dashboard to review this withdrawal request.

                VCC Card Ads
                """.formatted(
                event.orderId(),
                event.orderNo(),
                event.userId(),
                event.currency(),
                event.network(),
                event.amount(),
                event.toAddress()
        );

        sendTextEmail(adminEmail, subject, body, "WITHDRAW_ORDER_PENDING_ADMIN");
    }

    private void sendTextEmail(
            String to,
            String subject,
            String body,
            String mailType
    ) {
        if (to == null || to.isBlank()) {
            log.warn("{} email skipped because recipient is empty", mailType);
            return;
        }

        CreateEmailOptions email = CreateEmailOptions.builder()
                .from("%s <%s>".formatted(fromName, fromEmail))
                .to(to)
                .subject(subject)
                .text(body)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(email);

            log.info(
                    "{} email sent successfully to {} resendEmailId={}",
                    mailType,
                    maskEmail(to),
                    response.getId()
            );
        } catch (ResendException e) {
            log.error(
                    "{} email failed to send to {}",
                    mailType,
                    maskEmail(to),
                    e
            );
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
