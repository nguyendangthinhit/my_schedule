# SETUP STATUS
Setup lần đầu của bộ kit. Agent đọc file này đầu phiên: còn bước chưa "done" thì làm bước kế tiếp bằng prompt
tương ứng trong .claude/mykit/prompt/; tất cả "done" thì bỏ qua toàn bộ phần setup, không chạy lại.
Chỉ user được đặt lại một bước về "chưa làm". Agent không tự đặt lại.
Trạng thái: chưa làm | đang làm | done. Bước 3 chỉ "done" khi user đã nói rõ "xác nhận".

| Bước | Việc | Prompt | Trạng thái | Ngày |
|---|---|---|---|---|
| 1 | Tạo file hướng dẫn rỗng + file trạng thái | init-files | done | 2026-11-19 |
| 2 | Rà soát document | review-docs | done | 2026-11-20 |
| 3 | User xác nhận kết quả rà soát | (trong review-docs) | done | 2026-11-20 |
| 4 | Điền dữ liệu vào file hướng dẫn | fill-data | done | 2026-11-20 |
