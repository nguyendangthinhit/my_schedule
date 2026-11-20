# DECISIONS

Nhật ký quyết định, chỉ thêm vào cuối. Quyết định bên dưới có bằng chứng từ git/tài liệu; chỗ nào không rõ lý do thì ghi rõ.

## 2026-11-05 — Thay model Groq bị gỡ bằng GPT-OSS
- **Bối cảnh:** `llama-3.3-70b-versatile`, `llama-3.1-8b-instant` trả 404, `mixtral-8x7b-32768` bị `model_decommissioned`.
- **Quyết định:** Groq mặc định `openai/gpt-oss-120b`, phụ `openai/gpt-oss-20b`; thêm chuỗi model Gemini dự phòng; khi lỗi giữ lỗi gốc của nhà cung cấp được chọn.
- **Lý do:** test 35 buổi: gpt-oss-120b 35/35 ~10s; qwen không trả JSON nên loại (`TEMP_AI_FIX_NOTES.md`).
- **Hệ quả:** phải kiểm tra lại danh sách model định kỳ. Commit `2b483c9`.

## 2026-11-05 — AI chỉ đề xuất, người dùng xác nhận mới xóa/ghi
- **Quyết định:** AI đề xuất xóa bằng khối `delete_json`, app hiện thẻ xác nhận; thêm lịch qua thẻ chọn.
- **Lý do:** bản cũ báo "đã xóa" nhưng không có quyền xóa và thêm trùng 35 lịch (`AI_CHAT_TEST_NOTES.md`).
- **Hệ quả:** AI chỉ thấy lịch từ 14 ngày trước, tối đa 200 mục, mã E# đổi theo lượt.

## 2026-11-17 — Room v4: UUID, xóa mềm, cờ dirty (chuẩn bị sync)
- **Quyết định:** `id` UUID String, thêm `updated_at`, `deleted_at`, `dirty`; ghi tập trung qua `EventRepository`; 10 danh mục mặc định có UUID cố định (`DefaultBoards.kt`).
- **Lý do:** hai nơi cùng tạo bản ghi không được trùng id; thiết bị khác phải biết bản ghi bị xóa (`my-schedule-sync-app-tasks.md`). Commit `4197013` theo dõi schema v4.
- **Hệ quả:** sync Supabase chưa có; `fallbackToDestructiveMigration(true)` vẫn còn (lý do giữ: chưa rõ, cần user xác nhận).

## 2026-11-20 — Tự đổi model và provider khi chạm giới hạn
- **Quyết định:** commit `4bc2bc2` "auto-switch AI model and provider on limit". Lý do chi tiết: chưa rõ, cần user xác nhận.

## 2026-11-20 — Tên cột chuẩn là snake_case
- **Quyết định (user):** dự án mới dùng snake_case; code cũ (`boards` camelCase) chưa đổi.
- **Hệ quả:** khi tạo bảng Supabase và viết lại schema phải đổi `boards`.

## 2026-11-20 — Viết lại doc theo dự án cũ trước, rồi mới viết doc/code dự án mới
- **Quyết định (user):** `docs/*` hiện tại mô tả app cũ; doc mới và code mới làm sau.
- **Hệ quả:** hướng dự án mới (Supabase, agent, bản web, popup nổi sự kiện quan trọng) mới là ý tưởng trong `note.txt`, chưa phải quyết định.

## 2026-11-20 — Sự kiện quan trọng: cờ trên từng sự kiện, thông báo nổi + toàn màn hình
- **Bối cảnh:** `note.txt`: sự kiện được đánh dấu quan trọng phải nổi lên ngoài màn hình, chấp nhận thông báo bên ngoài.
- **Quyết định (user):** thêm `events.is_important` (Room v5); thông báo kênh riêng `schedule_event_channel_important`, heads-up + `setFullScreenIntent` mở `ImportantEventActivity`; luôn kêu và rung kể cả khi tắt chuông trong Cài đặt (vẫn tôn trọng công tắc tổng "bật thông báo"); làm gọn, không qua plan/agent.
- **Lý do:** cờ theo từng sự kiện linh hoạt hơn theo danh mục hay theo mức nhắc; full-screen intent là cách Android cho phép nổi lên khi khóa máy mà không cần quyền overlay (nhạy cảm, bị Google Play hạn chế).
- **Hệ quả:** chỉ có tác dụng khi sự kiện bật nhắc (`has_reminder`). Khi đang dùng máy, Android chỉ hiện banner. Android 14+ có thể phải cấp quyền toàn màn hình (app mở Cài đặt khi bật công tắc). Kênh thông báo đã tạo không đổi được mức ưu tiên nên dùng kênh mới. AI đặt cờ qua `important` trong `schedule_json` (chưa có nút chỉnh trên thẻ chọn lịch). Cột chưa sync vì chưa có sync.
