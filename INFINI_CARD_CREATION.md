# Tạo thẻ Infini: database, phí và API dành cho FE

## 1. Quy tắc tính tiền

- `amount` là số USD nạp vào thẻ, tối thiểu **10 USD**, tối đa 2 chữ số thập phân.
- Phí tạo thẻ của website cố định **1 USD**, chỉ chốt khi xác nhận tạo thẻ thành công.
- Tổng tiền cần có trong ví: `totalDebit = amount + 1.00`.
- Số tiền tạo thẻ tối đa: `maxAmount = max(availableBalance - 1.00, 0)`, làm tròn xuống 2 chữ số thập phân.

| Số dư khả dụng trong ví | Tiền vào thẻ | Phí tạo thẻ | Kết quả |
| ---: | ---: | ---: | --- |
| 10 USD | 10 USD | 1 USD | Không đủ số dư |
| 11 USD | 10 USD | 1 USD | Cho phép |
| 50 USD | 49 USD | 1 USD | Cho phép |
| 50 USD | 50 USD | 1 USD | Không đủ số dư |
| 51 USD | 50 USD | 1 USD | Cho phép |

Phí thực tế của Infini được lưu riêng trong `provider_fee` và `provider_total_pay`, không tự cộng thêm vào phí 1 USD của khách. Cần bảo đảm nguồn tiền, quy đổi và chi phí nhà cung cấp trước khi vận hành thực tế.

## 2. Flyway V11, V12 và V13

Các file migration:

- [V14__add_apply_started_at_to_card_creation_orders.sql](src/main/resources/db/migration/V14__add_apply_started_at_to_card_creation_orders.sql): cột `apply_started_at`, ghi trước khi gửi `apply`.
- [V11__add_infini_card_creation.sql](src/main/resources/db/migration/V11__add_infini_card_creation.sql).
- [V12__remove_infini_per_user_email.sql](src/main/resources/db/migration/V12__remove_infini_per_user_email.sql).
- [V13__separate_infini_cards.sql](src/main/resources/db/migration/V13__separate_infini_cards.sql).

V13 tạo bảng riêng `cards_infini`. Mọi thẻ Infini phát hành thành công được lưu tại đây, không lưu mới vào `cards`. Nếu đã có thẻ Infini trong bảng cũ, migration sao chép sang bảng mới và giữ nguyên ID nội bộ. Bản cũ được giữ làm lịch sử; entity `Card` chỉ đọc dữ liệu `provider = SLASH` để tránh tính trùng. Các cột chuyển tiếp V11 trong database cũ chưa bị xóa.

Cột liên kết trong `card_creation_orders` được đổi từ `card_id` thành `infini_card_id`, có khóa ngoại đến `cards_infini.id`. Thuộc tính Java `cardId` và trường JSON `card_id` vẫn là ID nội bộ, không phải ID Infini bên ngoài. Tổng tiền trong ví không bị thay đổi bởi migration.


Giữ nguyên nội dung và checksum V11 nếu đã chạy. Flyway sẽ chạy tiếp V12.

| Bảng | Thay đổi |
| --- | --- |
| `users` | V11 thêm `infini_member_email`; V12 bỏ cột này vì mọi yêu cầu dùng chung email merchant |
| `cards` | Thêm `provider`, `provider_card_id`, `product_id`, `token_type`, `creation_fee`, `holder_name`, `holder_email`; cho phép `slash_card_id` là `NULL` |
| `card_creation_orders` | Bảng mới lưu yêu cầu, số tiền, phí, tổng tiền giữ chờ, trạng thái, ID nhà cung cấp và kết quả đối soát |

Không sửa hoặc xóa dữ liệu Slash trong `card_bins` và `card_holders`. Luồng Infini không tạo bản ghi `card_holders` vì request mới không thu thập địa chỉ thanh toán. Bảng ví đã có các cột cần thiết nên không cần `ALTER TABLE wallets`.

Cột `member_email` trong `card_creation_orders` được giữ làm bản lưu email đã gửi cho nhà cung cấp; thuộc tính Java tương ứng là `applicantEmail`. Đây không còn là liên kết email theo từng user.

V11 giả định database hiện có các bảng cũ và migration V1–V10; không dùng để khởi tạo một database trống.

Sao lưu và kiểm tra trên database thử nghiệm trước. Khi chạy ứng dụng với Flyway được bật, Flyway tự áp dụng các migration V11/V12/V13 còn thiếu. Không sửa các migration đã áp dụng để tránh sai checksum. Các migration này chưa được chạy vào database thật trong phiên làm việc.

## 3. Cấu hình Infini

Cấu hình được khai báo trong `InfiniProperties` và `application.yml` (khóa `infini.*`, đọc từ các biến môi trường dưới đây, đều có giá trị mặc định nên ứng dụng vẫn khởi động khi Infini đang tắt):

