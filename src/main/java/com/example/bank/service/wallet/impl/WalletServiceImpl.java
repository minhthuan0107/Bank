package com.example.bank.service.wallet.impl;

import com.example.bank.entity.wallet.Wallet;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public void createWallet(Long userId) {

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .frozenBalance(BigDecimal.ZERO)
                .status("ACTIVE")
                .isLocked(false)
                .version(0)
                .build();
        walletRepository.save(wallet);
    }
}