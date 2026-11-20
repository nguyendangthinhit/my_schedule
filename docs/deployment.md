# My Schedule – Triển khai và hạ tầng (mô tả DỰ ÁN CŨ, BẢN NHÁP)

## 1. Môi trường
| Môi trường | Hiện trạng |
|---|---|
| Dev | Android Studio + Gradle 9.3.1 (không có `gradlew`), JDK của Android Studio (`.../Android Studio/jbr`) |
| Test | Emulator `emulator-5554` hoặc điện thoại thật qua adb |
| Production | Chưa có (chưa lên Google Play; `versionCode = 1`, `versionName = "1.0"`) |
Không có hạ tầng server.

## 2. Build và cài
```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
~/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle installDebug --offline
adb logcat -s AIChat:*
```
- `installDebug` cài đè, giữ dữ liệu. Chữ ký khác (APK cũ) → `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, phải gỡ và mất dữ liệu app.
- Chuẩn bị điện thoại (USB debugging, Install via USB trên Xiaomi/Oppo/Vivo/Realme): xem `ADB_DEPLOY_NOTES.md`.
- Ký debug: dùng `debug.keystore` ở thư mục gốc nếu có, không có thì bỏ qua (`app/build.gradle.kts`, commit `ff0677e`).

## 3. Nơi đặt secret/biến
| Secret | Nơi đặt |
|---|---|
| Key Gemini/Groq của người dùng | Nhập trong app (SharedPreferences) |
| `GEMINI_API_KEY` build-time | `.env` (plugin secrets, `.env.example` mặc định); chưa xác minh code có dùng |
| `google-services` | Thiếu file → chỉ cảnh báo (`MissingGoogleServicesStrategy.WARN`) |
Không commit `.env`, `local.properties`, keystore thật.

## 4. Release và rollback
- **Chưa có quy trình release.** Hiện trạng release: `isMinifyEnabled = false`, chưa có signing config release, chưa có chiến lược tăng `versionCode`.
- Rollback: chưa có thông tin (chưa phát hành).
- Domain/HTTPS: không áp dụng (không có server).

## 5. Checklist trước khi lên thật (đề xuất, chưa thực hiện)
- [ ] Tạo keystore release, lưu ngoài repo
- [ ] Bật minify/shrink, kiểm tra proguard rules
- [ ] Đặt `HttpLoggingInterceptor` về NONE ở release
- [ ] Mã hóa key AI (Keystore/EncryptedSharedPreferences)
- [ ] Quyết định `allowBackup` và nội dung backup rules
- [ ] Bỏ `fallbackToDestructiveMigration`
- [ ] Chạy toàn bộ test (xem `docs/testing-acceptance.md`)
- [ ] Viết chính sách quyền riêng tư (dữ liệu gửi tới AI, vị trí)
- [ ] Sửa README (hiện là mẫu AI Studio, bước 5 yêu cầu bỏ signingConfig không còn đúng)
