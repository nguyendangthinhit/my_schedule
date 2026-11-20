# My Schedule – Testing và tiêu chí nghiệm thu (mô tả DỰ ÁN CŨ, BẢN NHÁP)

## 1. Hiện trạng test tự động (`app/src/test/java/com/example/`)
| File | Phạm vi |
|---|---|
| `data/Migration3To4Test.kt` | Migration Room v3→v4 |
| `data/Migration4To5Test.kt` | Migration v4→v5: thêm `is_important` mặc định 0, giữ dữ liệu |
| `data/SoftDeleteAndRepositoryTest.kt` | Xóa mềm, Repository |
| `viewmodel/AiAttemptChainTest.kt` | Chuỗi thử key/model AI |
| `viewmodel/ParsePastedScheduleTest.kt` | Parse danh sách dán (4 test, gồm 35 buổi thật) |
| `ExampleRobolectricTest`, `ExampleUnitTest`, `GreetingScreenshotTest` | Mẫu của template, không kiểm tra nghiệp vụ |
`src/androidTest/` có thư mục nhưng chưa rà nội dung. Chạy: `gradle :app:testDebugUnitTest --offline`. Lần chạy gần nhất 2026-11-20: 21 test, 0 lỗi (gồm `Migration4To5Test`).

## 2. Tester và user thật
- Tester (nhà phát triển): chạy test tự động + kịch bản dưới đây trên emulator/điện thoại thật qua adb.
- User thật: chưa có (chưa phát hành). Ranh giới đưa cho user dùng thử: xem tiêu chí §3.

## 3. Tiêu chí tester phải đạt trước khi đưa user dùng thử
1. `testDebugUnitTest` qua toàn bộ, không bỏ qua test lỗi.
2. `assembleDebug` thành công; cài đè bản cũ không mất dữ liệu.
3. Mọi kịch bản §4 đạt trên ít nhất một thiết bị thật.
4. Checklist triển khai (`docs/deployment.md` §5) hoàn tất các mục bảo mật.

## 4. Kịch bản bắt buộc
**Dữ liệu cốt lõi**
1. Thêm sự kiện thường, cả ngày, lặp hằng tuần; sửa; xóa (biến mất ở mọi màn hình và widget, còn dòng với `deleted_at`).
2. Hoàn thành sự kiện lặp ở một ngày không ảnh hưởng ngày khác.
3. Nâng cấp từ bản có dữ liệu cũ: không mất dữ liệu, 10 danh mục không nhân đôi.

**AI** (từ `AI_CHAT_TEST_NOTES.md`, `TEMP_AI_FIX_NOTES.md`; tất cả đang **chưa test trên máy**)
4. Dán danh sách 35 buổi → đủ 35 thẻ, thêm vào lịch được.
5. "Xóa lịch đánh cầu" → thẻ xác nhận đúng lịch; Hủy không mất gì; Xóa mất đúng lịch.
6. Câu kép "xóa + thêm lại" → 2 thẻ.
7. Câu mơ hồ → AI hỏi lại, không xóa.
8. Gemini quá tải/key sai → failover sang Groq hoặc báo lỗi gốc rõ ràng; không hiện 3 mục mẫu vô nghĩa.
9. Mất mạng → `answerDirectLocally` vẫn trả lời.

**Nhắc việc và widget**
10. Nhắc trước N phút đúng giờ (thông báo và chuông); nút "thử thông báo" hoạt động.
11. Khởi động lại máy → chuông còn hiệu lực (chưa có thông tin về cách xử lý).
12. Cả 3 widget cập nhật sau khi thêm/xóa; nút (+) và bấm nền đúng điều hướng.

**Khác**
13. **Sự kiện quan trọng** (chưa test tay): tạo sự kiện bật "Quan trọng", nhắc sau 1-2 phút. (a) Khóa máy: màn hình bật sáng `ImportantEventActivity`, có âm và rung. (b) Đang dùng máy: banner nổi. (c) Tắt chuông trong Cài đặt: vẫn kêu. (d) Tắt thông báo tổng: không hiện. (e) "Hoàn thành" đánh dấu xong và hủy chuông; "Đã xem" chỉ đóng. (f) Android 14+ chưa cấp quyền toàn màn hình: app mở màn Cài đặt, sau khi cấp thì hoạt động. (g) Sự kiện thường không bị ảnh hưởng.
14. Chế độ sáng/tối; bật/tắt lịch âm, thời tiết (từ chối quyền vị trí thì app không crash).

## 5. Môi trường và dữ liệu test
- Emulator `emulator-5554`, thiết bị thật qua adb (`ADB_DEPLOY_NOTES.md`).
- Dữ liệu: `data/SampleDataSeeder.kt`, danh sách 35 buổi thật trong `ParsePastedScheduleTest`.
- Key AI: dùng key riêng để test, **tạo lại sau khi test** nếu từng dán vào chat/log.

## 6. Kiểm tra release
Chưa có thông tin (chưa có quy trình release, xem `docs/deployment.md`).
