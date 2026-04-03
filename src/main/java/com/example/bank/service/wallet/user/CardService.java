package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.user.BalanceResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;

public interface CardService {
    void createCard(CreateCardRequest request, Long userId) ;

    CardPageResponse getUserCards(Long userId, int page);

    BalanceResponse getUserBalance(Long userId) ;
}
