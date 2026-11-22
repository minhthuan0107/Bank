# Kế hoạch chuyển Slash sang Infini

Tài liệu tổng hợp cho dự án Bank, dựa trên code đã đọc và tài liệu Infini đã đối chiếu ngày 09/11/2026. Đây là kế hoạch triển khai; chưa thay đổi nghiệp vụ hoặc tích hợp production.

## 1. Phạm vi

Chuyển phần tích hợp thẻ từ Slash sang Infini: phát hành, thông tin thẻ, nạp/rút tiền thẻ, khóa/mở khóa, giao dịch, webhook và đồng bộ số dư.

Luồng khách nạp USDT vào ví do hệ thống quản lý rồi admin ghi có ví nội bộ vẫn có thể giữ. Rút khỏi ví website là nghiệp vụ riêng với rút tiền khỏi thẻ.

Khả năng cấp thẻ cho khách hàng website cần được Infini xác nhận theo tài khoản/hợp đồng được cấp. Tài liệu công khai mô tả Organization Card cho thành viên tổ chức; không mặc định mọi tài khoản cá nhân đều dùng được mô hình này.

## 2. API thẻ tương ứng

Prefix: `/v2/cards`.

| Chức năng | Slash hiện tại | API Infini |
| --- | --- | --- |
| Phát hành | createCard | POST /apply |
| Thông tin/trạng thái | getCard | GET /status?id=... |
| Nạp thẻ | setLimit tăng | POST /top-up |
| Rút khỏi thẻ | setLimit giảm | POST /redeem |
| Khóa | lockCard | POST /freeze |
| Mở khóa | unblockCard | POST /unfreeze |
| PAN/CVV/hạn thẻ | getCardSensitiveDetail | POST /reveal |
| Lịch sử | Luồng giao dịch Slash | GET /transactions?id=... |
| Danh sách | Hỗ trợ đối soát | GET /list |
| Trạng thái hàng loạt | Hỗ trợ đồng bộ | POST /status/batch |
| Đóng thẻ | Bổ sung | POST /delete |

Trường chính:

- apply: product_id, top_up_amount, token_type, user_email, holder_name.
- top-up/redeem: id, amount, token_type; note tùy chọn.
- freeze/unfreeze/reveal: id.
- delete: card_id.
- status/batch: card_ids.

Số tiền gửi dạng chuỗi thập phân. Kiểm tra cả HTTP status và code nghiệp vụ. Phát hành/đóng thẻ cần theo dõi trạng thái hoàn tất.

