# My Schedule – Hợp đồng tích hợp (mô tả DỰ ÁN CŨ, BẢN NHÁP)

App Android là client duy nhất. Không có server riêng. Nguồn: `app/src/main/java/com/example/network/*`, `util/AiConfigHelper.kt`.

## 1. Danh sách tích hợp
| Bên | Base URL | Endpoint dùng | Xác thực | Dữ liệu gửi đi |
|---|---|---|---|---|
| Google Gemini | `https://generativelanguage.googleapis.com/` | `POST v1beta/models/{model}:generateContent` | API key trong query `key=` | Prompt + ngữ cảnh lịch |
| Groq (tương thích OpenAI) | `https://api.groq.com/` | `POST openai/v1/chat/completions` | Header `Authorization: Bearer <key>` | Messages + ngữ cảnh lịch |
| Open-Meteo | `https://api.open-meteo.com/` | `GET v1/forecast` (`latitude`, `longitude`, `daily`, `hourly`, `timezone=auto`, `forecast_days=7`) | Không cần | Tọa độ (mặc định Hà Nội 21.0285, 105.8542) |
| Nager.Date | `https://date.nager.at/` | `GET api/v3/PublicHolidays/{year}/{countryCode}`, `GET api/v3/NextPublicHolidays/{countryCode}` | Không cần | Năm, mã nước (VN) |

## 2. Trách nhiệm
- App: dựng prompt, giữ key, xoay key/failover, đọc phản hồi, xác nhận với người dùng trước khi ghi/xóa, cache thời tiết/ngày lễ vào Room.
- Nhà cung cấp AI: sinh văn bản; không giữ trạng thái của app.
- Open-Meteo, Nager: trả dữ liệu công khai.

## 3. Định dạng AI
Yêu cầu (rút gọn): hệ thống prompt + lịch sử chat + (với Gemini) `maxOutputTokens` 8192 theo ghi chú trong `project_state.md` (chưa đối chiếu code).
Phản hồi AI phải chứa khối đặc biệt để app đọc:
```
```schedule_json
[{"title":"...","date":"2026-11-02","startTime":"09:15","endTime":"11:15","category":"STUDY","reminderNote":"...","important":false}]
```
```delete_json
["E3","E7"]
```
```
- `schedule_json` → thẻ chọn lịch; `delete_json` → thẻ xác nhận xóa (mã E# đổi theo từng lượt nhắn). JSON bị cắt giữa chừng được vá lại.
- Gemini: app ghép mọi `parts`, bỏ part `thought`.
- Trường `important` (boolean, tùy chọn, mặc định false) thêm 2026-11-20. Các trường khác theo prompt trong `AIChatViewModel.kt` (title, date, startTime, endTime, category, reminderNote).

## 4. Model và failover (theo code)
| Nhà cung cấp | Model | Ghi chú |
|---|---|---|
| Groq | `openai/gpt-oss-120b` (mặc định), `openai/gpt-oss-20b` | Llama/Mixtral đã bị gỡ |
| Gemini | `gemini-3.5-flash-lite` (mặc định), dự phòng `gemini-3.1-flash-lite-preview`, `gemini-2.5-flash` | |
- Lỗi 429/5xx (và cả 400/401/403, theo `isOverloadOrRateLimit`) → xoay sang key kế tiếp; hết key → chuyển nhà cung cấp; lỗi cuối cùng giữ lỗi gốc của nhà cung cấp được chọn.
- Gemini 404/500/502/503/504 → thử model dự phòng kế tiếp.
- Rủi ro đã biết: coi 400/401/403 là "quá tải" che mất lỗi key sai (`TEMP_AI_FIX_NOTES.md`).

## 5. Bảo mật giữa hai bên
- Key do người dùng nhập, lưu SharedPreferences dạng thô. Gemini truyền key qua query string.
- Tất cả qua HTTPS. `HttpLoggingInterceptor` mức BODY trên Groq (có thể ghi log nội dung và header).
- Dữ liệu tối thiểu mỗi bên giữ: app giữ toàn bộ lịch; nhà cung cấp AI chỉ nhận phần lịch trong prompt (từ 14 ngày trước, tối đa 200 mục, bỏ Ngày lễ). Nhà cung cấp có lưu hay không: theo chính sách của họ, chưa kiểm tra.

## 6. Cài đặt và kiểm tra
- Nhập key ở Cài đặt (biểu tượng 🔑), chọn nhà cung cấp/model, gửi tin thử. Log tag `AIChat` (`adb logcat -s AIChat:*`).
- Test tự động: `AiAttemptChainTest`, `ParsePastedScheduleTest` (`app/src/test/java/com/example/viewmodel/`).

## 7. Phiên bản và thay đổi
- Gemini dùng `v1beta`; Google đã gỡ model 2.5-flash-lite, nên danh sách model phải kiểm tra lại định kỳ (xảy ra thật ở commit `2b483c9`).
- Hợp đồng với Supabase/agent: **chưa có**, thuộc dự án mới (xem `my-schedule-sync-app-tasks.md`, phần giao ước sơ bộ).
