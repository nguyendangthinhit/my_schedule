# ISSUES

Persistent project-level problems. Task progress lives in TASK_STATE.md.

## Open

### Lệch tài liệu và code: `project_state.md` (2026-11-20)
- `project_state.md` (gốc repo, nay ở `docs/archive/project-state-old.md`) ghi Room v3, Groq Llama 3.3 70B/Mixtral, "bảo mật tuyệt đối trên thiết bị"; code: `AppDatabase.kt:20` v4, `GroqApiService.kt:55-56` GPT-OSS.
- **Đã xử lý 2026-11-20:** `project_state.md` → `docs/archive/project-state-old.md`, `TEMP_AI_FIX_NOTES.md` → `docs/archive/`. Còn lại: `README.md` vẫn là mẫu AI Studio; `ADB_DEPLOY_NOTES.md`, `AI_CHAT_TEST_NOTES.md` còn ở gốc repo.

### Dữ liệu có thể bị xóa sạch khi migration lỗi (2026-11-20)
- `app/src/main/java/com/example/data/AppDatabase.kt:280` `fallbackToDestructiveMigration(true)`.

### Bảo mật key và log (2026-11-20)
- Key Gemini/Groq lưu SharedPreferences dạng thô (`util/AiConfigHelper.kt`); Gemini gửi key qua query `key=` (`network/GeminiApiService.kt:60`).
- `HttpLoggingInterceptor` mức BODY ở `GroqApiService.kt` (log nội dung và header Authorization); chưa kiểm tra Gemini/các service khác.
- `AIChatViewModel.kt:275` log 4 ký tự đầu của key.
- `allowBackup="true"`, chưa đọc `xml/backup_rules`.

### Dependency Firebase không dùng (2026-11-20)
- `app/build.gradle.kts`: `firebase.bom`, `firebase.ai`, `firebase.appcheck.recaptcha`, plugin `google.services`; không có code dùng trong `src/main`.

### Dữ liệu mẫu nạp vào DB thật (2026-11-20)
- `MyApplication.kt` gọi `SampleDataSeeder.seedSampleDataIfNeeded`, lịch mẫu tháng 08/2026 (`data/SampleDataSeeder.kt`). Mức rủi ro với người dùng thật: chưa xác minh.

### Chuông báo sau khi khởi động lại máy (2026-11-20)
- Manifest không có receiver `BOOT_COMPLETED`; chuông chỉ đặt lại ở `MyApplication.onCreate`. Chưa kiểm chứng trên máy.

### Phân loại lỗi AI che lỗi thật (2026-11-05)
- `isOverloadOrRateLimit` (`util/AiConfigHelper.kt:491`) coi cả 400/401/403 là cần xoay/chuyển; che lỗi key sai (`TEMP_AI_FIX_NOTES.md`).

### Test tự động mỏng (2026-11-20)
- Chỉ có test cho migration, xóa mềm, chuỗi AI, parse dán. 3 file test mẫu template. Không CI. `androidTest` chưa rà. Thay đổi AI chưa test trên máy thật.

### Tồn đọng nhỏ
- Hai thư mục `viewmodel/` và `viewmodels/`.
- File UI vượt xa 200 dòng (`CalendarScreen.kt` 2891, `AIStudyPlannerScreen.kt` 2341).
- Tên cột `boards` camelCase lệch snake_case.
- `WelcomeScreen.kt:81` nút đăng nhập `TODO`; route `welcome` không còn là start destination.
- Chưa xóa mềm dọn dẹp (purge) bản ghi đã xóa.
- Chưa có phiên bản web (`note.txt`).
- Sự kiện quan trọng: chưa test tay trên máy thật (khóa máy, Android 14+ cấp quyền); thẻ chọn lịch của AI chưa có nút bật/tắt cờ; sự kiện lặp và widget chưa hiển thị dấu quan trọng; `ImportantEventActivity` chưa có test tự động.