Nguồn: [Card API](https://developer.infini.money/docs/en/card-api).

## 3. Luồng tiền

Có ba nơi cần phân biệt:

1. Ví USDT bên ngoài do bạn quản lý: giữ tiền blockchain khách đã gửi.
2. Số dư Business bên Infini: nguồn phục vụ thẻ.
3. Database Bank: sổ theo dõi quyền lợi và số dư từng khách.

Tài khoản Business và số dư từng thẻ được quản lý riêng. Infini mô tả chuyển tiền từ tài khoản chính vào thẻ; redeem đưa tiền thẻ về tài khoản chính.

Nguồn: [Corporate Cards](https://infini.money/products/corporate-cards/virtual-cards), [Spend Controls](https://infini.money/products/corporate-cards/spend-limits).

Tiền còn ở ví blockchain bên ngoài không tự được API top-up lấy sang. Cần cấp nguồn bên Infini trước. Địa chỉ nạp, mạng, phí, quy đổi và tài khoản nguồn cụ thể phải xác nhận cho tài khoản tích hợp được cấp. Không mặc định 1 USDT luôn bằng đúng 1 USD sau phí/quy đổi.

### Nạp thẻ — luồng đề xuất

1. Kiểm tra chủ thẻ, trạng thái, số tiền và số dư ví nội bộ.
2. Lưu lệnh và giữ chờ tiền trong một transaction ngắn.
3. Gọi Infini top-up ngoài transaction khóa dữ liệu kéo dài.
4. Xác nhận thành công: chốt chuyển tiền nội bộ từ ví sang khoản phân bổ cho thẻ.
5. Thất bại chắc chắn: giải phóng khoản giữ chờ.
6. Timeout/chưa rõ kết quả: giữ lệnh chờ, đối soát trước khi cho thử lại.

### Rút khỏi thẻ — luồng đề xuất

1. Tạo lệnh rút gắn đúng chủ thẻ.
2. Gọi redeem.
3. Khi xác nhận thành công, ghi giảm khoản phân bổ trên thẻ và cộng ví nội bộ của khách đúng một lần.
4. Đối soát với tiền về tài khoản Business bên Infini.

Ví dụ không phí/quy đổi: khách có 100 USD trong ví web, nạp 30 thì ví còn 70 và thẻ có 30. Rút 20 từ thẻ thì ví thành 90 và thẻ còn 10. Chuyển giữa ví và thẻ không tự làm tăng tổng tài sản.

API response và webhook có thể cùng phản ánh một thao tác. Không cộng/trừ tiền hai lần. Chưa xác nhận được cơ chế idempotency của Card API thì không tự gán request_id hoặc retry lệnh thay đổi tiền như thể Infini có hỗ trợ.

## 4. Webhook và đối soát

Endpoint backend đề xuất: `POST {api.prefix}/webhook/infini`. Đây là URL của Bank, cần đăng ký với Infini.

| Sự kiện | Mục đích |
| --- | --- |
| card.status_change | Đồng bộ vòng đời thẻ |
| card.transaction | Giao dịch và biến động tiền |
| card.challenge | Mã xác thực thanh toán |

Webhook dùng HMAC-SHA256 trên raw body. Chống trùng theo X-Webhook-Event-Id, liên kết cập nhật theo data.transaction_id. Một giao dịch có thể phát nhiều sự kiện; authorization và settlement không được trừ tiền toàn phần hai lần. Cần xử lý phí, chênh lệch quyết toán, hoàn tiền, reversal và sự kiện sai thứ tự.

Nguồn: [Webhook](https://developer.infini.money/docs/en/7-webhook).

Thiết kế đề xuất cho Bank:

- Xác minh chữ ký và kiểm tra timestamp trước khi nhận sự kiện.
- Lưu sự kiện bền vững với khóa duy nhất rồi xử lý, có retry và đối soát.
- Lưu liên kết provider, thẻ, giao dịch, sự kiện và trạng thái áp dụng tiền.
- Tách giao dịch nạp/rút khỏi giao dịch chi tiêu để dashboard/cashback không tính nhầm.
- Không coi polling danh sách là tương đương hoàn toàn với GET giao dịch theo ID của Slash. Tài liệu Card API đã đọc chưa có endpoint chi tiết tương đương getTransaction(entityId).
- Chốt quy tắc nguồn dữ liệu cho số dư; tránh ghi đè bằng snapshot cũ rồi áp dụng tiếp webhook khiến sai lệch.

## 5. Xác thực

Hướng dẫn Authentication hiện mô tả Ed25519 v2 với X-API-Key-ID, X-API-Timestamp, X-API-Nonce, Digest và Authorization. Quyền card.create/card.reveal cần IP allowlist.

Một số trang API Reference còn ghi HMAC. Cần kiểm tra với Sandbox và credential thực tế trước khi chốt implementation. Phân biệt chữ ký API với HMAC webhook. Private key chỉ lưu phía backend; không đưa vào repository, FE hoặc log.

Nguồn: [Authentication](https://developer.infini.money/docs/en/4-authorization).

## 6. Các thành phần cần sửa trong Bank

Đường dẫn service bên dưới tương đối với `src/main/java/com/example/bank/`.

| Thành phần | Công việc |
| --- | --- |
| common/config/slash/SlashClient.java | Tạo interface CardProvider và adapter Infini |
| common/config/properties/SlashProperties.java | Cấu hình Infini, signing và môi trường |
| service/wallet/user/impl/CardServiceImpl.java | Phát hành có trạng thái chờ, ánh xạ sản phẩm, khóa/mở khóa |
| service/wallet/user/impl/CardFundingServiceImpl.java | Thay setLimit bằng top-up/redeem, quản lý kết quả chưa rõ |
| service/wallet/user/impl/CardSensitiveDetailServiceImpl.java | Ánh xạ reveal vào DTO nội bộ |
| service/sync/CardSyncService.java | Đồng bộ dữ liệu Infini |
| controller/user/wallet/SlashWebhookController.java | Bổ sung endpoint Infini |
| service/wallet/user/impl/SlashWebhookServiceImpl.java | Tách adapter xử lý webhook Infini |
| service/wallet/user/impl/CardTransactionPersistenceService.java | Ánh xạ giao dịch và hạch toán |
| service/wallet/admin/impl/CardAdminServiceImpl.java | Đổi lời gọi nhà cung cấp |
| service/wallet/admin/impl/AdminUserCardSingleStatusServiceImpl.java | Đổi lời gọi nhà cung cấp và kiểm tra phân loại tiền khóa |
| Entity, repository, DTO, migration | Bổ sung provider, ID và trạng thái cần thiết |
| Dashboard, cashback, listener | Kiểm tra tác động giao dịch/phí/refund |

Các trường slashCardId, slashTransactionId và bảng sự kiện Slash đang gắn chặt nhà cung cấp. Nên bổ sung provider/providerCardId/providerTransactionId theo migration có bảo toàn dữ liệu, giữ ID nội bộ ổn định. Thẻ Slash cũ không tự biến thành thẻ Infini.

Code khóa thẻ hiện chuyển allocatedBalance sang frozenBalance và đặt remainingAmount về 0. Khi tích hợp Infini phải phân biệt số dư thực tế, số dư có thể chi tiêu và phân loại nội bộ; khóa thẻ không được hạch toán thành redeem.

## 7. Response trả FE

Ưu tiên giữ hợp đồng API và DTO nội bộ. Backend ánh xạ dữ liệu Infini trước khi trả FE, không trả nguyên response nhà cung cấp.

| Chức năng | Hướng xử lý |
| --- | --- |
| Danh sách, số dư, lịch sử | Cố gắng giữ DTO; kiểm tra ý nghĩa trường |
| PAN/CVV/hạn thẻ | Ánh xạ DTO hiện có, không log dữ liệu nhạy cảm |
| Khóa/mở khóa | Giữ response nếu hành vi tương đương |
| Tạo thẻ | FE cần biểu diễn trạng thái đang phát hành |
| Nạp/rút | FE cần biểu diễn đang xử lý/chưa xác định kết quả |
| Challenge | Thêm luồng hiển thị/gửi mã nếu triển khai |

Giữ cấu trúc JSON không đồng nghĩa giữ nguyên hành vi đồng bộ. Không báo thành công cuối cùng khi chưa xác nhận. Khi cần, bổ sung trạng thái theo cách có tương thích và kiểm tra cách FE xử lý enum hiện tại.

Webhook chỉ cập nhật backend; màn hình FE muốn tự cập nhật cần polling, SSE hoặc WebSocket riêng.

## 8. WithdrawAdminServiceImpl và Fund API

`service/wallet/admin/impl/WithdrawAdminServiceImpl.java` hiện duyệt rút khỏi ví và không gọi Slash. Giữ chuyển tiền thủ công thì phần này ít bị ảnh hưởng.

Nếu bổ sung tự động chuyển từ Infini ra ví blockchain của khách, cân nhắc:

| Mục đích | Endpoint |
| --- | --- |
| Nguồn tiền | GET /v2/funds/balances |
| Phí | GET /v2/funds/withdraw/fees |
| Tạo lệnh rút | POST /v2/funds/withdraw |
| Theo dõi | GET /v2/funds/withdraw/status |

Dùng một request_id cho một lần rút logic; không đánh dấu SUCCESS chỉ vì admin duyệt hoặc API tiếp nhận. Cần trạng thái chuyển tiền và đối soát. Không hoàn tiền nội bộ khi timeout mà chưa biết giao dịch bên ngoài đã thành công hay chưa.

Nguồn: [Fund API](https://developer.infini.money/docs/en/fund-api).

## 9. Thứ tự triển khai và kiểm tra

1. Xác nhận tài khoản tổ chức, quyền cấp thẻ cho khách, nguồn tiền, phí và thông tin Sandbox.
2. Tạo CardProvider/Infini adapter và kiểm tra chữ ký API.
3. Migration dữ liệu nhà cung cấp và các trạng thái chờ.
4. Phát hành, đồng bộ, reveal, khóa/mở khóa.
5. Nạp/rút thẻ với giữ chờ và đối soát.
6. Webhook, chống trùng, retry và đồng bộ số dư.
7. Kiểm tra hợp đồng FE, dashboard và cashback.
8. Chạy Sandbox trước khi lập kế hoạch chuyển production.

Các tình huống cần kiểm tra: thiếu nguồn tiền; phát hành thất bại; timeout sau khi phía Infini đã xử lý; request đồng thời; webhook trùng/sai chữ ký/sai thứ tự; authorization sang settlement; quyết toán khác số tiền giữ; refund/reversal; nạp/rút vừa có response vừa có webhook; khóa/mở khóa và bảo toàn tổng tài sản.

## 10. Cấu trúc thư mục Infini / Slash

Code của mỗi nhà cung cấp nằm trong thư mục con `infini` hoặc `slash` **bên trong `wallet`** ở từng lớp. Đường dẫn API không đổi.

| Lớp | Infini | Slash |
| --- | --- | --- |
| Cấu hình, client | `common/config/infini/` | `common/config/slash/` |
| Controller | `controller/user/wallet/infini/` | `controller/user/wallet/slash/` |
| DTO | `dto/request\|response/wallet/infini/` | `dto/request\|response/wallet/slash/` |
| Entity | `entity/wallet/infini/` | `entity/wallet/slash/` |
| Repository | `repository/wallet/infini/` | `repository/wallet/slash/` |
| Service | `service/wallet/infini/` (+ `impl/`) | `service/wallet/slash/` (+ `impl/`) |
| Scheduler, event, listener | `scheduler/infini/` | `event/slash/`, `listener/slash/` |
| Test | `src/test/.../service/wallet/infini/` | (không có) |

Phần dùng chung vẫn ở chỗ cũ: `WalletController` (số dư, dashboard ví), `WalletServiceImpl`, giao dịch thẻ (`CardTransaction*`), cashback, dashboard và các service admin.

### Khi xóa Slash

1. Xóa toàn bộ các thư mục `slash` ở bảng trên.
2. Sửa các file dùng chung còn phụ thuộc Slash (biên dịch sẽ báo lỗi đúng các chỗ này):
   - `controller/user/wallet/CardTransactionController.java`: lịch sử nạp/rút thẻ Slash (`/card-transactions/search`).
   - `service/wallet/admin/impl/CardAdminServiceImpl.java`, `AdminUserCardSingleStatusServiceImpl.java`, `AdminUserCardStatusSyncServiceImpl.java`: quản lý thẻ phía admin, cần viết lại cho Infini.
   - `service/wallet/user/impl/WalletServiceImpl.java`: bỏ phần cộng số dư và đếm thẻ Slash.
   - `service/wallet/infini/impl/CardCreationTransactionServiceImpl.java`: bỏ thẻ Slash khỏi hạn mức mở thẻ.
   - Giao dịch thẻ, cashback, dashboard đang đọc dữ liệu Slash (`CardTransaction`, cột `slash_transaction_id`): chuyển sang giao dịch Infini.
3. Bỏ `slash.*` trong `application.yml`, route `/webhook/slash` trong `SecurityConfig`, và các message key `SLASH_*`.
4. Bảng `cards`, `card_bins`, `card_holders`, `card_funding_transactions`, `slash_webhook_events` chỉ xóa bằng migration mới sau khi đã sao lưu và không còn cần dữ liệu lịch sử.

