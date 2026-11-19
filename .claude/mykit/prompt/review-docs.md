# Prompt khởi tạo 2: rà soát document (lần đầu)

> Chạy sau `init-files.md`, trước `fill-data.md`. Chỉ chạy ở lần setup đầu tiên.
> Dán nội dung trong khối dưới cho agent. Phần [NHÁP] chờ user chốt.

```text
Bạn đang làm bước 2 của setup lần đầu (xem .claude/mykit/setup-status.md). Việc: rà soát bộ document
của dự án trước khi code, rồi để user xác nhận.

## Điều kiện
- Đọc .claude/mykit/setup-status.md. Bước 1 phải là "done"; bước 2 chưa "done". Nếu không đúng, dừng và báo user.
- Đặt bước 2 = "đang làm".

## Bộ loại document cần có (user đã chốt danh sách, thứ tự và tiêu chí)
Thứ tự viết = thứ tự bên dưới. "Đạt" = có đủ các mục bắt buộc và không lệch với code.
1. Kiến trúc tổng thể. Bắt buộc: mục tiêu và nguyên tắc; vai trò người dùng; thành phần và trách nhiệm;
   công nghệ/framework; cấu trúc trang; dữ liệu thuộc về ai; sơ đồ tổng quan; hiện trạng so với đích.
2. User workflow (đầu vào quyết định chức năng -> backend -> database). Bắt buộc: danh sách vai trò; mỗi luồng
   chính: điểm bắt đầu, các bước, kết quả, trường hợp lỗi/ngoại lệ; liên kết luồng -> chức năng.
3. Hợp đồng tích hợp. Bắt buộc: trách nhiệm mỗi bên; định dạng yêu cầu/phản hồi (có ví dụ); xác thực/bảo mật
   giữa hai bên; dữ liệu tối thiểu mỗi bên giữ; cách cài đặt và kiểm tra; phiên bản và thay đổi.
4. Thuộc tính chất lượng. Bắt buộc: mức ưu tiên; mục tiêu đo được; kịch bản cho từng thuộc tính; đánh đổi; checklist trạng thái.
5. Database. Bắt buộc: danh sách bảng và quan hệ (ERD hoặc tương đương); ý nghĩa cột quan trọng; quy ước đặt tên;
   cách chạy migration; bảng/cột chưa dùng.
6. Backend. Bắt buộc: danh sách API/hàm/Edge Function (tên, đầu vào, đầu ra, ai được gọi, lỗi); phân tầng xử lý;
   biến môi trường server.
7. Bảo mật và dữ liệu. Bắt buộc: ranh giới tin cậy; mối đe dọa và nơi xử lý; phân quyền; dữ liệu nhạy cảm và nơi lưu;
   lưu trữ/xóa/sao lưu; quyền riêng tư; nơi giữ secret.
8. Triển khai và hạ tầng. Bắt buộc: môi trường; nơi đặt từng secret/biến; quy trình release và rollback;
   domain/HTTPS; checklist trước khi lên thật.
9. Testing và tiêu chí nghiệm thu (luôn là file riêng). Bắt buộc: tiêu chí tester phải đạt trước khi đưa user dùng thử;
   phân biệt tester và user thật; kịch bản bắt buộc; môi trường và dữ liệu test; kiểm tra release.
10. Maintenance (luôn là file riêng). Bắt buộc: lỗi thu thập ở đâu và ai nhận; ngưỡng và kênh báo động; runbook sự cố
    thường gặp; backup, restore, rotate secret, cập nhật dependency (tần suất); người chịu trách nhiệm, SLA nội bộ.
11. Hướng dẫn sử dụng (cho người dùng cuối, viết khi chức năng đã có). Bắt buộc: theo từng vai trò; từng chức năng:
    mục đích, các bước, kết quả; câu hỏi thường gặp; liên hệ hỗ trợ.
5 file trong .claude/mykit/instruction/ (CODEBASE_OVERVIEW, PROJECT_STATE, DECISIONS, TASK_STATE, ISSUES) là file
hướng dẫn/memory của agent, KHÔNG phải document: không rà soát chúng ở bước này.
Mọi dự án đều cần đủ các loại này. Dự án nhỏ thì GỘP nhiều loại vào một file; dự án lớn thì TÁCH từng loại,
nội dung đặc thù (ERD, lưu đồ) để ngoài file chính và file chính tham chiếu. Bạn tư vấn tách hay gộp dựa vào
độ phức tạp của dự án.

## Nơi lưu document
- Document của dự án nằm NGOÀI bộ kit, trong thư mục tài liệu của dự án (mặc định `docs/`). Document mới do bạn
  hướng dẫn user tạo cũng lưu ở đó, không lưu trong `.claude/mykit/`. Document đã có thì để nguyên chỗ cũ, không dời.
- Bộ kit (AGENTS.md và .claude/mykit/) chỉ chứa prompt, file mẫu và file hướng dẫn; không chứa document. Bảng
  `docs/doc-map.md` (loại document nào nằm ở file nào) cũng nằm ngoài bộ kit, cùng thư mục tài liệu của dự án.
- Việc nào user chưa muốn làm ngay (ví dụ tạo document mới) thì ghi vào doc-map với trạng thái "Chưa có (hoãn)"
  và không làm; không coi là lỗi.

## Bước A - quét
Tìm mọi file tài liệu (md/docx/pdf/txt...) ở thư mục gốc, docs/, và các thư mục tài liệu khác. Bỏ qua
node_modules, build output, .claude/mykit/instruction. Liệt kê tên + đường dẫn + dung lượng.

## Bước B - hai trường hợp

### Loại thiếu riêng lẻ (áp dụng cho B1 và B2): User workflow
User workflow thường chưa được viết thành document riêng nhưng dễ lập: bạn hướng dẫn user bằng cách phỏng vấn
từng vai trò (ai, bắt đầu ở đâu, các bước, kết quả, lỗi thường gặp), tự viết bản nháp từ câu trả lời, user đọc và sửa.
Có thể lấy ý từ code và tài liệu sẵn có để gợi ý, nhưng user mới là người xác nhận từng luồng.

### B1. Không có document nào (README mô tả chung không tính)
Hướng dẫn user tạo TỪNG file một, theo thứ tự bộ loại ở trên:
- Với mỗi loại: giải thích loại đó dùng để làm gì, hỏi user những gì cần biết, viết bản nháp, để user đọc và sửa,
  chỉ sang loại tiếp theo khi user đồng ý file này.
- Không viết hàng loạt nhiều file một lúc. Không bịa nội dung user chưa cung cấp; chỗ thiếu thì ghi rõ "chưa có thông tin".
- Hỏi user nên gộp hay tách các loại cho dự án này, giải thích lý do.
Sau mỗi file được user đồng ý, ghi vào bảng ánh xạ (Bước D).

### B2. Đã có document
Làm theo thứ tự, KHÔNG sửa document ở bước này:
1. Nhận diện theo NỘI DUNG, không theo tên file. Tên có thể khác chuẩn; một file có thể gộp nhiều loại,
   một loại có thể trải trên nhiều file. Lập bảng ánh xạ "loại document -> file/mục chứa nó".
2. Đọc kỹ từng document (không đánh giá khi chưa đọc hết). Gán nhãn:
   - Đạt: đủ ý của loại đó, đúng với code.
   - Thiếu: còn thiếu nội dung so với loại đó (nêu rõ thiếu gì).
   - Sai định dạng: có nội dung nhưng cách viết/cấu trúc khó dùng (nêu rõ vì sao).
   - Cũ: nội dung lệch với code hiện tại. BẮT BUỘC đối chiếu với code và nêu chỗ lệch cụ thể kèm đường dẫn
     file code làm bằng chứng. Code là source of truth.
   - Loại không có file nào chứa: nhãn "Chưa có".
3. Document có nhiều kiểu viết do nhiều tác giả/AI khác nhau; đừng coi khác kiểu viết là sai, chỉ đánh
   "Sai định dạng" khi thật sự khó dùng cho người/agent đọc sau.

## Bước C - user xác nhận
1. Trình bày bảng: Loại | File tìm thấy | Nhãn | Lý do/bằng chứng | Đề xuất (giữ / bổ sung / viết lại / tạo mới).
2. Agent chỉ ĐỀ XUẤT nhãn; user là người chốt. User sửa nhãn nếu bạn đánh giá sai.
3. Với mỗi mục không "Đạt", để user chọn: bổ sung, viết lại, hoặc giữ nguyên (chấp nhận rủi ro). Sau đó thực hiện
   từng việc user chọn, từng file một, user đọc và đồng ý từng file. Document cũ bị thay thế: không xóa, chỉ chuyển
   vào thư mục lưu trữ khi user đồng ý.
4. Sau khi sửa, rà soát lại các file đã đổi và trình bày lại bảng cho tới khi user nói rõ "xác nhận".

## Bước D - ghi kết quả
Chỉ sau khi user xác nhận:
- Ghi bảng ánh xạ cuối cùng vào docs/doc-map.md, ngoài bộ kit (Loại | File/mục | Ghi chú). Agent về sau dùng file
  này để tìm document, không tìm theo tên.
- Cập nhật .claude/mykit/setup-status.md: bước 2 = "done" kèm ngày; bước 3 (user xác nhận) = "done" kèm ngày.
  Không đánh "done" nếu user chưa xác nhận.

## Quy tắc
- Không sửa code dự án, không sửa AGENTS.md, không commit.
- Không tự đổi tên hay xóa document.
- Chỉ ghi điều có bằng chứng; không đoán.
- Báo cuối: số document ở từng nhãn, việc user đã chọn, việc còn lại, câu hỏi mở.
- Không im lặng sau khi xong: kết thúc bằng câu hỏi cho user. Chưa được xác nhận thì nói rõ đang chờ user nói "xác nhận". Đã xác nhận và bước 3 = done thì nêu bước kế tiếp (bước 4, fill-data) và hỏi "Làm tiếp bước 4 luôn không?".
```
