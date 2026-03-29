package com.example.bank.service.sync;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.dto.response.slash.SlashCardDetailResponse;
import com.example.bank.repository.wallet.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSyncService {

    private final CardRepository cardRepository;
    private final SlashClient slashClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncCard(String slashCardId, Long cardId) {
        int maxRetry = 5;
        for (int i = 0; i < maxRetry; i++) {
            try {
                SlashCardDetailResponse res = slashClient.getCard(slashCardId);

                if (res.getLast4() != null && !res.getLast4().isBlank()) {
                    cardRepository.updateCardBasicInfo(
                            cardId,
                            res.getLast4(),
                            Integer.parseInt(res.getExpiryMonth()),
                            Integer.parseInt(res.getExpiryYear())
                    );
                    return;
                }
                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("SYNC ERROR {}", e.getMessage());
            }
        }
        log.error("SYNC FAILED cardId={}", cardId);
    }
}
