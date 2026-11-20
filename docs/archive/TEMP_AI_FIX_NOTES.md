# Ghi chú tạm: sửa lỗi "lên lịch bằng AI" (05/11/2026)

> File tạm, chưa commit. Xóa khi làm xong. **Không ghi API key vào file này.**

## Trạng thái hiện tại
- Các thay đổi dưới đây **đã sửa trong working tree nhưng CHƯA commit / CHƯA push**.
- Đã build `assembleDebug` thành công, toàn bộ unit test qua, đã cài lên emulator (`emulator-5554`).
- Chưa thao tác thử trực tiếp trên giao diện app (dán lại danh sách lịch) → **việc đầu tiên mai làm**.
- Xem lại toàn bộ thay đổi: `git diff` và `git status`.

## Nguyên nhân lỗi gốc
Màn AI báo `Lỗi máy chủ (404): The model llama-3.3-70b-versatile does not exist...` kèm 3 mục mẫu vô nghĩa
("Học lý thuyết & Ôn bài…", "Đọc sách…").

Chuỗi sự kiện:
1. Header chọn Gemini → app gọi Gemini trước, Gemini lỗi (503 quá tải; `gemini-3.1-flash-lite-preview` cũng trả 503 lúc test).
2. `AiConfigHelper.isOverloadOrRateLimit` coi 400/401/403/429/5xx đều là lỗi cần chuyển → app tự gọi Groq với `llama-3.3-70b-versatile`.
3. Groq trả 404 vì **cả 3 model Groq trong app đều không còn dùng được**:
   - `llama-3.3-70b-versatile` → 404 (không tồn tại / không có quyền)
   - `llama-3.1-8b-instant` → 404
   - `mixtral-8x7b-32768` → 400 `model_decommissioned`
4. Lỗi Groq đè mất lỗi gốc của Gemini → người dùng chỉ thấy lỗi Groq.
5. Do prompt chứa chữ "lịch", `generateFallbackSchedule` hiện 3 mục mẫu không liên quan.

## Kết quả test với danh sách 35 buổi (yêu cầu thật)
| Model | Kết quả |
|---|---|
| Groq `openai/gpt-oss-120b` | 35/35, ~10s → chọn làm mặc định |
| Groq `openai/gpt-oss-20b` | 35/35, ~8s |
| Groq `qwen/qwen3.8-27b` | không trả khối JSON → loại |
| Gemini `gemini-3.5-flash-lite` | 35/35, ~18s |
| Gemini `gemini-2.5-flash` | 35/35, ~30s |
| Gemini `gemini-2.5-flash-lite` | 404, Google đã gỡ |
| Gemini `gemini-3.1-flash-lite-preview` | 503 quá tải lúc test |

Model Groq khả dụng với key lúc test: `openai/gpt-oss-120b`, `openai/gpt-oss-20b`, `openai/gpt-oss-safeguard-20b`,
`qwen/qwen3.8-27b`, `allam-2-7b` (+ whisper/orpheus/prompt-guard, không dùng cho chat).

## Các thay đổi đã làm (file → nội dung)
1. `network/GroqApiService.kt`
   - `MODEL_LLAMA_3_3_70B` → `MODEL_GPT_OSS_120B = "openai/gpt-oss-120b"`
   - `MODEL_LLAMA_3_1_8B` → `MODEL_GPT_OSS_20B = "openai/gpt-oss-20b"`
   - Xóa `MODEL_MIXTRAL_8X7B`.
2. `util/AiConfigHelper.kt`
   - Cập nhật `AVAILABLE_MODELS` (bỏ Mixtral, đổi tên/mô tả).
   - Model Llama đã lưu trong SharedPreferences sẽ tự rơi về mặc định (do `getSelectedModel` kiểm tra theo catalog).
3. `ui/screens/AIScreen.kt`, `SettingsScreen.kt`, `AIStudyPlannerScreen.kt`
   - Đổi các tham chiếu hằng số và chuỗi hiển thị "Llama 3.3 / 3.1 / Mixtral" sang GPT-OSS.
4. `viewmodel/AIChatViewModel.kt`
   - **Chuỗi model Gemini dự phòng:** khi 404/500/502/503/504 thì thử lần lượt
     model đã chọn → `3.5-flash-lite` → `3.1-flash-lite-preview` → `2.5-flash` (hằng `GEMINI_MODEL_FALLBACK_CODES`).
   - **Giữ lỗi gốc:** khi failover sang Groq cũng lỗi thì `throw geminiEx` (lỗi Gemini) thay vì lỗi Groq.
   - **`parsePastedSchedule()` (mới, `internal`)**: khi AI lỗi mà tin nhắn có dạng
     `Thứ 2 (02/11)` + `- Tên môn, phòng — 09:15–11:15.` thì app tự đọc danh sách trong máy,
     tách tên/phòng tại dấu phẩy đầu tiên nằm ngoài ngoặc, trả về các mục có thể tích chọn và thêm vào lịch.
     Chạy trước `generateFallbackSchedule` nên 3 mục mẫu vô nghĩa không còn xuất hiện trong trường hợp này.
5. `app/src/test/java/com/example/viewmodel/ParsePastedScheduleTest.kt` (file mới)
   - 4 test, gồm parse đủ 35 buổi thật, tách tên/phòng, header "Chủ nhật", chuỗi thường trả về rỗng.

## Việc nên làm tiếp (mai)
- [ ] Mở app trên emulator/điện thoại, dán lại danh sách lịch, kiểm tra thực tế (cả khi AI đang lỗi).
- [ ] Cần Groq key trong app: vào biểu tượng 🔑 nhập key mới (key cũ đã lộ trong chat → **xóa và tạo lại**).
- [ ] Cân nhắc đổi `isOverloadOrRateLimit`: hiện coi cả 400/401/403 là "quá tải" nên che mất lỗi key sai.
- [ ] Cân nhắc dùng parser cục bộ làm đường **chính** cho danh sách có cấu trúc (nhanh, không tốn token) thay vì chỉ làm dự phòng.
- [ ] Cập nhật `project_state.md` (vẫn ghi mặc định "Groq Llama 3.3 70B", "Mixtral 8x7B").
- [ ] Cập nhật `AIStudyPlannerScreen.kt` nếu muốn kiểm tra lại luồng failover ở đó (chỉ mới đổi hằng số/chuỗi).
- [ ] Quyết định commit/push (đề xuất: commit riêng, ví dụ `fix: replace decommissioned Groq models and improve AI failover`).
- [ ] Các file chưa theo dõi cần quyết định: `.idea/`, `app/schemas/`, `gradle/gradle-daemon-jvm.properties`.

## Môi trường build (để chạy lại nhanh)
Repo không có `gradlew`. Dùng Gradle 9.3.1 đã cache + JDK của Android Studio:
```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
~/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Backup cấu hình Gradle cũ: `C:\Users\ADMIN\gradle-backup\`. Phần sửa `build.gradle.kts` (keystore tùy chọn) đã push ở commit `ff0677e`.
