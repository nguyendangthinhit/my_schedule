# PROJECT_STATE

> Ảnh chụp "dự án đang ở đâu". Tiến độ task: `TASK_STATE.md`. Cấu trúc code: `CODEBASE_OVERVIEW.md`. Lý do quyết định: `DECISIONS.md`. Vấn đề tồn tại: `ISSUES.md`.

_Cập nhật: 2026-11-20_

Mô hình: app Android một người dùng/thiết bị, dữ liệu trong Room, gọi AI/thời tiết trực tiếp từ app. Dự án mới (Supabase, agent, web) chưa bắt đầu, sẽ viết lại doc trước rồi code.

## 1. Dự án
- My Schedule: quản lý lịch cá nhân + trợ lý AI + widget. Chưa phát hành (`versionCode 1`).
- Giai đoạn: dự án cũ hoàn thiện tính năng, đang test; chuyển sang dự án mới sau khi viết lại doc.
- Môi trường chạy: emulator `emulator-5554`, điện thoại thật qua adb (chưa kết nối khi ghi `ADB_DEPLOY_NOTES.md`, 2026-11-18).

## 2. Hạ tầng và tích hợp
| Thành phần | Trạng thái |
|---|---|
| Room | v5, migration 2→3→4→5, test `Migration3To4Test`, `Migration4To5Test` |
| Gemini / Groq | Tích hợp, failover và xoay key có code; Groq mặc định `gpt-oss-120b` |
| Open-Meteo, Nager.Date | Tích hợp có code, chưa xác minh chạy thực tế gần đây |
| Backend / Supabase / đăng nhập | **Chưa có** |
| Firebase | Khai báo dependency, không có code dùng |
| CI | Không có |
| Test tự động | 6 file unit test nghiệp vụ; chạy 2026-11-20: 21 test, 0 lỗi |

## 3. Tính năng
- ✅ Hoạt động đã test tay: **chưa có bằng chứng ghi nhận trong repo, chưa xác minh** (user cần xác nhận).
- ⏳ Code xong, chờ test tay (theo ghi chú ngày 2026-11-05): xóa lịch bằng AI có thẻ xác nhận, nút copy tin nhắn user, dán danh sách 35 buổi, failover Groq/Gemini, tự đổi model/provider khi chạm giới hạn (commit `4bc2bc2`).
- ⏳ Code xong, build và test tự động qua, đã cài lên điện thoại thật `RF8R6009JNY` (app khởi động không crash), chờ test tay: sự kiện quan trọng nổi toàn màn hình (2026-11-20; kịch bản `docs/testing-acceptance.md` mục 13).
- 🧪 Code xong, chưa test: Study Planner, Weekly Review (chưa kiểm tra lại sau khi đổi model).
- 🧪 Có trong code, chưa test tay (theo ghi chú): Việc cần làm, lịch 4 chế độ, lịch âm, thống kê, widget, nhắc việc, quản lý danh mục, theme (`project_state.md` §2 mô tả, chưa xác minh).
- 🔲 Chưa có: sync Supabase, đăng nhập, agent ngoài, bản web, chính sách quyền riêng tư.
- ⛔ Không làm: chưa có quyết định "không làm" nào được ghi.

## 4. Ràng buộc và việc cần khi lên production
- Chưa có quy trình release (xem `docs/deployment.md` §4-5): keystore, minify, tắt log BODY, mã hóa key AI, quyết định backup, bỏ destructive migration.
- Gỡ `SampleDataSeeder` hoặc đảm bảo không nạp lịch mẫu vào dữ liệu thật.
- Gỡ dependency Firebase nếu không dùng.
- Sửa `README.md` (mẫu AI Studio).

## 5. Blocker / rủi ro lớn
- Chưa test thực tế các thay đổi AI trên máy thật: cần cắm điện thoại hoặc dùng emulator.
- Danh sách model AI có thể lỗi thời bất cứ lúc nào (đã xảy ra ở `2b483c9`).
- `docs/archive/project-state-old.md` lỗi thời (Room v3, Llama/Mixtral); không dùng làm nguồn kỹ thuật.
