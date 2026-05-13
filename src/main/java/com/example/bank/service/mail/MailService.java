package com.example.bank.service.mail;

import com.example.bank.event.DepositOrderCreatedEvent;

public interface MailService {

    void sendOtp(String email, String otp);

    void sendDepositOrderCreatedToAdmin(String adminEmail, DepositOrderCreatedEvent event);

}
