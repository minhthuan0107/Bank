# Khóa và mở khóa thẻ Infini

## API

| Request | Mô tả |
| --- | --- |
| `POST {api.prefix}/infini-cards/{cardId}/lock` | Khóa thẻ (Infini `POST /v2/cards/freeze`) |
| `POST {api.prefix}/infini-cards/{cardId}/unlock` | Mở khóa thẻ (Infini `POST /v2/cards/unfreeze`) |

| HTTP | Khi nào |
| ---: | --- |
| 200 | Thành công |
| 404 | Thẻ không tồn tại hoặc không thuộc user |
| 409 | Thẻ đã khóa (khi khóa), đã mở (khi mở), không ở trạng thái hợp lệ, hoặc đang có lệnh nạp/rút chờ xử lý |
| 502 | Infini chưa khóa/mở được. Trạng thái trong hệ thống giữ nguyên như trước, user có thể thử lại |
| 503 | Tính năng Infini chưa bật hoặc thiếu cấu hình |

## Hạch toán (giống thẻ Slash)

Tổng tài sản (`total_balance`) không đổi.

| | Ví | Thẻ |
| --- | --- | --- |
| Khóa | `allocated -= b`, `frozen += b` | `locked_amount = b`, `remaining_amount = 0`, `BLOCKED` |
| Mở khóa | `frozen -= b`, `allocated += b` | `remaining_amount = b`, `locked_amount = 0`, `ACTIVE` |

`b` là số dư thẻ trong DB lúc khóa. Thẻ bị khóa không nạp, rút hay xem thông tin được.

## Thứ tự xử lý

**Khóa:**
1. Khóa trong DB trước (trong một transaction ngắn), để không ai tạo lệnh nạp/rút mới cho thẻ.
2. Gọi `freeze`. Infini trả `success: true` thì xong.
3. Nếu lỗi hoặc không rõ: hỏi `status`. Thẻ là `suspend` thì coi như đã khóa. Còn `active` hoặc không hỏi được thì **mở lại trong DB** và trả 502.

**Mở khóa:**
1. Gọi `unfreeze` trước, chưa đổi gì trong DB.
2. Infini trả `success: true`, hoặc `status` là `active`, thì mới mở trong DB.
3. Ngược lại giữ thẻ khóa và trả 502.

Thử lại luôn an toàn: nếu một lần trước đã khóa/mở ở Infini nhưng mất phản hồi, lần sau sẽ thấy đúng trạng thái qua `status` và cập nhật DB cho khớp.

## Admin quản lý thẻ Infini

Endpoint riêng, không dùng chung `/admin/cards` (thẻ Slash) để không lẫn ID thẻ:

| Request | Mô tả |
| --- | --- |
| `GET {api.prefix}/admin/infini-cards` | Danh sách thẻ Infini của mọi user. Lọc theo `cardNumber` (4 số cuối), `cardName`, `username`, `status`, `fromTime`, `toTime`; phân trang `page` |
| `POST {api.prefix}/admin/infini-cards/{cardId}/lock` | Khóa thẻ của bất kỳ user nào |
| `POST {api.prefix}/admin/infini-cards/{cardId}/unlock` | Mở khóa thẻ của bất kỳ user nào |

Khóa/mở của admin dùng đúng luồng của user (cùng hạch toán và kiểm tra lại với Infini). Log ghi `adminId`, `cardId` và `userId`. Danh sách không trả số thẻ đầy đủ hay ID thẻ phía Infini; có `locked_amount` để thấy số dư đang bị khóa.

## Admin khóa/mở tài khoản user

Khi admin khóa user (`UserLockedEvent`), hệ thống khóa mọi thẻ Slash **và mọi thẻ Infini `ACTIVE`** của user. Khi admin mở lại, mở mọi thẻ `BLOCKED`, kể cả thẻ user tự khóa trước đó (giống cách làm với thẻ Slash). Lỗi của từng thẻ được ghi log và không chặn các thẻ còn lại.

## Giả định cần kiểm tra trên Infini

- Tài liệu `freeze`/`unfreeze` không nêu trạng thái của thẻ bị đóng băng. Code dùng `suspend`, lấy từ danh sách trạng thái trong tài liệu webhook.
- Tài liệu không nói tiền trên thẻ bị đóng băng có còn nguyên không. Code giả định tiền vẫn ở trên thẻ (khóa không phải là rút tiền).

## Mã nguồn

- `service/wallet/infini/InfiniCardLockService`, `InfiniCardLockTransactionService` và `impl/`
- `InfiniCardClient.freeze`, `InfiniCardClient.unfreeze`
- Endpoint trong `controller/user/wallet/infini/InfiniCardController`
- `service/wallet/admin/impl/AdminUserCardStatusSyncServiceImpl` gọi thêm phần Infini
- `controller/admin/wallet/infini/AdminInfiniCardController`, `service/wallet/infini/AdminInfiniCardService` và `impl/`
- Postman: thư mục **Card lock** (`K1`, `K2`) và **Admin Infini cards** (`A1`–`A3`, cần `admin_access_token`)
