# AGENT RULES

Đây là file hướng dẫn chính. Agent đọc file này trước mọi việc. Các file khác của bộ kit là "đệ tử" của file này: file này quy định cách đọc và quản lý chúng.

## 0. Việc đầu tiên mỗi phiên: kiểm tra setup lần đầu

Đọc `.claude/mykit/setup-status.md`.

- File chưa tồn tại, hoặc còn bước chưa `done`: làm bước kế tiếp bằng prompt tương ứng trong `.claude/mykit/prompt/` (xem mục 3). Chưa xong setup thì chưa bắt đầu code.
- Tất cả bước đã `done`: bỏ qua toàn bộ mục 3, không chạy lại setup. Sang mục 4.

Chỉ user được đặt lại một bước về `chưa làm`.

---

## 1. Mục đích và nguyên tắc

Agent duy trì cả codebase lẫn trạng thái dự án dùng chung. Repository là bộ nhớ chung giữa các agent, phiên và máy.

**Source code là source of truth.** Tài liệu phải phản ánh đúng cài đặt thực tế.

Không tự sửa `AGENTS.md`. Chỉ sửa khi user yêu cầu rõ.

---

## 2. Cấu trúc bộ kit

Đường dẫn khai báo ở một chỗ này; đổi vị trí thì chỉ sửa bảng dưới.

| Đường dẫn | Vai trò |
|---|---|
| `AGENTS.md` | Cách agent làm việc (file này) |
| `.claude/mykit/setup-status.md` | Trạng thái setup lần đầu |
| `.claude/mykit/prompt/` | Prompt setup lần đầu: `init-files`, `review-docs`, `fill-data` |
| `.claude/mykit/instruction/` | 5 file hướng dẫn / memory của dự án (bên dưới) |

**Bộ kit chỉ gồm `AGENTS.md` và `.claude/mykit/`.** Document của dự án nằm ngoài bộ kit, trong thư mục tài liệu của dự án (mặc định `docs/`), cùng với `docs/doc-map.md` (bảng cho biết loại document nào nằm ở file nào). Document ở lại cùng dự án; bộ kit được mang đi thì không mang theo document.

5 file hướng dẫn trong `instruction/`:

```text
CODEBASE_OVERVIEW.md    → Codebase chứa gì và chạy thế nào
PROJECT_STATE.md        → Trạng thái tổng thể của dự án
DECISIONS.md            → Quyết định kỹ thuật/sản phẩm quan trọng
TASK_STATE.md           → Tiến độ và trạng thái công việc hiện tại
ISSUES.md               → Vấn đề tồn tại lâu ở cấp dự án
```

`CODEBASE_OVERVIEW.md` là memory của agent, **không phải document**. Document là bộ tài liệu dự án (mục 5).

Không lặp thông tin giữa các file. Chỉ cập nhật file bị ảnh hưởng.

---

## 3. Setup lần đầu (chỉ khi còn bước chưa `done`)

Thứ tự: 1 tạo file rỗng -> 2 rà soát document -> 3 user xác nhận -> 4 điền dữ liệu.

| Bước | Prompt | Việc |
|---|---|---|
| 1 | `init-files` | Tạo file hướng dẫn rỗng và `setup-status.md` |
| 2 | `review-docs` | Rà soát document, gán nhãn Đạt / Thiếu / Sai định dạng / Cũ / Chưa có, ghi `docs/doc-map.md` |
| 3 | (trong `review-docs`) | User xác nhận kết quả rà soát; chỉ `done` khi user nói rõ "xác nhận" |
| 4 | `fill-data` | Điền dữ liệu vào 5 file hướng dẫn từ code và document đã xác nhận |

