package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateCardRequest;

public interface CardService {
    void createCard(CreateCardRequest request, Long userId) ;
}
