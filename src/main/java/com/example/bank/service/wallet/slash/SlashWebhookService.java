package com.example.bank.service.wallet.slash;



public interface SlashWebhookService {

    void handle(String payload);
}
