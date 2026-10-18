package com.example.bank.service.sync;

import com.example.bank.repository.wallet.WithdrawOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawOtpExpirationServiceImpl
        implements WithdrawOtpExpirationService {

    private final WithdrawOrderRepository withdrawRepository;

    @Override
    @Transactional
    public int expireBatch(int batchSize) {

        if (batchSize <= 0 || batchSize > 1000) {
            throw new IllegalArgumentException(
                    "Invalid withdraw OTP expiration batch size"
            );
        }

        return withdrawRepository.expirePendingOtpBatch(batchSize);
    }
}