| Biến môi trường | Giá trị / ý nghĩa |
| --- | --- |
| `INFINI_ENABLED` | `true` để bật; mặc định `false` |
| `INFINI_BASE_URL` | Mặc định `https://openapi-sandbox.infini.money`; chỉ gồm origin HTTPS, không kèm `/v2/cards` |
| `INFINI_KEY_ID` | Key ID được Infini cấp |
| `INFINI_PRIVATE_KEY_PATH` | Đường dẫn file khóa riêng Ed25519, định dạng PKCS#8 PEM, trên backend |
| `INFINI_TOKEN_TYPE` | Mặc định `USDT` |
| `INFINI_APPLICANT_EMAIL` | **Bắt buộc**, không có giá trị mặc định. Email thành viên tổ chức trên Infini, dùng chung cho mọi yêu cầu |
| `INFINI_PRODUCT_ID` | Mặc định `2`; backend lấy sản phẩm từ cấu hình |
| `INFINI_CREATION_POLL_DELAY_MS` | Mặc định `10000` mili giây |

Tài khoản tích hợp cần quyền `card.create`, IP nằm trong danh sách cho phép và đủ nguồn tiền bên Infini.

Mọi user trên website dùng chung một email merchant, đặt qua biến môi trường `INFINI_APPLICANT_EMAIL` (không lưu trong repo). Thiếu biến này thì khi bật Infini, `POST /wallet/create-card` trả 503. Backend không nhận email, token hoặc productId từ FE và không yêu cầu liên kết email từng user. Cần bảo đảm Infini chấp nhận email chung này cho tài khoản tích hợp.

## 4. Request tạo thẻ

Endpoint:

```http
POST {api.prefix}/wallet/create-card
```

Request FE sử dụng `camelCase`:

```json
{
  "requestId": "4bb5c655-1dd4-4bb9-8dde-060c2ba222fe",
  "name": "Thẻ mua sắm",
  "amount": 10.00,
  "holderName": "Nguyen Van An",
  "note": "Ghi chú nội bộ"
}
```

FE tạo UUID một lần cho mỗi yêu cầu tạo thẻ. Nếu mất mạng hoặc timeout, gửi lại **cùng `requestId` và cùng nội dung**, không tạo UUID mới. Thay đổi số tiền, tên thẻ, tên chủ thẻ hoặc ghi chú với UUID đã dùng sẽ bị từ chối.

Request mới bỏ `bin` và đối tượng `holder` chứa địa chỉ. Backend lấy email chung, token và productId từ cấu hình. `holderName` là tên chủ thẻ gửi sang Infini; `note` là ghi chú nội bộ.

## 5. Response và các API hỗ trợ FE

Response vẫn được bọc bằng `ApiResponse`. Phần `data` sử dụng `snake_case`:

```json
{
  "request_id": "4bb5c655-1dd4-4bb9-8dde-060c2ba222fe",
  "status": "PENDING",
  "provider": "INFINI",
  "card_id": null,
  "amount": 10.00,
  "fee": 1.00,
  "total_debit": 11.00
}
```

Đối với API POST tạo thẻ:

| Trạng thái | HTTP | Ý nghĩa |
| --- | ---: | --- |
| `PENDING` | 202 | Đang xử lý |
| `REVIEW_REQUIRED` | 202 | Cần đối soát kết quả với nhà cung cấp |
| `SUCCEEDED` | 200 | Đã xác nhận tạo thẻ thành công |
| `FAILED` | 200 | Yêu cầu bị từ chối và khoản giữ chờ đã được giải phóng |

FE phải đọc `status`; HTTP 200 không tự có nghĩa là đã tạo thẻ thành công.

### Lấy cấu hình tạo thẻ

```http
GET {api.prefix}/wallet/card-creation-config
```

Trả về `min_amount`, `creation_fee`, `available_balance`, `max_amount`, `can_create` và `product_ids` (hiện chỉ chứa sản phẩm cấu hình: `[2]`).

`can_create` phản ánh điều kiện số dư, trạng thái ví và việc tính năng đã bật. Khi nhận yêu cầu tạo thẻ, backend vẫn kiểm tra cấu hình nhà cung cấp, hạn mức, sản phẩm và lệnh đang chờ.

### Theo dõi yêu cầu tạo thẻ

```http
GET {api.prefix}/wallet/card-creation/{requestId}
```

Trả kết quả thuộc user đang đăng nhập. API GET trả HTTP 200 khi tìm thấy yêu cầu; trạng thái xử lý nằm trong `data.status`.

FE gọi định kỳ endpoint này để theo dõi kết quả, không gọi POST với UUID mới để kiểm tra trạng thái.

## 6. Hạch toán và đối soát

