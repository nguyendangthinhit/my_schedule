# My Schedule – Kiến trúc, Database, Bảo mật (mô tả DỰ ÁN CŨ; user đã duyệt 2026-11-20)

Gộp 3 loại document: (1) Kiến trúc tổng thể, (5) Database, (7) Bảo mật và dữ liệu.
Ngày rà soát code: 2026-11-20. Code là source of truth. Mục "chưa có thông tin" chờ user bổ sung.

---

## PHẦN 1. KIẾN TRÚC TỔNG THỂ

### 1.1 Mục tiêu và nguyên tắc
- Ứng dụng Android quản lý lịch trình cá nhân (ngày/tuần/tháng/năm, lịch âm VN, thời tiết, widget) kèm trợ lý AI.
- **Room (SQLite trên máy) là bản làm việc chính.** Widget, chuông báo và chế độ offline đọc từ Room.
- Xóa luôn là xóa mềm (`deleted_at`), mọi bản ghi có `updated_at` và cờ `dirty` để chuẩn bị đồng bộ.
- AI chỉ đề xuất; người dùng xác nhận (thẻ chọn lịch, thẻ xác nhận xóa) rồi app mới ghi/xóa.

### 1.2 Vai trò người dùng
- Hiện tại: **một người dùng duy nhất trên mỗi thiết bị**, không có tài khoản, không phân quyền.
- Dự kiến (chưa code, theo `note.txt` và `my-schedule-sync-app-tasks.md`): agent bên ngoài (ví dụ Telegram) ghi lịch qua server. Chưa có thông tin chi tiết về vai trò này.

### 1.3 Công nghệ
| Mục | Giá trị (bằng chứng) |
|---|---|
| Nền tảng | Android native, Kotlin, Jetpack Compose, Material 3 |
| SDK | minSdk 28, targetSdk 36 (`app/build.gradle.kts`) |
| Package | `com.aistudio.myschedule.xcvtq` |
| Kiến trúc | MVVM + Repository (`viewmodel/`, `data/EventRepository.kt`) |
| Lưu trữ | Room, DB `schedule_database`, version 5 (`AppDatabase.kt`) |
| Mạng | Retrofit: Gemini, Groq, Open-Meteo, Nager.Date |
| Chuông báo | AlarmManager + NotificationManager (`util/NotificationHelper.kt`, `NotificationReceiver.kt`); sự kiện quan trọng: kênh riêng + full-screen intent + `ImportantEventActivity` |

### 1.4 Thành phần và trách nhiệm
| Thành phần | Vị trí | Trách nhiệm |
|---|---|---|
| UI | `ui/screens`, `ui/components`, `ui/navigation` | Màn hình Compose; điều hướng có `welcome`, `main`, `add_event` và các màn con |
| ViewModel | `viewmodel/` (ScheduleViewModel, AIChatViewModel, AIWeeklyReviewViewModel, DashboardViewModel), `viewmodels/EventViewModel.kt` | Trạng thái màn hình, gọi Repository, gọi AI |
| Repository | `data/EventRepository.kt`, `DashboardRepository.kt` | Điểm ghi tập trung (đặt `updated_at`, `dirty`, xóa mềm) |
| DAO/Entity | `data/dao`, `data/entities` | Truy cập Room |
| AI | `network/GeminiApiService.kt`, `GroqApiService.kt`, `util/AiConfigHelper.kt`, `util/AIScheduleContextHelper.kt` | Gọi model, xoay key, failover; dựng ngữ cảnh lịch cho AI |
| Thời tiết / ngày lễ | `network/Weather*`, `CalendarApiService.kt`, `CalendarSyncRepository.kt` | Lấy dữ liệu công khai, cache vào Room |
| Widget | `widget/` (3 provider: Small, Compact, Micro) | Hiển thị lịch ở màn hình chính |
| Phân tích | `analytics/AnalyticsEngine.kt` | Thống kê năng suất |

### 1.5 Cấu trúc trang
Bottom navigation 5 tab: Việc cần làm, Lịch trình, Trợ lý AI, Thống kê, Cài đặt. Chi tiết tính năng từng tab xem `project_state.md` §2 (còn đúng phần tính năng; phần kỹ thuật trong file đó đã cũ).

