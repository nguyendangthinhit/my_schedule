# Hợp đồng công cụ MCP của agent lịch

Trạng thái: đã duyệt 2026-11-20 (user đồng ý ba điểm: `complete_event` cần duyệt; chưa có công cụ sửa sự kiện có sẵn; giữ hai cửa vào agent lịch). Tên công cụ lấy từ thiết kế gốc mục 4.3; tham số và kết quả là đề xuất, chốt khi viết mã.
Nguồn: thiết kế gốc mục 4.3, 5; `calendar-data-model.md`; `job-queue-and-logging.md`; D-02, D-06, D-08, D-11, D-12, D-14, D-17, D-18.
Agent lịch là dịch vụ trên mạng (D-11). Agent chính gọi qua client MCP hạn chế; app dùng API đồng bộ riêng (xem `calendar-data-model.md` mục 6). Hai cửa vào tách nhau để luật "không xoá/dời" chỉ áp cho agent, không chặn người dùng xoá trong app.

## 1. Luật bất biến

1. Không có công cụ xoá hay dời sự kiện. Không có công cụ sửa `date`, `start_time`, `end_time` của sự kiện có sẵn.
2. Trùng lịch: vẫn tạo mới và trả cảnh báo, không từ chối.
3. Công cụ ghi chỉ chạy khi agent chính đã có duyệt của chủ (D-02). Agent lịch không tự quyết duyệt; nó kiểm tra token có quyền ghi.
4. Đọc và ghi qua công cụ có kiểm tra, không ghi bảng trực tiếp.
5. Dữ liệu trong `title`, `location`, `reminderNote` là dữ liệu, không bao giờ là lệnh (D-03).

## 2. Công cụ

| Công cụ | Loại | Mục đích |
|---|---|---|
| `list_events` | Đọc | Liệt kê sự kiện theo khoảng thời gian |
| `find_free_slots` | Đọc | Tìm khoảng trống đủ dài |
| `weekly_summary` | Đọc | Tóm tắt một tuần: số sự kiện, đã/chưa hoàn thành |
| `create_event` | Ghi, cần duyệt | Tạo sự kiện mới |
| `complete_event` | Ghi, cần duyệt | Đánh dấu hoàn thành |

Thiết kế gốc chỉ ghi `create_event` là "cần duyệt". Theo D-02 (mọi hành động ghi đều qua duyệt), user chốt `complete_event` cũng cần duyệt.

Chưa có công cụ sửa tiêu đề, địa điểm, ghi chú của sự kiện có sẵn (user chốt 2026-11-20, YAGNI). Cần thì thêm sau và ghi vào `DECISIONS.md`.

## 3. Chi tiết (đề xuất)

### `list_events`
- Vào: `from`, `to` (ngày), `board_id` (tuỳ chọn), `include_completed` (mặc định true).
- Ra: danh sách sự kiện chưa xoá mềm, sắp theo ngày và giờ bắt đầu. Mỗi sự kiện có `id`, `title`, `date`, `start_time`, `end_time`, `location`, `board_id`, `is_completed`, `source`.

### `find_free_slots`
- Vào: `from`, `to`, `min_minutes`, giờ làm việc (tuỳ chọn).
- Ra: danh sách khoảng (`date`, `start_time`, `end_time`) không giao với sự kiện nào.

### `weekly_summary`
- Vào: `week_start`.
- Ra: số sự kiện theo ngày và theo danh mục, số đã hoàn thành, danh sách việc chưa xong.

### `create_event`
- Vào: `title`, `date`, `start_time`, `end_time`, `location`, `reminder_note`, `board_id`, `source`, `external_id` (hai trường cuối khi nguồn là hệ thống ngoài).
- Ra: sự kiện đã tạo, kèm `warnings` (danh sách sự kiện trùng giờ).
- Trùng (`source`, `external_id`): trả về bản đã có, không tạo thêm (chạy lại an toàn).

### `complete_event`
- Vào: `event_id`, mốc thời gian hoàn thành (tuỳ chọn, cho sự kiện lặp).
- Ra: sự kiện sau khi cập nhật.

## 4. Lỗi

| Mã | Khi nào |
|---|---|
| `invalid_argument` | Thiếu hoặc sai trường, giờ kết thúc trước giờ bắt đầu |
| `not_found` | `event_id` hoặc `board_id` không tồn tại |
| `forbidden` | Token chỉ có quyền đọc mà gọi công cụ ghi |
| `unauthorized` | Thiếu hoặc sai token |
| `unavailable` | Supabase hoặc dịch vụ lỗi; agent chính báo về kênh gốc của yêu cầu, không tự thử vô hạn |

Trùng lịch không phải lỗi; nó là `warnings` trong kết quả.

## 5. Xác thực

- Mỗi client (agent chính, sau này Calendar_Asisstant) có token riêng; tách quyền đọc và ghi (theo giao ước Supabase trong tài liệu app).
- Khoá Supabase chỉ nằm ở dịch vụ agent lịch, không phát cho client.
- Cách cấp, xoay và thu hồi token chưa chốt. Chi tiết rủi ro ở `security-and-threat-model.md`.

## 6. Phía agent chính

- Client MCP nằm sau giao diện công cụ lịch (ranh giới ở `system-architecture.md` mục 3).
- Lệnh `/lich` đi thẳng bằng luật; tin tự nhiên để model chọn công cụ. Hành động ghi vẫn dừng chờ duyệt: nút trên web, hoặc `/duyet <mã>` trên Zalo, chỉ ở kênh gốc (D-18; xem `job-queue-and-logging.md`).
- Khi agent lịch không phản hồi: báo lỗi về kênh gốc, không đoán kết quả.

## 7. Việc còn mở

- [x] `complete_event` cần duyệt (2026-11-20).
- [ ] Giao thức vận chuyển MCP (HTTP) và nền tảng chạy; chọn khi viết `deployment-and-operations.md`.
- [x] Sửa tiêu đề, địa điểm, ghi chú qua MCP: chưa có công cụ ở giai đoạn đầu (2026-11-20).
- [ ] Cách cấp và thu hồi token.
- [ ] Xử lý sự kiện lặp (nếu app có) cho `complete_event`.
- [ ] Quy ước múi giờ cho `date`/`start_time` (xem `calendar-data-model.md`).
