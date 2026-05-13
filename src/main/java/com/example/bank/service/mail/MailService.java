package com.example.bank.service.mail;

import com.example.bank.event.DepositOrderCreatedEvent;
import com.example.bank.event.WithdrawOrderPendingAdminEvent;

public interface MailService {

    void sendOtp(String email, String otp);

    void sendDepositOrderCreatedToAdmin(String adminEmail, DepositOrderCreatedEvent event);


    void sendWithdrawOrderPendingAdminToAdmin(
            String adminEmail,
            WithdrawOrderPendingAdminEvent event
    );
}