### 1.6 Dữ liệu thuộc về ai
- Lịch, danh mục, nhắc nhở, lịch sử hoàn thành: **thuộc người dùng, lưu trên thiết bị**.
- Dữ liệu công khai (thời tiết, ngày lễ): cache cục bộ, không thuộc người dùng.
- Server riêng: **chưa có**.

### 1.7 Sơ đồ tổng quan
```
UI (Compose) ──> ViewModel ──> EventRepository ──> Room (schedule_database v5)
     │                │                                   ▲
     │                ├──> AI: Gemini / Groq (HTTPS)       │
     │                └──> Weather / Holidays (HTTPS)      │
     └── Widget, AlarmManager ── đọc ─────────────────────┘
(Chưa có)  Supabase / agent ngoài  <── sync hai chiều (kế hoạch)
```

### 1.8 Hiện trạng so với đích
| Hạng mục | Hiện trạng | Đích |
|---|---|---|
| Schema sẵn sàng sync (UUID, `updated_at`, `deleted_at`, `dirty`) | Đã có (Room v5) | |
| Xóa mềm qua Repository | Đã có (`EventRepository.deleteEventById`) | |
| Remote/Supabase, đăng nhập, SyncEngine | Chưa có (không thấy code) | Sync hai chiều |
| Phiên bản web, agent | Chưa có | Ý tưởng trong `note.txt` |
| Sự kiện quan trọng nổi toàn màn hình | Đã có (2026-11-20, chưa test tay): cờ `is_important`, kênh thông báo `schedule_event_channel_important`, `util/ImportantEventActivity.kt` | |

Truy vấn đọc trong `ScheduleDao.kt` đã lọc `deleted_at IS NULL` (kể cả JOIN với `boards`); chỉ `getEventByIdIncludeDeleted` và một truy vấn board theo id có chủ ý lấy cả bản đã xóa. Chưa kiểm tra các truy vấn ngoài DAO (widget, analytics).

---

## PHẦN 2. DATABASE

### 2.1 Bảng và quan hệ
```
boards 1 ──< events 1 ──< reminders
                      └──< event_completions
holidays, weather_forecasts: độc lập (cache)
```
Khóa ngoại `events.board_id`, `reminders.event_id`, `event_completions.event_id` đều `ON DELETE CASCADE` (vì xóa mềm nên cascade thật hầu như không chạy).

| Bảng | Cột quan trọng | Sync? |
|---|---|---|
| `boards` | `id` (UUID), `name`, `colorHex`, `isVisible`, `sortOrder`, `updatedAt`, `deletedAt`, `dirty` | Có |
| `events` | `id`, `title`, `description`, `board_id`, `start_date` (yyyy-MM-dd), `start_time`/`end_time` (HH:mm), `is_all_day`, `repeat_rule` (NONE/DAILY/WEEKLY/MONTHLY), `repeat_days`, `repeat_end_date`, `is_completed`, `has_reminder`, `is_important` (mặc định 0, v5), `updated_at`, `deleted_at`, `dirty` | Có |
| `reminders` | `id`, `event_id`, `remind_before_minutes`, `is_triggered`, `notification_id`, + 3 cột sync | Có |
| `event_completions` | `id`, `event_id`, `occurrence_date`, `is_completed`, `completed_at`, + 3 cột sync | Có |
| `holidays` | `id` (Int tự tăng), `date`, `name`, `source`, `is_lunar_based`, `last_synced_at` | Không |
| `weather_forecasts` | `date` (khóa), `tempAvg/Min/Max`, `weatherEmoji`, `conditionDescription`, `updatedAt` | Không |

### 2.2 Quy ước
- Khóa chính bảng sync: UUID dạng String. 10 danh mục mặc định có UUID cố định (`data/DefaultBoards.kt`, dạng `00000000-0000-4000-a000-0000000000NN`).
- Thời gian ghi: epoch ms (Long). Ngày: chuỗi ISO. Giờ: "HH:mm".
- Tên cột: `boards` dùng camelCase, các bảng khác snake_case (code hiện tại, không nhất quán).
- **Quyết định user (2026-11-20): chuẩn là snake_case.** Áp dụng cho dự án mới; code cũ chưa đổi.