Quy tắc:
- Mỗi bước có trạng thái `chưa làm | đang làm | done`. Làm xong thì ghi `done` kèm ngày vào `setup-status.md`.
- Chưa có document: hướng dẫn user tạo từng file một, không viết hàng loạt.
- Đã có document: nhận diện theo nội dung, không theo tên file. Agent chỉ đề xuất nhãn, user là người chốt. Không tự đổi tên hay xóa document.
- Agent không tự coi một file là sơ khai hay đầy đủ; nhãn do user xác nhận.
- Phiên bị ngắt: phiên sau đọc `setup-status.md` và tiếp tục từ bước đang dở.

---

## 4. Bộ document

Mọi dự án đều cần đủ 11 loại. Dự án nhỏ **gộp** nhiều loại vào một file; dự án lớn **tách** từng loại, nội dung đặc thù (ERD, lưu đồ) để ngoài và file chính tham chiếu. Agent tư vấn tách hay gộp theo độ phức tạp của dự án.

1. Kiến trúc tổng thể
2. User workflow
3. Hợp đồng tích hợp
4. Thuộc tính chất lượng
5. Database
6. Backend
7. Bảo mật và dữ liệu
8. Triển khai và hạ tầng
9. Testing và tiêu chí nghiệm thu (luôn là file riêng)
10. Maintenance (luôn là file riêng)
11. Hướng dẫn sử dụng

Thứ tự trên cũng là thứ tự viết. Tiêu chí "Đạt" của từng loại nằm trong prompt `review-docs`.

Tìm document qua `docs/doc-map.md`, không tìm theo tên file. Document mới tạo cũng lưu trong `docs/`. Sau setup, sửa hoặc thêm document trong lúc làm việc là việc bình thường và không chạy lại setup.

---

## 5. Trước khi bắt đầu một task

1. Đọc `AGENTS.md` (file này).
2. Đọc `TASK_STATE.md`.
3. Đọc `PROJECT_STATE.md` khi task phụ thuộc trạng thái tổng thể.
4. Đọc `CODEBASE_OVERVIEW.md` khi làm ở phần code lạ hoặc ảnh hưởng cấu trúc.
5. Đọc `DECISIONS.md` và `ISSUES.md` khi liên quan.
6. Đọc document liên quan (qua `docs/doc-map.md`).
7. Xem code thật liên quan task.
8. Xem Git status/diff nếu có Git.

Không code chỉ dựa vào tài liệu. Yêu cầu mơ hồ mà các cách hiểu làm thay đổi đáng kể phần cài đặt thì hỏi user.

Đọc tối thiểu cho task thông thường: `AGENTS.md`, `TASK_STATE.md`, code liên quan. Không biết file nào liên quan thì đọc, đừng đoán là không cần.

Task mới và phase mới: trình bày cho user và chờ đồng ý trước khi ghi vào `TASK_STATE.md`. Dự án chưa có code: `PROJECT_STATE.md` ghi nhiệm vụ hoàn thiện document theo thứ tự mục 4.

---

## 6. Khi cài đặt

- Hiểu hành vi hiện có trước khi đổi.
- Theo quy ước của dự án. Đổi nhỏ, đúng mục tiêu.
- Không đụng tính năng không liên quan. Không xóa hay ghi đè thay đổi của user.
- Không thêm dependency lớn, đổi kiến trúc hay refactor lớn khi chưa có lý do.
- Không âm thầm đổi một quyết định đã chốt.
- Cài đặt mâu thuẫn với tài liệu: kiểm tra code rồi cập nhật tài liệu.
- Đổi database hoặc logic nghiệp vụ: kiểm tra khớp với yêu cầu và mô hình dữ liệu hiện tại.

---

## 7. Kiểm chứng

Trước khi báo hoàn thành một task có ý nghĩa:
- Chạy test, build, lint, type check hoặc kiểm tay phù hợp.
- Xem lại Git diff nếu có.
- Không nói đã test hay kiểm chứng khi chưa làm.
- Báo vấn đề và giới hạn còn lại. Không kiểm chứng được thì nói lý do.

---

## 8. Đồng bộ trạng thái bắt buộc

Sau công việc có ý nghĩa, cập nhật các file bị ảnh hưởng (trong `.claude/mykit/instruction/`).

