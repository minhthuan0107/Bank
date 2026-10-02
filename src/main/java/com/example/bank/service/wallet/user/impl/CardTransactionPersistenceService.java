package com.example.bank.service.wallet.user.impl;

import com.example.bank.dto.response.wallet.user.SlashTransactionResponse;
import com.example.bank.entity.wallet.Card;
import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.entity.wallet.SlashWebhookEvent;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.repository.wallet.SlashWebhookEventRepository;
import com.example.bank.repository.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardTransactionPersistenceService {

    private final CardTransactionRepository repository;
    private final CardRepository cardRepository;
    private final WalletRepository walletRepository;
    private final SlashWebhookEventRepository webhookEventRepository;

    /**
     * Check nhanh trước khi gọi Slash GET transaction.
     *
     * Đây chỉ là bước tối ưu để tránh gọi Slash API lại
     * khi cùng một event đã được xử lý thành công trước đó.
     *
     * Khi persistence thật sự chạy, eventId sẽ được kiểm tra
     * lại bên trong transaction.
     */
    @Transactional(readOnly = true)
    public boolean isEventProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }

        return webhookEventRepository.existsByEventId(eventId);
    }

    @Transactional
    public void upsertTransaction(
            SlashTransactionResponse tx,
            String eventType,
            String eventId,
            String payload,
            CardTransactionStatus newStatus
    ) {
        validate(
                tx,
                eventType,
                eventId,
                newStatus
        );

        /*
         * Lock Card trước.
         *
         * Mọi transaction cùng một card sẽ tuần tự khi thay đổi:
         * - spentAmount
         * - remainingAmount
         * - wallet balance
         *
         * Quan trọng:
         * việc GET transaction từ Slash đã thực hiện ở bên ngoài method này,
         * nên không giữ DB lock trong lúc chờ HTTP.
         */
        Card card = cardRepository
                .findBySlashCardIdForUpdate(tx.getCardId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Card not found for slashCardId="
                                        + tx.getCardId()
                        )
                );

        /*
         * Check eventId lại sau khi đã lấy lock.
         *
         * Check ở SlashWebhookService chỉ là optimization.
         * Check này mới nằm trong transaction xử lý dữ liệu.
         */
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.debug(
                    "CARD_TX_DUPLICATE_EVENT txId={} eventId={} slashCardId={}",
                    tx.getId(),
                    eventId,
                    tx.getCardId()
            );

            return;
        }

        BigDecimal newAmount = toAmount(
                tx.getAmountCents()
        );

        /*
         * Lock transaction hiện tại nếu đã tồn tại.
         *
         * slash_transaction_id là UNIQUE nên mỗi transaction Slash
         * chỉ tương ứng một CardTransaction local.
         */
        CardTransaction entity = repository
                .findBySlashTransactionIdForUpdate(tx.getId())
                .orElse(null);

        CardTransactionStatus oldStatus =
                entity != null
                        ? entity.getStatus()
                        : null;

        BigDecimal oldAmount =
                entity != null && entity.getAmount() != null
                        ? entity.getAmount()
                        : BigDecimal.ZERO;

        /*
         * Đồng bộ ảnh hưởng tài chính theo current state vừa GET từ Slash.
         *
         * Không còn dùng state machine webhook cũ kiểu:
         * PENDING -> POSTED
         * PENDING -> FAILED
         * ...
         *
         * PENDING / POSTED = tiền đang được áp dụng.
         * FAILED / REVERSED = tiền không còn được áp dụng.
         */
        reconcileBalance(
                card,
                oldStatus,
                newStatus,
                oldAmount,
                newAmount,
                tx.getId()
        );

        /*
         * Transaction mới chỉ được tạo sau khi đã có đầy đủ dữ liệu
         * bắt buộc để phù hợp schema NOT NULL.
         *
         * Không saveAndFlush entity rỗng như implementation cũ.
         */
        if (entity == null) {
            entity = new CardTransaction();
            entity.setSlashTransactionId(tx.getId());
        }

        /*
         * slashEventId chỉ lưu event gần nhất để audit/debug.
         * Không dùng field này để deduplicate webhook.
         */
        entity.setSlashEventId(eventId);

        entity.setCardId(tx.getCardId());
        entity.setUserId(card.getUserId());

        /*
         * Bank lưu amount dưới dạng giá trị tuyệt đối, luôn là số dương.
         */
        entity.setAmount(newAmount);
        entity.setCurrency("USD");

        entity.setStatus(newStatus);
        entity.setDetailedStatus(tx.getDetailedStatus());

        entity.setMerchantDescription(
                tx.getMerchantDescription()
        );

        entity.setDescription(
                tx.getDescription()
        );

        entity.setDeclineReason(
                tx.getDeclineReason()
        );

        entity.setAuthorizedAt(
                parseInstant(
                        tx.getAuthorizedAt(),
                        tx.getId(),
                        "authorizedAt"
                )
        );

        entity.setTransactionDate(
                parseInstant(
                        tx.getDate(),
                        tx.getId(),
                        "transactionDate"
                )
        );

        /*
         * Lưu raw webhook payload gần nhất.
         *
         * Đây không phải response của GET /transaction/{id}.
         */
        entity.setRawPayload(payload);

        repository.save(entity);

        /*
         * Chỉ đánh dấu event đã xử lý sau khi toàn bộ:
         * - Card
         * - Wallet
         * - CardTransaction
         *
         * đã được xử lý thành công.
         *
         * Method đang @Transactional nên nếu bất kỳ bước nào rollback
         * thì SlashWebhookEvent cũng rollback.
         */
        webhookEventRepository.save(
                SlashWebhookEvent.create(
                        eventId,
                        eventType,
                        tx.getId()
                )
        );

        log.info(
                "CARD_TX_SYNCED txId={} eventId={} eventType={} userId={} slashCardId={} oldStatus={} newStatus={} oldAmount={} newAmount={} detailedStatus={}",
                tx.getId(),
                eventId,
                eventType,
                card.getUserId(),
                tx.getCardId(),
                oldStatus,
                newStatus,
                oldAmount,
                newAmount,
                tx.getDetailedStatus()
        );
    }

    /**
     * Đồng bộ số tiền local đang bị transaction chiếm dụng
     * với trạng thái hiện tại lấy trực tiếp từ Slash.
     *
     * PENDING / POSTED:
     * transaction đang ảnh hưởng balance.
     *
     * FAILED / REVERSED:
     * transaction không còn ảnh hưởng balance.
     */
    private void reconcileBalance(
            Card card,
            CardTransactionStatus oldStatus,
            CardTransactionStatus newStatus,
            BigDecimal oldAmount,
            BigDecimal newAmount,
            String txId
    ) {
        BigDecimal oldAppliedAmount =
                isAppliedStatus(oldStatus)
                        ? oldAmount
                        : BigDecimal.ZERO;

        BigDecimal newAppliedAmount =
                isAppliedStatus(newStatus)
                        ? newAmount
                        : BigDecimal.ZERO;

        BigDecimal delta =
                newAppliedAmount.subtract(
                        oldAppliedAmount
                );

        /*
         * Không có thay đổi tài chính.
         *
         * Ví dụ:
         * null       -> REVERSED
         * null       -> FAILED
         * PENDING 50 -> POSTED 50
         * POSTED 50  -> POSTED 50
         * REVERSED   -> REVERSED
         */
        if (delta.signum() == 0) {
            log.debug(
                    "CARD_TX_BALANCE_UNCHANGED txId={} userId={} oldStatus={} newStatus={} oldAppliedAmount={} newAppliedAmount={}",
                    txId,
                    card.getUserId(),
                    oldStatus,
                    newStatus,
                    oldAppliedAmount,
                    newAppliedAmount
            );

            return;
        }

        Wallet wallet = walletRepository
                .findByUserIdForUpdate(card.getUserId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Wallet not found for userId="
                                        + card.getUserId()
                        )
                );

        /*
         * delta > 0:
         * trạng thái hiện tại của Slash yêu cầu giữ nhiều tiền hơn
         * số tiền local hiện đang giữ.
         *
         * Ví dụ:
         * null        -> PENDING 50
         * null        -> POSTED 50
         * PENDING 50  -> POSTED 60
         * REVERSED    -> POSTED 50
         */
        if (delta.signum() > 0) {
            debit(
                    card,
                    wallet,
                    delta
            );

            log.debug(
                    "CARD_TX_BALANCE_DEBIT txId={} userId={} amount={} oldStatus={} newStatus={} oldAmount={} newAmount={}",
                    txId,
                    card.getUserId(),
                    delta,
                    oldStatus,
                    newStatus,
                    oldAmount,
                    newAmount
            );

            return;
        }

        /*
         * delta < 0:
         * local hiện đang giữ nhiều tiền hơn trạng thái hiện tại của Slash.
         *
         * Ví dụ:
         * PENDING 50 -> FAILED
         * PENDING 50 -> REVERSED
         * POSTED 50  -> REVERSED
         * POSTED 60  -> POSTED 50
         */
        BigDecimal restoreAmount =
                delta.abs();

        restore(
                card,
                wallet,
                restoreAmount
        );

        log.debug(
                "CARD_TX_BALANCE_RESTORED txId={} userId={} amount={} oldStatus={} newStatus={} oldAmount={} newAmount={}",
                txId,
                card.getUserId(),
                restoreAmount,
                oldStatus,
                newStatus,
                oldAmount,
                newAmount
        );
    }

    /**
     * PENDING và POSTED đều được xem là tiền đang bị trừ.
     *
     * Theo nghiệp vụ Bank:
     * PENDING đã được Slash giữ/trừ tiền nên vẫn phải phản ánh
     * vào spentAmount và wallet balance.
     */
    private boolean isAppliedStatus(
            CardTransactionStatus status
    ) {
        return status == CardTransactionStatus.PENDING
                || status == CardTransactionStatus.POSTED;
    }

    /**
     * Trừ tiền vào Card và Wallet.
     */
    private void debit(
            Card card,
            Wallet wallet,
            BigDecimal amount
    ) {
        card.setSpentAmount(
                card.getSpentAmount().add(amount)
        );

        card.setRemainingAmount(
                card.getRemainingAmount().subtract(amount)
        );

        wallet.setTotalBalance(
                wallet.getTotalBalance().subtract(amount)
        );

        wallet.setAllocatedBalance(
                wallet.getAllocatedBalance().subtract(amount)
        );
    }

    /**
     * Hoàn lại phần tiền local đã bị transaction trừ trước đó.
     */
    private void restore(
            Card card,
            Wallet wallet,
            BigDecimal amount
    ) {
        card.setSpentAmount(
                card.getSpentAmount().subtract(amount)
        );

        card.setRemainingAmount(
                card.getRemainingAmount().add(amount)
        );

        wallet.setTotalBalance(
                wallet.getTotalBalance().add(amount)
        );

        wallet.setAllocatedBalance(
                wallet.getAllocatedBalance().add(amount)
        );
    }

    /**
     * Chuyển amountCents của Slash sang BigDecimal USD.
     *
     * Bank hiện lưu amount theo magnitude:
     * luôn là số dương.
     *
     * Ví dụ:
     * -5000 cents -> 50.0000 USD
     */
    private BigDecimal toAmount(Long amountCents) {
        if (amountCents == null) {
            throw new IllegalArgumentException(
                    "Slash transaction amountCents is null"
            );
        }

        return BigDecimal.valueOf(amountCents)
                .abs()
                .movePointLeft(2);
    }

    private void validate(
            SlashTransactionResponse tx,
            String eventType,
            String eventId,
            CardTransactionStatus status
    ) {
        if (tx == null) {
            throw new IllegalArgumentException(
                    "Slash transaction is null"
            );
        }

        if (tx.getId() == null
                || tx.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Slash transaction id is blank"
            );
        }

        if (tx.getCardId() == null
                || tx.getCardId().isBlank()) {

            throw new IllegalArgumentException(
                    "Slash card id is blank"
            );
        }

        if (eventType == null
                || eventType.isBlank()) {

            throw new IllegalArgumentException(
                    "Slash event type is blank"
            );
        }

        if (eventId == null
                || eventId.isBlank()) {

            throw new IllegalArgumentException(
                    "Slash event id is blank"
            );
        }

        if (status == null
                || status == CardTransactionStatus.UNKNOWN) {

            throw new IllegalArgumentException(
                    "Invalid card transaction status"
            );
        }
    }

    private Instant parseInstant(
            String value,
            String txId,
            String field
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);

        } catch (Exception e) {
            log.warn(
                    "CARD_TX_INVALID_DATE txId={} field={} value={}",
                    txId,
                    field,
                    value
            );

            return null;
        }
    }
}