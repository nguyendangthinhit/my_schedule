# My Schedule – User Workflow (mô tả DỰ ÁN CŨ, BẢN NHÁP)

Nguồn: suy ra từ code và ghi chú (`project_state.md`, `AI_CHAT_TEST_NOTES.md`, `TEMP_AI_FIX_NOTES.md`). **Chưa phỏng vấn user**, nên từng luồng chờ user xác nhận. Luồng → chức năng → dữ liệu đối chiếu `docs/architecture.md`.

## Vai trò
| Vai trò | Mô tả |
|---|---|
| Người dùng | Chủ thiết bị, không đăng nhập, toàn quyền trên dữ liệu của mình |
| Agent ngoài / web | Chưa có ở dự án cũ (thuộc dự án mới) |

## Luồng 1. Thêm sự kiện thủ công
- Bắt đầu: nút thêm (+) ở app, hoặc dấu (+) trên widget (mở route `add_event`).
- Bước: nhập tiêu đề, danh mục, ngày, giờ bắt đầu/kết thúc hoặc cả ngày, lặp (NONE/DAILY/WEEKLY/MONTHLY), nhắc trước N phút, (tùy chọn) bật "Sự kiện quan trọng" (chỉ hiện khi bật nhắc; Android 14+ chưa cấp quyền toàn màn hình thì app mở màn Cài đặt để cấp) → Lưu.
- Kết quả: `EventRepository.insertEvent` ghi `events` + `reminders` (`dirty = true`); đặt chuông AlarmManager; widget cập nhật.
- Lỗi/ngoại lệ: thiếu tiêu đề; chưa cấp quyền thông báo/báo thức chính xác thì chuông không kêu (chi tiết xử lý: chưa có thông tin).

## Luồng 2. Xem và hoàn thành việc trong ngày
- Bắt đầu: tab "Việc cần làm".
- Bước: chọn ngày ở thanh tuần → xem danh sách theo giờ → chạm để đánh dấu xong/chưa xong; tìm theo từ khóa, lọc danh mục.
- Kết quả: `toggleCompletion`/`setCompletion` ghi `event_completions` theo từng ngày (sự kiện lặp hoàn thành riêng từng ngày); thanh tiến độ cập nhật.

## Luồng 3. Xem lịch (ngày/tuần/tháng/năm)
- Bắt đầu: tab "Lịch trình" → chọn chế độ.
- Kết quả: hiển thị sự kiện, lịch âm, ngày lễ, thời tiết nếu bật. Chỉ đọc.

## Luồng 4. AI lên lịch hàng loạt (dán danh sách)
- Bắt đầu: tab "Trợ lý AI", dán danh sách buổi học/việc.
- Bước: app gửi AI → AI trả khối `schedule_json` → app hiển thị thẻ chọn từng buổi → người dùng tích chọn → bấm "AI thêm vào lịch trình".
- Kết quả: sự kiện được ghi qua Repository. AI chỉ đặt `important: true` khi người dùng nói rõ sự kiện quan trọng; thẻ chọn lịch chưa có nút bật/tắt cờ này (sửa lại ở màn sự kiện).
- Lỗi/ngoại lệ (đã code): key bị 429/5xx → xoay key; Gemini lỗi → thử chuỗi model dự phòng rồi sang Groq (và ngược lại); AI lỗi hẳn mà tin nhắn đúng định dạng `Thứ 2 (02/11) - Môn, phòng — 09:15–11:15` → `parsePastedSchedule` đọc cục bộ; vẫn lỗi → hiện lỗi gốc của nhà cung cấp.

## Luồng 5. AI xóa/sửa lịch
- Bắt đầu: nhắn "xóa lịch đánh cầu" ở tab Trợ lý AI.
- Bước: AI nhận danh sách lịch có mã E1, E2... → đề xuất khối `delete_json` → app hiện thẻ xác nhận "Xóa N lịch" / "Hủy".
- Kết quả: chỉ khi bấm Xóa mới gọi `deleteEvents` (xóa mềm). Hủy thì không đổi gì. Sửa = `delete_json` + `schedule_json` trong cùng câu trả lời.
- Ngoại lệ: câu mơ hồ → AI phải hỏi lại. **Chưa test thực tế trên máy** (`AI_CHAT_TEST_NOTES.md`).

## Luồng 6. Hỏi đáp về lịch
- Bắt đầu: hỏi ở tab Trợ lý AI ("tuần này còn việc gì chưa xong?").
- Kết quả: `AIScheduleContextHelper` đưa dữ liệu Room vào prompt; `answerDirectLocally` trả lời trực tiếp từ dữ liệu cục bộ, dùng được khi mất mạng.

## Luồng 7. Nhắc việc
- Bắt đầu: đến mốc `start - remind_before_minutes`.
- Kết quả: `NotificationReceiver` hiện thông báo (và chuông nếu bật trong Cài đặt). Có nút "thử thông báo" trong Cài đặt.
- **Sự kiện quan trọng** (`is_important`): dùng kênh riêng, luôn kêu và rung kể cả khi tắt chuông trong Cài đặt; hiện thông báo nổi (heads-up); khi máy khóa/tắt màn hình thì bật sáng màn hình `ImportantEventActivity` (tiêu đề, giờ, ghi chú; nút "Đã xem", "Hoàn thành", "Mở ứng dụng"). Khi đang dùng máy, Android chỉ hiện banner nổi. Vẫn tôn trọng công tắc tổng "bật thông báo".
- Ngoại lệ: tắt thông báo/chuông trong Cài đặt; chưa có thông tin về hành vi sau khi khởi động lại máy.

## Luồng 8. Cài đặt
Quản lý danh mục (thêm/xóa, 18 màu), nhập danh sách API key Gemini/Groq, chọn nhà cung cấp và model, bật/tắt thông báo, chuông, lịch âm, thời tiết GPS, giao diện sáng/tối/hệ thống.

## Luồng 9. Widget
Widget 2x2, 4x2, 2x1: bấm (+) mở thêm sự kiện; bấm nền mở tab Việc cần làm.

## Liên kết luồng → chức năng
| Luồng | Code chính |
|---|---|
| 1, 2 | `ui/screens`, `EventViewModel`, `ScheduleViewModel`, `EventRepository` |
| 4, 5, 6 | `AIChatViewModel`, `AIScheduleContextHelper`, `AiConfigHelper`, `Gemini/GroqApiService` |
| 7 | `NotificationHelper`, `NotificationReceiver` |
| 9 | `widget/*` |

## Chưa có thông tin (cần user)
Tên màn hình/nút chính xác, lỗi thường gặp trong thực tế, luồng Study Planner và Weekly Review, luồng onboarding (`welcome` có dùng không: `startDestination = "main"`).
