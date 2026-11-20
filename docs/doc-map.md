# Doc Map (user xác nhận 2026-11-20)

Bộ document mô tả **dự án cũ** (app Android hiện tại, Room v5). Sẽ viết lại cho dự án mới sau.

| # | Loại | File/mục | Ghi chú |
|---|---|---|---|
| 1 | Kiến trúc tổng thể | `docs/architecture.md` Phần 1 | User đã duyệt 2026-11-20 |
| 2 | User workflow | `docs/user-workflow.md` | Nháp suy ra từ code, chưa phỏng vấn |
| 3 | Hợp đồng tích hợp | `docs/integration-contract.md` | Chỉ AI/thời tiết/ngày lễ; hợp đồng Supabase thuộc dự án mới |
| 4 | Thuộc tính chất lượng | — | Chưa có (hoãn) |
| 5 | Database | `docs/architecture.md` Phần 2 | Chuẩn tên cột: snake_case |
| 6 | Backend | — | Chưa có (hoãn); app không có backend |
| 7 | Bảo mật và dữ liệu | `docs/architecture.md` Phần 3 | |
| 8 | Triển khai và hạ tầng | `docs/deployment.md` | Chưa có quy trình release |
| 9 | Testing và nghiệm thu | `docs/testing-acceptance.md` | Kịch bản AI chưa test trên máy |
| 10 | Maintenance | — | Chưa có (hoãn) |
| 11 | Hướng dẫn sử dụng | — | Chưa có (hoãn); tính năng mô tả ở `docs/archive/project-state-old.md` §2 |

Tài liệu ngoài bộ 11 loại: `docs/archive/project-state-old.md` (đã lưu trữ 2026-11-20; phần tính năng còn tham khảo được, phần kỹ thuật cũ), `my-schedule-sync-app-tasks.md` (kế hoạch sync, thuộc dự án mới), `ADB_DEPLOY_NOTES.md`, `AI_CHAT_TEST_NOTES.md`, `docs/archive/TEMP_AI_FIX_NOTES.md` (đã lưu trữ), `note.txt`, `README.md` (mẫu AI Studio, cũ).

**Vai trò agent con (2026-11-20):** doc chuẩn của hệ thống Agent nằm ở `docs/agent-chinh/` (bản sao từ repo `Agent`, không sửa ở đây). Xem `AGENTS.md` mục 1b. Khi mâu thuẫn với các doc mô tả dự án cũ ở trên, theo `docs/agent-chinh/`.