Căn cứ tài liệu Infini (Card API, Authentication): `apply` không có idempotency key; lỗi nghiệp vụ trả HTTP 200 kèm `code` khác 0; chỉ 401/403 được mô tả cho lỗi xác thực. Tài liệu **không** liệt kê trạng thái thất bại của thẻ (chỉ `init`, `pending`, `active`, `pending_delete`, `deleted`) và không liệt kê mã lỗi nghiệp vụ của Card API.

1. Mở transaction ngắn, khóa ví USD, kiểm tra số tiền cộng phí, hạn mức và lệnh đang chờ.
2. Lưu yêu cầu và giữ chờ tổng tiền:
   `availableBalance -= amount + fee`;
   `frozenBalance += amount + fee`.
   `totalBalance` chưa thay đổi.
3. Commit, rồi ghi `apply_started_at` (transaction ngắn) ngay trước khi gọi `POST /v2/cards/apply`. Không tự động gửi lại thao tác tạo thẻ. `card_alias` gửi sang Infini chính là `requestId`.
4. Lưu ID Infini trả về, sau đó scheduler kiểm tra `GET /v2/cards/status`. Request của user trả `PENDING` ngay, không chờ thẻ hoạt động.
5. Khi thẻ ở trạng thái `active`, tiền tệ là USD và số dư ban đầu khớp, tạo thẻ nội bộ và chốt đúng một lần:
   `frozenBalance -= amount + fee`;
   `allocatedBalance += amount`;
   `totalBalance -= fee`.

### Khi nào giải phóng khoản giữ chờ

Chỉ khi chắc chắn Infini không tạo thẻ. Khoản giữ chờ quay về `availableBalance`, lệnh thành `FAILED`; phí chưa bị trừ.

| Tình huống | Kết quả |
| --- | --- |
| `apply` trả HTTP 401/403 | `FAILED` (`AUTHENTICATION_REJECTED`) |
| `apply` trả HTTP 400, 404, 422, 429 | `FAILED` (`PROVIDER_REJECTED`) |
| `apply` trả HTTP 200 với `code` khác 0 | `FAILED` (`PROVIDER_ERROR_<code>`) |
| Lệnh chưa từng gửi (`apply_started_at` rỗng) quá 2 phút | `FAILED` (`APPLY_NOT_SENT`) |
| Đã gửi, tra theo `card_alias` không thấy thẻ sau hơn 60 phút | `FAILED` (`PROVIDER_CARD_NOT_FOUND`) |

### Khi nào giữ tiền và chờ

| Tình huống | Kết quả |
| --- | --- |
| Timeout, 5xx, phản hồi sai định dạng, lỗi lưu ID sau khi gọi | `REVIEW_REQUIRED`, giữ tiền |
| Đã gửi nhưng chưa có ID: scheduler tra `GET /v2/cards/list?card_alias=<requestId>` | Thấy đúng 1 thẻ: lưu ID, trở lại `PENDING` và chạy luồng bình thường. Thấy nhiều thẻ: `REVIEW_REQUIRED` (`PROVIDER_LOOKUP_AMBIGUOUS`). Không thấy dưới 60 phút: `REVIEW_REQUIRED`. |
| Lỗi tạm thời khi gọi `status` hoặc `list` | Giữ nguyên trạng thái, hỏi lại sau; chỉ khi lệnh quá 30 phút mới thành `REVIEW_REQUIRED` (`STATUS_UNAVAILABLE`) |
| Thẻ còn `init`/`pending` quá 30 phút | `REVIEW_REQUIRED` (`PROVIDER_TIMEOUT`) |
| Trạng thái khác `active` (ví dụ `pending_delete`, `deleted`, trạng thái chưa được tài liệu) hoặc số dư ban đầu lệch | `REVIEW_REQUIRED`, giữ tiền |

Lệnh `REVIEW_REQUIRED` đã có ID vẫn được hỏi lại và tự chốt khi thẻ `active`. Khoảng hỏi lại tăng dần: 15 giây (dưới 5 phút), 60 giây (dưới 30 phút), 5 phút sau đó.

Khóa database và `requestId` giúp tránh giữ tiền, thu phí hoặc tạo trùng trong các lần gửi lại cùng một yêu cầu. Lệnh đang chờ sẽ chặn yêu cầu mới của cùng user; lỗi `card.creation.already_pending` trả `data.request_id` của lệnh đang chờ, và `can_create` trong cấu hình là `false`.

Chưa có giao diện quản trị. Lệnh `REVIEW_REQUIRED` không tự giải quyết được cần đối soát với Infini bằng tay. Không sửa trực tiếp số dư ví để giải phóng một lệnh chưa xác minh.