**`TASK_STATE.md`** - cập nhật khi bắt đầu task, tiến độ đổi đáng kể, task bị chặn hoặc xong.

**`PROJECT_STATE.md`** - ảnh chụp toàn dự án ("dự án đang ở đâu"). Gồm: dự án là gì và giai đoạn; hạ tầng và tích hợp cùng trạng thái; tính năng chia nhóm (hoạt động đã kiểm chứng / code xong chờ test / chưa làm / quyết định không làm); ràng buộc môi trường và việc cần cho production; blocker và rủi ro. Không chứa tiến độ từng bước, file đã sửa, ghi chú debug (thuộc `TASK_STATE.md`), cấu trúc code (`CODEBASE_OVERVIEW.md`), lý do quyết định (`DECISIONS.md`, chỉ link). Cập nhật khi: tính năng đổi nhóm; thêm/bỏ/đổi tích hợp, môi trường, đích deploy; áp migration lên DB dùng chung/từ xa; blocker xuất hiện hoặc được gỡ; đổi phạm vi. Không cập nhật cho tiến độ trong task. Khi task xong, chuyển kết quả lâu dài sang `PROJECT_STATE.md` và rút gọn mục trong `TASK_STATE.md`.

**`CODEBASE_OVERVIEW.md`** - cập nhật khi cấu trúc code, module, tích hợp, data flow hoặc hành vi quan trọng đổi lớn.

**`DECISIONS.md`** - cập nhật khi có quyết định kỹ thuật/sản phẩm quan trọng được chốt hoặc đổi. Chỉ thêm vào cuối theo thời gian, không xóa quyết định cũ.

**`ISSUES.md`** - cập nhật khi một vấn đề trở thành vấn đề tồn tại lâu ở cấp dự án.

Không cập nhật file chỉ để có thay đổi.

---

## 9. Cổng hoàn thành task

Một task có ý nghĩa chưa xong cho tới khi:

```text
[ ] Đã làm đúng việc được yêu cầu
[ ] Đã xem code liên quan
[ ] Đã kiểm chứng
[ ] TASK_STATE.md đã đồng bộ
[ ] Các file trạng thái khác bị ảnh hưởng đã đồng bộ
[ ] Document bị ảnh hưởng đã cập nhật
[ ] Không xóa/ghi đè thay đổi không liên quan của user
```

Task đổi một quyết định quan trọng thì ghi vào `DECISIONS.md`.

---

## 10. Xung đột và không chắc chắn

Khi thông tin mâu thuẫn:

1. Theo yêu cầu rõ ràng hiện tại của user.
2. Kiểm tra cài đặt thật.
3. Cập nhật tài liệu cũ.
4. Giữ các quyết định quan trọng trừ khi chủ ý đổi.
5. Hỏi user khi không thể xác định cách hiểu đúng một cách an toàn.

Không âm thầm bịa yêu cầu hay quyết định kiến trúc lớn.

---

## 11. Báo cáo cuối

Sau khi xong task, báo ngắn:
- Đã đổi gì
- Đã kiểm chứng gì
- Đã cập nhật file trạng thái nào
- Vấn đề hoặc blocker còn lại

Không nói hoàn thành nếu chưa đồng bộ trạng thái hoặc chưa kiểm chứng theo yêu cầu.

---

## 12. Quy tắc không thương lượng

1. Đọc trạng thái dự án liên quan trước khi code.
2. Xem code thật trước khi sửa.
3. Không bịa yêu cầu mơ hồ.
4. Không âm thầm ghi đè quyết định quan trọng.
5. Kiểm chứng thay đổi có ý nghĩa.
6. Giữ memory của dự án đồng bộ với thực tế.
7. Giữ thay đổi không liên quan của user.
8. Không nói đã làm hay đã kiểm chứng điều chưa làm.
9. Kiến thức quan trọng của dự án không được chỉ nằm trong cuộc chat.
10. **Code và trạng thái dự án phải nhất quán sau mỗi task có ý nghĩa.**
