# Nạp và rút tiền thẻ Infini

## 1. Quy tắc

- Không thu phí. Nạp bao nhiêu thì ví giảm bấy nhiêu; rút bao nhiêu thì ví tăng bấy nhiêu.
- Mỗi lần tối thiểu **10 USD**, tối đa 2 chữ số thập phân.
- Chỉ thẻ `ACTIVE` mới nạp hoặc rút được.
- Mỗi thẻ chỉ có **một** yêu cầu nạp/rút đang chờ tại một thời điểm.
- Trước khi rút, backend hỏi Infini **số dư thật** của thẻ, vì số dư thẻ trong database chưa trừ chi tiêu.

## 2. API

| Request | Mô tả |
| --- | --- |
| `POST {api.prefix}/infini-cards/{cardId}/topup` | Nạp tiền từ ví vào thẻ |
| `POST {api.prefix}/infini-cards/{cardId}/withdraw` | Rút tiền từ thẻ về ví |
| `GET {api.prefix}/infini-cards/funding/{requestId}` | Trạng thái một yêu cầu |

Body của nạp và rút:

```json
{ "requestId": "<UUID mới cho mỗi lần>", "amount": 20.00 }
```

Gửi lại cùng `requestId` và cùng nội dung sẽ nhận lại kết quả cũ, không nạp/rút lần hai. Cùng `requestId` mà khác số tiền, thẻ hoặc loại thao tác thì bị 409.

Response (`data`):

```json
{ "request_id": "...", "type": "TOP_UP", "status": "SUCCEEDED",
  "card_id": 10014, "amount": 20.00, "card_balance": 30.00 }
```

| `status` | HTTP | Ý nghĩa |
| --- | ---: | --- |
| `SUCCEEDED` | 200 | Infini đã xác nhận, ví và thẻ đã cập nhật |
| `FAILED` | 200 | Infini từ chối, không có tiền nào bị chuyển |
| `PENDING`, `REVIEW_REQUIRED` | 202 | Chưa rõ kết quả, đang chờ webhook Infini |

`card_balance` là số dư thẻ do Infini trả về sau thao tác.

## 3. Hạch toán

`total_balance` không bao giờ đổi khi nạp/rút.

| Bước | Ví |
| --- | --- |
| Nạp: giữ chờ | `available -= a`, `frozen += a` |
| Nạp: thành công | `frozen -= a`, `allocated += a`, thẻ `+= a` |
| Nạp: thất bại | `frozen -= a`, `available += a` |
| Rút: thành công | `allocated -= a`, `available += a`, thẻ `-= a` |
| Rút: thất bại | Không đổi gì |

## 4. Gọi Infini và xử lý kết quả

- `POST /v2/cards/top-up` và `POST /v2/cards/redeem`, gửi `id`, `amount`, `token_type`, `note` (= `requestId`). Không tự gửi lại, vì Infini không có idempotency cho hai API này.
- Có `tx_id` trong phản hồi: chốt ngay (`SUCCEEDED`).
- HTTP 401/403/400/404/422/429 hoặc `code` khác 0: `FAILED`, trả lại khoản giữ chờ của lệnh nạp.
- Timeout, 5xx, phản hồi sai: `REVIEW_REQUIRED`, giữ nguyên. Kết quả cuối do webhook quyết định.
- Lệnh chưa từng gửi quá 2 phút: `FAILED`. Lệnh đã gửi nhưng tiến trình dừng trước khi ghi kết quả: `REVIEW_REQUIRED`.

## 5. Webhook Infini

Endpoint: `POST {api.prefix}/webhook/infini` (không cần JWT, xác thực bằng chữ ký).

- Chữ ký: HMAC-SHA256 dạng hex trên `{X-Webhook-Timestamp}.{X-Webhook-Event-Id}.{raw body}`, khóa là `INFINI_WEBHOOK_SECRET`. Timestamp lệch quá 1 giờ bị từ chối.
- Sự kiện được lưu vào `infini_webhook_events` (chống trùng theo event ID) rồi mới trả 200. Xử lý lỗi thì scheduler thử lại, tối đa 10 lần.
- `card.transaction` loại `top_up`/`redeem`:
  - `completed` hoặc `failed` cho lệnh chưa rõ kết quả của đúng thẻ đó, đúng loại và **đúng số tiền**: chốt hoặc trả lại.
  - Không khớp (ví dụ nạp thủ công trên dashboard Infini): bỏ qua, không gắn vào lệnh nào.
  - `failed` cho lệnh đã `SUCCEEDED`: chỉ đánh dấu `review_reason = PROVIDER_REPORTED_FAILED`, không tự đảo tiền.
- `card.status_change`: alias là `requestId` của lệnh tạo thẻ, nên dùng để gắn ID thẻ và đối soát lệnh tạo thẻ ngay, không chờ vòng hỏi.
- Webhook chỉ là tín hiệu. Lệnh tạo thẻ vẫn được kiểm tra lại bằng API `status` trước khi chốt.

Cấu hình trên Infini: Developer → Webhook Settings, URL HTTPS công khai trỏ tới endpoint trên, đăng ký `card.transaction` và `card.status_change`, lấy secret đặt vào `INFINI_WEBHOOK_SECRET`. Khi chạy trên máy cá nhân cần một đường hầm HTTPS (ví dụ ngrok).

## 6. Cấu hình

| Biến môi trường | Ý nghĩa |
| --- | --- |
| `INFINI_WEBHOOK_SECRET` | Khóa ký webhook. Để trống thì webhook bị từ chối (503) và Infini thử lại sau |
| `INFINI_FUNDING_POLL_DELAY_MS` | Chu kỳ scheduler dọn lệnh và thử lại webhook, mặc định `30000` |

## 7. Database (Flyway V15)

- `infini_funding_orders`: một dòng cho mỗi `requestId`; `provider_tx_id` duy nhất.
- `infini_webhook_events`: sự kiện webhook đã xác minh.

## 8. Chưa làm

- Đồng bộ giao dịch chi tiêu (`consume`, `refund`, `reversal`) vào số dư thẻ và dashboard.
- Khóa/mở khóa, xem PAN/CVV cho thẻ Infini.
- Giao diện admin xử lý lệnh `REVIEW_REQUIRED`.

## 9. Tham khảo

- [Card API Infini](https://developer.infini.money/docs/en/card-api)
- [Webhook Infini](https://developer.infini.money/docs/en/7-webhook)
