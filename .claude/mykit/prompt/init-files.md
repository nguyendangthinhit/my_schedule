# Prompt khởi tạo 1: tạo các file hướng dẫn rỗng + file trạng thái

> Dùng khi vào dự án mới, chưa có file hướng dẫn nào. Dán nội dung trong khối dưới cho agent.
> Thứ tự setup lần đầu: (1) prompt này -> (2) `review-docs.md` -> (3) user xác nhận -> (4) `fill-data.md`.

```text
Bạn đang ở thư mục gốc của một dự án. Đây là bước 1 của setup lần đầu bộ kit. Việc: tạo file hướng dẫn rỗng
và file trạng thái.

## Việc 1 - tạo 5 file rỗng
Tạo đúng 5 file rỗng (0 byte, không tiêu đề, không nội dung mẫu) trong .claude/mykit/instruction/ :
  CODEBASE_OVERVIEW.md
  PROJECT_STATE.md
  DECISIONS.md
  TASK_STATE.md
  ISSUES.md

## Việc 2 - tạo file trạng thái .claude/mykit/setup-status.md với đúng nội dung sau
(bước 1 đánh "done" kèm ngày hôm nay sau khi Việc 1 xong; các bước khác "chưa làm")

# SETUP STATUS
Setup lần đầu của bộ kit. Agent đọc file này đầu phiên: còn bước chưa "done" thì làm bước kế tiếp bằng prompt
tương ứng trong .claude/mykit/prompt/; tất cả "done" thì bỏ qua toàn bộ phần setup, không chạy lại.
Chỉ user được đặt lại một bước về "chưa làm". Agent không tự đặt lại.
Trạng thái: chưa làm | đang làm | done. Bước 3 chỉ "done" khi user đã nói rõ "xác nhận".

| Bước | Việc | Prompt | Trạng thái | Ngày |
|---|---|---|---|---|
| 1 | Tạo file hướng dẫn rỗng + file trạng thái | init-files | chưa làm | |
| 2 | Rà soát document | review-docs | chưa làm | |
| 3 | User xác nhận kết quả rà soát | (trong review-docs) | chưa làm | |
| 4 | Điền dữ liệu vào file hướng dẫn | fill-data | chưa làm | |

## Quy tắc
- Tạo thư mục nếu chưa có.
- File nào đã tồn tại thì KHÔNG ghi đè, KHÔNG sửa; chỉ báo "đã có, bỏ qua". Nếu setup-status.md đã có,
  đọc nó và dừng, báo user đang ở bước nào.
- Không tạo hay sửa AGENTS.md, không đụng code, không đụng file khác. Không điền nội dung vào 5 file hướng dẫn.
- Xong thì báo: file đã tạo / đã có sẵn.
- Không im lặng sau khi xong: kết thúc bằng câu hỏi cho user, nêu rõ bước kế tiếp (bước 2, rà soát document, prompt review-docs) và hỏi "Làm tiếp bước 2 luôn không?". User đồng ý thì làm ngay, không chờ nhắc.
```