### 2.3 Migration
- Khai báo trong `AppDatabase.kt`: `MIGRATION_2_3`, `MIGRATION_3_4` (số → UUID, thêm cột sync), `MIGRATION_4_5` (thêm `events.is_important`). Schema xuất ra `app/schemas/.../5.json` (`exportSchema = true`).
- **Rủi ro:** `fallbackToDestructiveMigration(true)` (`AppDatabase.kt:280`): nếu thiếu migration, Room xóa toàn bộ dữ liệu thay vì báo lỗi.
- Migration test: có (`app/src/test/java/com/example/data/Migration3To4Test.kt`, `SoftDeleteAndRepositoryTest.kt`).

### 2.4 Bảng/cột chưa dùng
Chưa rà soát. Chưa có thông tin.

---

## PHẦN 3. BẢO MẬT VÀ DỮ LIỆU

### 3.1 Ranh giới tin cậy
- Tin cậy: tiến trình app và Room trong bộ nhớ riêng của app.
- Không tin cậy: phản hồi của AI (chỉ là đề xuất, có bước xác nhận), phản hồi API thời tiết/ngày lễ.

### 3.2 Dữ liệu nhạy cảm và nơi lưu
| Dữ liệu | Nơi lưu | Ghi chú |
|---|---|---|
| Lịch, ghi chú, địa điểm | Room, bộ nhớ riêng của app | Không mã hóa |
| API key Gemini/Groq (người dùng nhập) | `SharedPreferences` dạng thô (`AiConfigHelper.kt`) | Chưa dùng Keystore/EncryptedSharedPreferences |
| Vị trí GPS | Dùng để lấy thời tiết (quyền FINE/COARSE) | Chưa rà soát nơi gửi tọa độ |

### 3.3 Dữ liệu rời khỏi thiết bị
- Khi hỏi AI: nội dung chat và một phần lịch (từ 14 ngày trước trở đi, tối đa 200 mục, bỏ Ngày lễ) gửi tới Google Gemini hoặc Groq.
- Gemini: API key gửi dạng query param `key=` (`GeminiApiService.kt:60`), có thể lọt vào log proxy.
- Thời tiết (Open-Meteo), ngày lễ (date.nager.at): gửi request công khai.
- Câu "không gửi dữ liệu ra bên thứ ba" trong `project_state.md` §4.2 chỉ đúng khi không dùng AI; sẽ sai hoàn toàn khi có Supabase.

### 3.4 Mối đe dọa và nơi xử lý
| Mối đe dọa | Xử lý hiện tại |
|---|---|
| AI xóa/ghi sai | Thẻ xác nhận trước khi xóa/thêm (`AIDeletionConfirmCard`) |
| Lộ API key | Chưa có biện pháp mã hóa; nhắc người dùng tự tạo lại key khi lộ |
| Log lộ nội dung lịch/key | `AIChatViewModel.kt:275,452` log tiền tố key (4 ký tự) và `finishReason` (log `RAW reply` đã bỏ); `GroqApiService.kt` bật `HttpLoggingInterceptor` mức BODY (log cả nội dung và header Authorization) |
| Backup đám mây | `allowBackup="true"`; cấu hình tại `xml/backup_rules`, `xml/data_extraction_rules` (chưa đọc) |
| Bản release | `isMinifyEnabled = false`; chưa có cấu hình release riêng |

### 3.5 Phân quyền
Không có (một người dùng). Quyền Android: INTERNET, ACCESS_NETWORK_STATE, FINE/COARSE_LOCATION, POST_NOTIFICATIONS, VIBRATE, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, USE_FULL_SCREEN_INTENT (Android 14+ người dùng có thể phải cấp trong Cài đặt).

### 3.6 Lưu trữ, xóa, sao lưu
- Xóa mềm, chưa có job dọn bản ghi đã xóa (hoãn theo tasks.md Bước 2.4).
- Sao lưu: chưa có chính sách riêng, phụ thuộc Android Auto Backup.

### 3.7 Nơi giữ secret
- Hiện: key AI trong SharedPreferences; `.env`/`local.properties` cho cấu hình build (README nhắc `GEMINI_API_KEY`, chưa xác minh có dùng trong code).
- Chưa có thông tin: chính sách quyền riêng tư gửi người dùng.

---

## Câu hỏi mở cho user
1. Có chấp nhận ghi "một người dùng/thiết bị" ở §1.2 không? Vai trò agent/web mô tả thêm gì?
2. Chốt quy ước tên cột (camelCase của `boards` hay snake_case)?
3. Có muốn bỏ `fallbackToDestructiveMigration` sau này không (ghi vào DECISIONS khi chốt)?
