# Mô hình dữ liệu lịch

Trạng thái: đã duyệt 2026-11-20 (user đồng ý cả bốn đề xuất: cột giữ lại, tách hai cửa, hai cột chuông báo, UUID danh mục). Doc là chuẩn, code Room v5 sửa theo doc (mục 3.6). Còn mở: múi giờ, cảnh báo trùng, `user_id`. Phần "đề xuất" chưa được user chốt.
Nguồn: mã Room v5 và `my_schedule/docs/architecture.md` mục 2 (git pull 2026-11-20, commit 05d0ae1), `my_schedule/my-schedule-sync-app-tasks.md` (cột thêm cho đồng bộ), thiết kế gốc mục 5, D-06, D-08, D-09.
Mục 3.6 đã đối chiếu với entity Room v5 (đọc 2026-11-20). Bản `my_schedule/` ở ổ D chỉ để đọc (D-19); thay đổi chưa push ở ổ C thì chưa thấy.

## 1. Nguyên tắc

- Một nguồn sự thật: Supabase (Postgres), chỉ agent lịch truy cập (D-06, D-08).
- Room trên điện thoại là bản sao làm việc; widget, chuông báo, chế độ offline đọc từ Room.
- Bốn bảng đồng bộ: `events`, `boards`, `reminders`, `event_completions`. `holidays` và `weather_forecasts` sinh lại được nên không đồng bộ.
- Xoá luôn là xoá mềm để thiết bị khác biết bản ghi đã bị xoá.

## 2. Cột chung cho bảng đồng bộ

| Cột | Có ở | Ý nghĩa |
|---|---|---|
| `id` | Room, Supabase | UUID sinh phía client (Room v5 đã dùng UUID). Hai nơi cùng tạo bản ghi không đụng khoá |
| `updated_at` | Room, Supabase | Epoch ms UTC, đặt lại mỗi lần ghi; dùng giải xung đột |
| `deleted_at` | Room, Supabase | Null nếu chưa xoá; có giá trị là xoá mềm |
| `server_updated_at` | Supabase | Do server đặt bằng `now()` mỗi lần insert/update; làm con trỏ kéo về, không dùng đồng hồ điện thoại |
| `dirty` | Chỉ Room | True nếu có thay đổi chưa đẩy lên; không có trên server |
| `user_id` | Supabase | Chủ bản ghi; giữ để mở đường cho RLS về sau (D-08 hoãn RLS) |

Doc chọn `deleted_at` (mốc thời gian, null nếu chưa xoá), không dùng cờ `isDeleted`. Room v5 cũng dùng `deleted_at`.

## 3. Bảng (thiết kế đích)

Doc này là chuẩn; code app viết lại theo doc (user chốt 2026-11-20). Tên cột là snake_case. Khác biệt với Room v5 hiện tại ở mục 3.6.

### 3.1 `events`

| Cột | Nguồn | Ghi chú |
|---|---|---|
| `id`, `title`, `date`, `start_time`, `end_time`, `location`, `reminder_note`, `board_id`, `is_completed` | Thiết kế đích | `date` dạng `yyyy-MM-dd`; giờ dạng `HH:mm` |
| `description` (text, null), `is_all_day` (bool), `is_important` (bool, mặc định false), `created_at` (epoch ms) | Giữ từ app (user chốt 2026-11-20) | `is_important`: thông báo toàn màn hình |
| `source` | Thêm (D-09) | Nguồn: `app`, `gmail`, `slack`... Tách sự kiện tự động khỏi nhập tay |
| `external_id` | Thêm (D-09) | Mã ở nguồn ngoài; cặp (`source`, `external_id`) unique để chạy lại không tạo trùng |
| Cột chung | Mục 2 | |

Giai đoạn đầu chưa có sự kiện lặp: không có `repeat_rule`, `repeat_days`, `repeat_end_date`; cũng chưa có `completed_at` và `has_reminder` ở `events` (nhắc nhở nằm ở bảng `reminders`). Thêm sau khi cần và ghi `DECISIONS.md`.

Múi giờ: chưa chốt. Thiết kế gốc nêu hai múi giờ (Việt Nam, Pháp) cho phần trích giờ, nên cần chốt một quy ước trước khi viết `calendar-ingestion.md`.

### 3.2 `boards`

Cột: `id`, `name`, `color_hex`, `is_visible`, `sort_order`, cộng cột chung.

Có 10 danh mục mặc định: Công việc, Học tập, Đi chơi, Cuộc họp, Ngày lễ, Cá nhân, Sức khỏe, Tài chính, Việc cần làm, Gia đình. Mỗi danh mục phải có UUID cố định để mọi thiết bị ra cùng id; nếu không, cài lại app sẽ nhân đôi danh mục trên server. App hiện đã có bộ UUID cố định (`DefaultBoards.kt`, `00000000-0000-4000-a000-0000000000NN`); đích giữ nguyên bộ này (user chốt 2026-11-20); Supabase dùng đúng các UUID đó.

### 3.3 `reminders`

Mốc nhắc trước sự kiện: `id`, `event_id` (khoá ngoại), `remind_before_minutes`, cộng cột chung. Hai cột `is_triggered`, `notification_id` là trạng thái chuông báo riêng từng thiết bị: không đưa vào đích, không đồng bộ, chỉ giữ cục bộ trong app (user chốt 2026-11-20).

