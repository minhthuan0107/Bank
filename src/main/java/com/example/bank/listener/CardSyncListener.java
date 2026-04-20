package com.example.bank.listener;
import com.example.bank.event.CardCreatedEvent;
import com.example.bank.service.sync.CardSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardSyncListener {
    private final CardSyncService cardSyncService;

    @Async("webhookTaskExecutor") // Sử dụng chung pool bạn vừa cấu hình
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CardCreatedEvent event) {
        log.info("Starting sync for card: {}", event.cardId());
        try {
            // Thay vì sleep cứng, hãy để syncCard tự retry nếu cần
            // Thread.sleep(3000);
            cardSyncService.syncCard(event.slashCardId(), event.cardId());
            log.info("Sync success for card: {}", event.cardId());
        } catch (Exception e) {
            log.error("SYNC FAIL for card {}: {}", event.cardId(), e.getMessage(), e);
            // Có thể bắn message ra một Dead Letter Queue hoặc lưu vào bảng logs_error để xử lý sau
        }
    }
}
