package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CardTransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "card_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_card_transactions_slash_transaction_id",
                        columnNames = "slash_transaction_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_card_transactions_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_card_transactions_card_id",
                        columnList = "card_id"
                ),
                @Index(
                        name = "idx_card_transactions_user_status",
                        columnList = "user_id,status"
                ),
                @Index(
                        name = "idx_card_transactions_transaction_date",
                        columnList = "transaction_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // ID duy nhất của transaction bên Slash.
    // Dùng làm key để đồng bộ/upsert transaction local.
    @Column(
            name = "slash_transaction_id",
            nullable = false,
            length = 100
    )
    private String slashTransactionId;

    // Event Slash gần nhất đã được đồng bộ vào transaction này.
    // Không dùng field này để deduplicate webhook.
    // Dedupe chính thức nằm ở bảng slash_webhook_events.
    @Column(name = "slash_event_id", length = 100)
    private String slashEventId;

    // ID card bên Slash.
    @Column(
            name = "card_id",
            nullable = false,
            length = 100
    )
    private String cardId;

    // ID user nội bộ của hệ thống Bank.
    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    // Giá trị tuyệt đối của giao dịch.
    // Bank luôn lưu amount dưới dạng số dương.
    //
    // Ví dụ Slash trả:
    // -5000 cents -> Bank lưu 50.0000 USD.
    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal amount;

    // Loại tiền của giao dịch.
    @Builder.Default
    @Column(
            name = "currency",
            nullable = false,
            length = 10
    )
    private String currency = "USD";

    // Trạng thái nghiệp vụ local của giao dịch:
    // PENDING / POSTED / FAILED / REVERSED.
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private CardTransactionStatus status;

    // Trạng thái chi tiết nguyên bản lấy từ Slash.
    // Ví dụ: pending, settled, declined, reversed, refund...
    @Column(
            name = "detailed_status",
            length = 100
    )
    private String detailedStatus;

    // Nội dung merchant của giao dịch.
    @Column(
            name = "merchant_description",
            length = 255
    )
    private String merchantDescription;

    // Mô tả giao dịch từ Slash.
    @Column(
            name = "description",
            length = 255
    )
    private String description;

    // Thời điểm giao dịch theo dữ liệu Slash.
    @Column(name = "transaction_date")
    private Instant transactionDate;

    // Thời điểm giao dịch được authorize.
    @Column(name = "authorized_at")
    private Instant authorizedAt;

    // Lý do giao dịch bị từ chối nếu có.
    @Column(
            name = "decline_reason",
            length = 255
    )
    private String declineReason;

    // Raw payload webhook gần nhất nhận từ Slash.
    // Không phải response GET /transaction/{id}.
    @Column(
            name = "raw_payload",
            columnDefinition = "TEXT"
    )
    private String rawPayload;
}