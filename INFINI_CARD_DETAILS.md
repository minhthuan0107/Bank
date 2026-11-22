# Xem số thẻ, CVV và ngày hết hạn thẻ Infini

## API

```http
GET {api.prefix}/infini-cards/{cardId}/sensitive-details
```

Response (`data`), cùng định dạng với bản Slash:

```json
{ "pan": "4111111111111335", "expiry_month": 8, "expiry_year": 2029,
  "cvv": "123", "created_at": "2026-11-18T15:17:15Z" }
```

| HTTP | Khi nào |
| ---: | --- |
| 200 | Thành công |
| 404 | Thẻ không tồn tại hoặc không thuộc user |
| 409 | Thẻ không ở trạng thái `ACTIVE` |
| 429 | Quá 10 lần xem trong giờ hiện tại |
| 503 | Infini không trả được dữ liệu, dữ liệu sai định dạng, hoặc không kiểm tra được giới hạn (Redis lỗi) |

## Cách hoạt động

1. Kiểm tra cấu hình Infini, thẻ thuộc user và đang `ACTIVE`.
2. Tăng bộ đếm Redis `card:reveal:hour:{userId}:{yyyyMMddHH}`. Quá 10 lần thì 429. Không đọc được Redis thì từ chối (503).
3. Gọi `POST /v2/cards/reveal` với `id` thẻ Infini. Key cần quyền `card.reveal` và IP nằm trong allowlist.
4. Kiểm tra định dạng (`card_number` 12–19 chữ số, `cvv` 3–4 chữ số, `expiration_mmyy` hợp lệ) và **4 số cuối khớp `last4` trong DB**, để không bao giờ trả nhầm thẻ.
5. Đổi `expiration_mmyy` (MMYY) thành `expiry_month` và `expiry_year`.

## Bảo mật

- PAN, CVV và ngày hết hạn **không được lưu** vào DB và **không ghi log**. Log chỉ có `Card details revealed userId=... cardId=...`.
- Response có `Cache-Control: no-store` và `Pragma: no-cache`.
- Chưa yêu cầu nhập lại mật khẩu hoặc OTP trước khi xem. Nên bổ sung trước khi mở cho khách thật nếu cần mức bảo vệ cao hơn.
- FE không nên lưu các giá trị này vào local storage, và nên ẩn lại sau vài giây.

## Mã nguồn

- `service/wallet/infini/InfiniCardSensitiveDetailService` và `impl/InfiniCardSensitiveDetailServiceImpl`
- `InfiniCardClient.reveal`
- Endpoint trong `controller/user/wallet/infini/InfiniCardController`
- Postman: thư mục **Card details**, request `R1`

Tham khảo: [Card API Infini](https://developer.infini.money/docs/en/card-api).