Giả định cần kiểm tra trên Sandbox: bộ lọc `card_alias` của `/v2/cards/list` khớp chính xác (code đã so sánh lại bằng `equals`); danh sách mặc định trả mọi trạng thái; mã 400/404/422/429 của `apply` luôn xảy ra trước khi tạo thẻ; `code` khác 0 nghĩa là chưa tạo thẻ.

## 7. Cấu trúc code

Mọi code Infini nằm trong thư mục con `infini` của từng lớp (xem `SLASH_TO_INFINI_MIGRATION.md`, mục 10). Luồng gọi tạo thẻ:

```text
InfiniCardCreationController   (controller/user/wallet/infini, đường dẫn vẫn là /wallet/...)
    → CardCreationService      (service/wallet/infini)
    → CardCreationServiceImpl  (service/wallet/infini/impl)
    → CardCreationTransactionServiceImpl  (giao dịch tiền, REQUIRES_NEW)
```

- Controller nhận request, lấy user đăng nhập và bọc kết quả bằng `ApiResponse`.
- Các transaction liên quan đến tiền nằm trong `CardCreationTransactionService` và `CardCreationTransactionServiceImpl`. Bean riêng giúp `REQUIRES_NEW` có hiệu lực.
- `CardCreationScheduler` (`scheduler/infini`) gọi interface service để đối soát định kỳ.
- DTO response dùng `@Getter`, `@Builder` và `@JsonProperty` theo cấu trúc response ví hiện có.
- Validation và thông báo nghiệp vụ sử dụng `MessageKeys` cùng các file i18n.

## 8. Phạm vi đã triển khai

Đã thay luồng **tạo thẻ** sang Infini.

Chưa chuyển các chức năng nạp thêm, rút khỏi thẻ, xem thông tin nhạy cảm, khóa/mở khóa và webhook giao dịch sang Infini. Các luồng Slash cũ chặn thẻ Infini trước khi gọi Slash để tránh gửi nhầm ID nhà cung cấp.

Chưa bật phát hành thẻ cho khách trên môi trường thật trước khi hoàn thiện webhook, đối soát giao dịch và các chức năng quản lý cần thiết.

## 9. Tài liệu tham khảo

- [Card API Infini](https://developer.infini.money/docs/en/card-api).
- [Xác thực API Infini](https://developer.infini.money/docs/en/4-authorization).


## 10. Sản phẩm mặc định và mục đích scheduler

Cấu hình trong `application.yml`:

```yaml
infini:
  product-id: 2
```

FE không truyền `productId`; backend lưu sản phẩm đã cấu hình vào từng yêu cầu và gửi giá trị đó sang Infini. Gửi lại cùng requestId sẽ giữ sản phẩm của yêu cầu ban đầu, kể cả khi cấu hình đã thay đổi.

Scheduler kiểm tra trạng thái các yêu cầu đã có ID Infini nhưng chưa hoàn tất, và tra lại thẻ theo `card_alias` cho các lệnh đã gửi mà chưa có ID. Khi xác nhận thẻ hoạt động và dữ liệu khớp, backend mới chốt khoản giữ chờ và phí. Scheduler không gửi lại API tạo thẻ và không trừ phí lần thứ hai.


## 11. Lưu và đọc thẻ Infini riêng

- Entity: `InfiniCard`, bảng `cards_infini`, repository `InfiniCardRepository`.
- Lệnh tạo thẻ chỉ lưu vào bảng mới sau khi Infini xác nhận hoàn tất và chốt tiền thành công.
- API tạo, cấu hình tạo và trạng thái yêu cầu vẫn giữ đường dẫn hiện tại.
- Danh sách: `GET {api.prefix}/infini-cards?page=0`.
- Chi tiết: `GET {api.prefix}/infini-cards/{cardId}`.
- Hai API đọc dùng `InfiniCardController → InfiniCardService → InfiniCardServiceImpl`, chỉ trả thẻ thuộc user đang đăng nhập.
- ID trong response tạo thẻ thuộc `cards_infini`. FE phải phân biệt `provider = INFINI`; không gửi ID này vào các API quản lý thẻ Slash cũ.
- Migration đặt ID mới cao hơn ID cũ tại thời điểm chuyển đổi để giảm nhầm lẫn; đây không thay thế việc tách đường dẫn API.
- Hạn mức mở thẻ tính tổng số thẻ Slash, thẻ Infini và yêu cầu đang chờ.
- API số dư ví cộng số dư thẻ từ cả hai bảng; số thẻ hoạt động trên dashboard ví cũng tính cả hai.
- Các API danh sách/quản lý thẻ Slash cũ vẫn phục vụ Slash. Dashboard/thống kê riêng của thẻ, giao dịch và cashback chưa được chuyển đầy đủ sang Infini.
- Các chức năng nạp/rút, khóa/mở khóa, reveal và webhook giao dịch Infini vẫn cần triển khai tiếp.