### 3.4 `event_completions`

Lịch sử hoàn thành: `id`, `event_id` (khoá ngoại), `occurrence_date` (`yyyy-MM-dd`, ngày hoàn thành), `completed_at` (epoch ms), cộng cột chung. Mốc là ngày vì giai đoạn đầu chưa có sự kiện lặp (user chốt 2026-11-20).

### 3.5 Không đồng bộ

`holidays`, `weather_forecasts`: cache cục bộ trên điện thoại.

### 3.6 Khác biệt: Room v5 hiện tại so với đích (việc cho lần viết lại code)

Nguồn: entity Room v5 trong `my_schedule/` (đọc 2026-11-20). Code sai doc thì sửa code, không sửa doc.

| Hạng mục | Đích (doc này) | Room v5 hiện tại | Việc |
|---|---|---|---|
| Ngày sự kiện | `date` | `start_date` | Đổi tên |
| Địa điểm | `location` | Không có cột | Thêm cột |
| Ghi chú nhắc | `reminder_note` | Không có cột (có `description`, và bảng `reminders`) | Thêm cột; quyết định `description` giữ hay gộp |
| `source`, `external_id` | Có | Không có | Thêm cột |
| `user_id`, `server_updated_at` | Có (mục 2) | Không có | Thêm khi làm sync |
| `boards` | snake_case | camelCase (`colorHex`...) | Đổi tên |
| Xoá mềm, UUID, `updated_at`, `dirty` | Có | Đã có | Không |
| Cột app có, giữ trong đích | Có | `description`, `is_all_day`, `is_important`, `created_at` | Không |
| Cột app có, hoãn khỏi đích | Không (giai đoạn đầu) | `repeat_rule`, `repeat_days`, `repeat_end_date`, `completed_at`, `has_reminder` ở `events`; `is_triggered`, `notification_id` ở `reminders` | Giữ cục bộ hoặc bỏ khi viết lại |

## 4. Quan hệ

```
boards 1 ── n events 1 ── n reminders
                    └──── n event_completions
```

Thứ tự đồng bộ: `boards` → `events` → `reminders` → `event_completions` (cha trước, con sau).

## 5. Quy tắc dữ liệu

| Quy tắc | Chi tiết |
|---|---|
| Xung đột | Bản có `updated_at` mới hơn thắng; ghi log xung đột |
| Đẩy lên | Chạy lại nhiều lần vẫn an toàn (khoá là UUID, upsert) |
| Kéo về | Lấy dòng có `server_updated_at` lớn hơn con trỏ của bảng, sắp tăng dần, phân trang |
| Truy vấn đọc | Mặc định lọc `deleted_at IS NULL` |
| Trùng lịch | Tạo mới và gắn cảnh báo, không từ chối, không xoá/dời bản có sẵn |
| Chống tạo trùng từ nguồn ngoài | Unique (`source`, `external_id`) |

Cách thể hiện "cảnh báo trùng" trong dữ liệu (cột riêng hay tính khi truy vấn) chưa chốt; đề xuất tính khi truy vấn để không thêm cột.

## 6. Hai cửa vào agent lịch

Luật "không xoá/dời" của agent lịch áp cho công cụ MCP mà các agent gọi. Người dùng vẫn phải sửa, dời, xoá sự kiện của chính mình trong app. Vì vậy tách hai cửa (user xác nhận 2026-11-20):

| Cửa | Ai dùng | Quyền |
|---|---|---|
| API đồng bộ | my_schedule | Đẩy/kéo đầy đủ, gồm sửa và xoá mềm do người dùng thực hiện |
| Công cụ MCP | Agent chính (và sau này Calendar_Asisstant) | Không có công cụ xoá hay dời; chi tiết ở `calendar-agent-mcp-tools.md` |

Nếu không tách, luật "không xoá/dời" sẽ mâu thuẫn với việc người dùng xoá sự kiện trong app.

## 7. Việc còn mở

- [x] Đọc mã Room, lập bảng khác biệt (mục 3.6) (2026-11-20).
- [x] `deleted_at` (mốc thời gian), theo mã Room (2026-11-20).
- [x] Cột giữ/hoãn khỏi đích: giữ `description`, `is_all_day`, `is_important`, `created_at`; hoãn sự kiện lặp và các cột liên quan (2026-11-20).
- [x] `is_triggered`, `notification_id`: không đưa vào đích (2026-11-20).
- [x] Giữ bộ UUID cố định của 10 danh mục (2026-11-20).
- [ ] Doc 12 chưa có `description`, `is_all_day`, `is_important` trong `create_event`/`list_events` (và `event_completions` dùng `occurrence_date`): đối chiếu khi viết mã.
- [ ] Quy ước múi giờ.
- [ ] Cách lưu cảnh báo trùng.
- [x] Tách hai cửa (mục 6) (2026-11-20).
- [ ] Giữ `user_id` ngay từ đầu hay thêm khi cần RLS.
- [ ] Sao lưu `schedule_database` và việc bỏ `fallbackToDestructiveMigration` (thuộc `calendar-sync-my-schedule.md`). Migration v3→v4→v5 đã có trong app.
