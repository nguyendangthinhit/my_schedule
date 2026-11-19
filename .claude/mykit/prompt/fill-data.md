# Prompt khởi tạo 2: fill data

> Dùng khi 5 file hướng dẫn đã được tạo (rỗng) và dự án **đã có sẵn code** (vd dùng lại code từ dự án khác).
> Dùng ở bước 4 của setup lần đầu, sau khi `review-docs.md` xong và user đã xác nhận.
> Dán nội dung trong khối dưới cho agent. Phần "Mô tả từng file" được tóm từ 5 file thật của một dự án mẫu, chỉ lấy cách viết, không lấy nội dung.

```text
Bạn đang ở thư mục gốc của một dự án đã có code. Các file hướng dẫn đã được tạo nhưng còn rỗng:
CODEBASE_OVERVIEW.md, PROJECT_STATE.md, DECISIONS.md, TASK_STATE.md, ISSUES.md
(trong .claude/mykit/instruction/). Việc của bạn: điền dữ liệu vào chúng từ dự án thật.

## Nguyên tắc chung
- Mã nguồn, migration, cấu hình thực tế là source of truth. Chỉ ghi điều bạn đã đọc và kiểm chứng
  trong code/cấu hình. Không suy đoán, không bịa yêu cầu hay quyết định.
- Không tự phân loại document là "sơ khai" hay "đầy đủ": việc đó đã làm ở bước rà soát và user đã chốt.
- Không sửa code dự án. Không sửa AGENTS.md. Không commit.
- Mỗi thông tin nằm ở MỘT file đúng chỗ (xem phạm vi từng file), file khác chỉ tham chiếu.
- Điều chưa biết hoặc không xác minh được (vd không truy cập được DB/cloud thật): ghi rõ
  "chưa xác minh" và lý do, không đoán.
- Viết bằng ngôn ngữ của user; ngắn gọn, bảng khi liệt kê, đường dẫn file trong dấu `backtick`.

## Thứ tự làm
1. Đọc AGENTS.md. Khảo sát dự án: cây thư mục, package/manifest, entry point, config, migration/schema,
   tài liệu có sẵn, git log gần đây.
2. Viết CODEBASE_OVERVIEW.md.
3. Viết PROJECT_STATE.md (dựa trên bước 2 và tài liệu/cấu hình có thật).
4. Viết DECISIONS.md, ISSUES.md, TASK_STATE.md (chỉ phần có bằng chứng).
5. Báo user: đã điền gì, chỗ nào "chưa xác minh", câu hỏi cần user trả lời. (Xem "Điều kiện bắt đầu và kết thúc".)

## Mô tả từng file

### CODEBASE_OVERVIEW.md - bản đồ kiến thức của code ("code chứa gì, chạy thế nào")
Là knowledge map để định hướng khi làm việc; lập từ trạng thái code hiện tại, KHÔNG ghi tiến độ,
lịch sử hay lý do quyết định. Dòng mở đầu (blockquote) nói rõ: khi tài liệu khác code thì code đúng.
Các mục theo thứ tự:
 1. Project làm gì: mô hình hệ thống, các "bề mặt"/thành phần chính (vd website công khai, admin, workspace),
    cách điều hướng/routing tổng quát.
 2. Công nghệ và cách chạy: runtime, framework, thư viện chính, backend dùng gì, biến môi trường bắt buộc,
    các script (dev/build/lint/test) kèm chú thích đúng bản chất (vd "lint" thực ra là type-check),
    cái gì KHÔNG có (test runner, CI, server...).
 3. Cấu trúc thư mục: cây dạng text, mỗi thư mục/file chính một dòng chú thích.
 4. Entry point, routing, state ownership: sơ đồ text luồng mount, bảng "URL/state -> view -> ghi chú",
    state nào do ai giữ, state nào không giữ chung.
 5. Module/component chính: chia theo bề mặt, mỗi phần một bảng "Component | Vai trò / nguồn dữ liệu".
 6. Data flow: sơ đồ text từ sự kiện đến dữ liệu rồi hiển thị; dữ liệu nào lưu phía client.
 7. Backend/DB: migration, hàm/RPC/API, bảng chính, nhóm theo chức năng, kèm số migration/file nguồn.
 8. Xác thực và phân quyền: từng loại người dùng, luồng đăng nhập, nơi kiểm tra quyền (frontend vs DB).
 9. Các flow nghiệp vụ chính: đánh số, mỗi flow một dòng "A -> B -> C".
10. Mức độ hoàn thiện: chỉ trỏ sang PROJECT_STATE.md, nêu phần còn là mô phỏng/placeholder.
11. Dependency: đang dùng / khai báo nhưng chưa dùng.
12. Rủi ro và điểm chưa nhất quán trong code (đánh số, mỗi điểm một đoạn ngắn).
13. Thứ tự nên đọc code (danh sách file theo bước).
14. Quy ước source of truth (sơ đồ ngắn: tài liệu -> code/migration/cấu hình -> hành vi thực tế)
    và lưu ý phải kiểm tra lại code/cấu hình trước khi implement hay audit.
Cách viết: tiêu đề "## N. ..." đánh số, bảng cho danh sách, code block text cho cây thư mục và sơ đồ.

### PROJECT_STATE.md - ảnh chụp toàn dự án ("dự án đang ở đâu")
Trả lời cho agent/phiên mới. Không chứa: tiến độ từng bước, file đã sửa, ghi chú debug (thuộc TASK_STATE.md),
cấu trúc code (CODEBASE_OVERVIEW.md), lý do quyết định (DECISIONS.md, chỉ link).
Đầu file: blockquote nói file này là gì và trỏ sang 3 file kia; dòng "_Cập nhật: <ngày>_";
một dòng "Mô hình" tóm tắt mô hình hệ thống/vai trò.
 1. Dự án: là gì, các bề mặt chính, giai đoạn hiện tại (test/xây dựng/production), môi trường đang chạy.
 2. Hạ tầng và tích hợp: bảng "Thành phần | Trạng thái" (backend, DB/migration đã chạy tới đâu, auth,
    hosting, API ngoài, test tự động...).
 3. Tính năng, chia nhóm bằng emoji:
    ✅ Hoạt động (đã test tay, kèm ngày) | ⏳ Code xong, chờ test tay | 🧪 Code xong, chưa test (kèm lý do chờ) |
    🔲 Chưa có / dở dang | ⛔ Không làm (đã quyết định).
 4. Ràng buộc và việc cần khi lên production: môi trường dùng chung, cấu hình cloud cần có, quy tắc không được đụng.
 5. Blocker / rủi ro lớn: chỉ cái thật sự chặn, kèm điều kiện gỡ chặn.
Cách viết: gạch đầu dòng ngắn, mỗi dòng một tính năng/trạng thái, ghi ngày xác nhận khi có.
Khi điền cho dự án mới: nhóm ✅ chỉ ghi cái đã có bằng chứng chạy được; không có bằng chứng thì
xếp ⏳/🧪 hoặc ghi "chưa xác minh". KHÔNG tự đánh dấu ✅ nếu chưa chạy hoặc user chưa xác nhận.

### DECISIONS.md - nhật ký quyết định kỹ thuật/sản phẩm quan trọng ("vì sao làm vậy")
Chỉ thêm vào cuối theo thời gian, không xóa quyết định cũ. Mỗi quyết định là một mục:
  ## YYYY-MM-DD — Tiêu đề ngắn nêu chủ đề
  - **Bối cảnh:** (tùy chọn) hoàn cảnh dẫn tới quyết định.
  - **Quyết định:** làm gì, cụ thể.
  - **Lý do:** vì sao chọn cách này (và vì sao không chọn cách khác nếu quan trọng).
  - **Hệ quả:** ảnh hưởng về sau, việc phải làm khi đổi, ràng buộc mới.
  - **Cập nhật <ngày>:** (tùy chọn) khi quyết định bị chỉnh; quyết định bị thay thì gạch bỏ phần cũ bằng ~~...~~
    hoặc ghi "thay quyết định trước...".
Khi điền cho dự án có sẵn code: chỉ ghi quyết định có bằng chứng (comment, tài liệu, commit message, cấu trúc
rõ ràng); ghi ngày từ git nếu có; không thấy lý do thì ghi "lý do: chưa rõ, cần user xác nhận" thay vì bịa.
Nếu chưa có quyết định nào đủ bằng chứng, để file chỉ có tiêu đề "# DECISIONS".

### TASK_STATE.md - tiến độ công việc ("đang làm gì, đã xong gì, còn gì")
Phần mềm trí nhớ ngắn hạn giữa các phiên. Các phần:
 - "## Current task": việc đang làm hoặc câu "Không có task đang làm"; mỗi task đánh số mục, nêu trạng thái (XONG/code xong
   chờ test/đang làm), ngày, plan liên quan, nó làm gì, đã kiểm chứng gì, cách test tay, chỗ còn mở.
 - (tùy chọn) đánh giá mức sẵn sàng production: bảng mảng/mức/ghi chú, việc chặn go-live, thứ tự đề xuất.
 - "## Đã hoàn thành": bảng "Mục | Nội dung | Xong (ngày)"; chi tiết lâu dài đã chuyển sang PROJECT_STATE.md/DECISIONS.md/plans/git log.
 - "## Verification and blockers": lệnh đã chạy và kết quả, hoặc blocker đang chặn; "(không có)" nếu trống.
 - "## Remaining ...": bảng "Mục | Việc | Ghi chú" cho việc đã ghi nhận nhưng chưa làm (kèm điều kiện bắt đầu, câu hỏi cần user).
 - "## Source of truth": một câu nêu code/migration là chuẩn.
Khi task xong, kết quả lâu dài chuyển sang PROJECT_STATE.md và mục trong file này rút ngắn.
Khi điền cho dự án mới có sẵn code: KHÔNG bịa task. Ghi "Không có task đang làm", chỉ thêm việc dở dang nếu thấy
bằng chứng rõ (TODO/FIXME lớn, nhánh git dở, thay đổi chưa commit, tài liệu kế hoạch). Phần "Đã hoàn thành" để trống.
Task thật do user giao hoặc do bước phân tích sau khi đủ document (cần user đồng ý).

### ISSUES.md - vấn đề tồn tại lâu ở cấp dự án ("điều gì còn sai/thiếu và chưa xử lý")
Dòng mở đầu: "Persistent project-level problems. Task progress lives in TASK_STATE.md." Phần "## Open",
mỗi vấn đề là "### Tiêu đề (ngày ghi nhận)" với gạch đầu dòng:
 - hiện trạng cụ thể kèm đường dẫn file/migration/dòng cấu hình liên quan;
 - ảnh hưởng/rủi ro;
 - cần gì để xử lý (quyết định, dịch vụ, công việc).
Vấn đề đã xử lý: thêm dòng "**Đã xử lý <ngày>:** ..." ngay dưới mục (giữ vết), hoặc chuyển xuống phần đã đóng.
Các vấn đề nhỏ cùng loại gom thành một mục "Tồn đọng nhỏ ..." với danh sách ngắn.
Chỉ ghi vấn đề tồn tại lâu cấp dự án, không ghi lỗi của task đang làm (đó thuộc TASK_STATE.md).
Khi điền cho dự án có sẵn code: ghi vấn đề thấy trực tiếp trong code/cấu hình (dữ liệu mẫu, tính năng chưa nối backend,
dependency khai báo không dùng, thiếu test/CI, secret/cấu hình rủi ro, lệch giữa tài liệu và code); mỗi mục nêu đường dẫn
bằng chứng. Không thấy vấn đề thì để file chỉ có tiêu đề + "## Open" trống.

## Điều kiện bắt đầu và kết thúc
- Đọc .claude/mykit/setup-status.md. Bước 1, 2, 3 phải là "done" (document đã được rà soát và user đã xác nhận);
  nếu chưa, dừng và báo user chạy bước còn thiếu. Đặt bước 4 = "đang làm".
- Tìm document qua docs/doc-map.md (không tìm theo tên file; document nằm ngoài bộ kit). Khi có document đã xác nhận, dùng
  chúng làm đầu vào cùng với code; chỗ document và code lệch nhau thì code đúng, ghi chỗ lệch vào ISSUES.md.
- Khi điền xong cả 5 file: tóm tắt cho user (đã điền gì, chỗ "chưa xác minh", câu hỏi mở), rồi đặt bước 4 = "done"
  kèm ngày. Sau đó toàn bộ setup lần đầu kết thúc, không chạy lại các prompt này.
- Không im lặng sau khi xong: báo setup đã hoàn tất và hỏi user muốn bắt đầu task nào tiếp theo (theo AGENTS.md mục 5).
```
