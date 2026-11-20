# TASK_STATE

## Current task
**1. Sự kiện quan trọng nổi lên ngoài màn hình** — code xong, chờ test tay (2026-11-20).
- Làm gì: công tắc "Sự kiện quan trọng" trong `AddEventScreen` (chỉ khi bật nhắc); `events.is_important` (Room v5, `MIGRATION_4_5`); kênh `schedule_event_channel_important`, heads-up + full-screen intent, `ImportantEventActivity`; manifest thêm `USE_FULL_SCREEN_INTENT`; AI đặt `important` trong `schedule_json`.
- Đã kiểm chứng: `compileDebugKotlin`, `assembleDebug`, `testDebugUnitTest` (21 test, 0 lỗi, mới: `Migration4To5Test`); `installDebug` lên điện thoại `RF8R6009JNY`, app khởi động không crash trong logcat.
- Chưa kiểm chứng: dữ liệu cũ còn nguyên sau migration trên máy thật (chưa truy cập được DB), toàn bộ hành vi nổi/khóa màn hình/Android 14 (kịch bản `docs/testing-acceptance.md` mục 13).
- Test tay: tạo sự kiện bật "Quan trọng" nhắc sau 1-2 phút, khóa máy.
- Chỗ còn mở: nút bật/tắt cờ trên thẻ AI, dấu quan trọng trong danh sách/widget.
- Doc đã cập nhật: `docs/architecture.md`, `user-workflow.md`, `integration-contract.md`, `testing-acceptance.md`, `doc-map.md`; instruction: CODEBASE_OVERVIEW, PROJECT_STATE, DECISIONS, ISSUES.

## Đã hoàn thành
| Mục | Nội dung | Xong (ngày) |
|---|---|---|
| Setup bộ kit | Rà soát doc, viết bộ doc dự án cũ (`docs/`), điền 5 file hướng dẫn | 2026-11-20 |

## Verification and blockers
- Chưa chạy build hay test trong phiên này (chỉ đọc code, viết doc). Đối chiếu code bằng đọc/grep.
- Blocker: không có.

## Remaining
| Mục | Việc | Ghi chú |
|---|---|---|
| Doc dự án mới | Viết lại doc theo dự án mới (Supabase, agent, web) | User đã định hướng 2026-11-20; cần phỏng vấn user |
| Code dự án mới | Làm sau khi có doc | |
| Test AI trên máy | Dán 35 buổi, xóa bằng AI, thẻ xác nhận, Groq (`docs/testing-acceptance.md` §4) | Cần cắm điện thoại hoặc emulator |
| Test tay sự kiện quan trọng | Kịch bản mục 13 `docs/testing-acceptance.md` | Chờ user |
| Quyết định commit | Commit `docs/` và file kit | Chỉ khi user yêu cầu |

## Source of truth
Code, migration và cấu hình là chuẩn; sau mỗi task cập nhật file này.
