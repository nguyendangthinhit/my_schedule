# CODEBASE_OVERVIEW

> Bản đồ code "chứa gì, chạy thế nào", lập từ code ngày 2026-11-20. Khi tài liệu khác code thì code đúng. Không ghi tiến độ (xem `TASK_STATE.md`), trạng thái (`PROJECT_STATE.md`), lý do quyết định (`DECISIONS.md`). Document chi tiết: `docs/doc-map.md`.

## 1. Project làm gì
App Android native "My Schedule": quản lý lịch (việc cần làm, lịch ngày/tuần/tháng/năm, lịch âm VN, thời tiết, widget) kèm trợ lý AI (Gemini/Groq). Một app, không server riêng. Dữ liệu ở Room trên máy. Điều hướng: `NavHost` start `main` (`ui/navigation/AppNavigation.kt`), `MainScreen` có 5 tab (home, calendar, AI, stats, settings); route khác: `welcome` (không còn là điểm bắt đầu), `add_event`, màn chi tiết danh mục.

## 2. Công nghệ và cách chạy
- Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Room (KSP), Retrofit + Moshi + OkHttp, Coroutines/StateFlow. minSdk 28, targetSdk 36, namespace `com.example`, applicationId `com.aistudio.myschedule.xcvtq`.
- Build: **không có `gradlew`**; dùng Gradle 9.3.1 đã cache + JDK của Android Studio (lệnh trong `docs/deployment.md`). `installDebug`, `:app:testDebugUnitTest`, `assembleDebug`.
- Env: `.env` (plugin secrets, `.env.example`), `local.properties`. Key AI do người dùng nhập trong app.
- Không có: CI, backend, DataStore, Supabase, test UI thực chất (`androidTest` chưa rà).

## 3. Cấu trúc thư mục
```
app/src/main/java/com/example/
  MainActivity.kt, MyApplication.kt   Application: tạo DB/Repository, kênh thông báo, seed mẫu, đặt lại chuông + widget
  data/            AppDatabase (v5, migrations), EventRepository (ghi tập trung), DashboardRepository,
                   DefaultBoards (UUID cố định 10 danh mục), SampleDataSeeder, TimeProvider
  data/dao/        ScheduleDao (mọi truy vấn)    data/entities/  Board, Event, Reminder, EventCompletion, Holiday, WeatherForecast
  models/          Event, EventInstance, ai/AISuggestedScheduleItem, dashboard/*
  network/         GeminiApiService, GroqApiService, WeatherApiService(+SyncRepository), CalendarApiService(+SyncRepository)
  util/            AiConfigHelper (key, model, failover), AIScheduleContextHelper, NotificationHelper/Receiver,
                   ImportantEventActivity (màn hình nổi cho sự kiện quan trọng),
                   DateUtils, LunarCalendarHelper, ThemeHelper, WeatherHelper
  viewmodel/       ScheduleViewModel, AIChatViewModel, AIWeeklyReviewViewModel, DashboardViewModel
  viewmodels/      EventViewModel (thư mục thứ hai, tên gần giống)
  ui/screens/      Home, Calendar, AI, AIStudyPlanner, AIWeeklyReview, AddEvent, CategoryDetail, DashboardOverview, Stats, Settings, Welcome, Main
  ui/components/   EventDetailSheet, DeleteConfirmationDialog, dashboard/
  widget/          3 provider (Small 2x2, Compact 4x2, Micro 2x1), ActionReceiver, Service, DataHelper, Renderer
  analytics/       AnalyticsEngine
app/schemas/       Room schema export (4.json, 5.json)     app/src/test/  unit test (xem docs/testing-acceptance.md)
```

## 4. Entry point, state
`MainActivity` → `AppNavigation` → `MainScreen`/`add_event`. `MyApplication` giữ `database` và `repository` (lazy), ViewModel lấy từ đây. Cài đặt (theme, key AI, bật/tắt chuông, lịch âm, thời tiết) trong SharedPreferences (`AiConfigHelper`, `NotificationHelper`, `ThemeHelper`). Cờ seed mẫu: `sample_data_prefs`.

## 5. Module chính
| Component | Vai trò / nguồn dữ liệu |
|---|---|
| `EventRepository` | Điểm ghi duy nhất: đặt `updated_at`, `dirty`; xóa mềm event + reminder + completion |
| `ScheduleDao` | Truy vấn lọc `deleted_at IS NULL`, JOIN `boards` |
| `AIChatViewModel` | Dựng prompt, gọi Gemini/Groq, failover, tách `schedule_json`/`delete_json`, `parsePastedSchedule` |
| `AiConfigHelper` | Danh sách key, xoay key, catalog model, `isOverloadOrRateLimit` |
| `AIScheduleContextHelper` | Đưa lịch Room vào prompt (mã E#), `answerDirectLocally` |
| `NotificationHelper` | Đặt/hủy AlarmManager, `rescheduleAllAlarmsAndRefreshWidgets`; 3 kênh (sound, silent, important); `showImportantNotification` (heads-up + full-screen intent), `canUseFullScreenIntent` |

## 6. Data flow
Thao tác UI → ViewModel → `EventRepository` → Room → Flow → UI. Sau ghi: đặt lại chuông và làm mới widget. AI: tin nhắn + lịch Room → Gemini/Groq → thẻ UI → người dùng xác nhận → Repository. Thời tiết/ngày lễ: API → Room (cache) → UI.

## 7. Database
Room `schedule_database` v5. Migration `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5` (thêm `events.is_important`) trong `AppDatabase.kt`; `fallbackToDestructiveMigration(true)`. Bảng và cột: `docs/architecture.md` Phần 2. Bảng sync: boards, events, reminders, event_completions; cache: holidays, weather_forecasts.

## 8. Xác thực và phân quyền
Không có tài khoản. Quyền Android: INTERNET, vị trí, POST_NOTIFICATIONS (xin ở `MainScreen`, `SettingsScreen`), báo thức chính xác, USE_FULL_SCREEN_INTENT (Android 14+ có thể cần cấp trong Cài đặt).

## 9. Flow nghiệp vụ
1. Thêm sự kiện: AddEvent → Repository.insertEvent → chuông + widget.
2. Hoàn thành: toggle → `event_completions` theo ngày.
3. AI lên lịch: dán → AI → thẻ chọn → "AI thêm vào lịch trình" → Repository.
4. AI xóa: `delete_json` → thẻ xác nhận → `deleteEvents` (xóa mềm).
5. Failover: lỗi key → xoay key → đổi model → đổi nhà cung cấp → `parsePastedSchedule` cục bộ.
6. Nhắc việc: AlarmManager → `NotificationReceiver` → thông báo/chuông. Sự kiện `is_important`: → kênh `schedule_event_channel_important` + full-screen intent → `ImportantEventActivity` (đã Đã xem / Hoàn thành / Mở app).
Chi tiết: `docs/user-workflow.md`.

## 10. Mức độ hoàn thiện
Xem `PROJECT_STATE.md`. Phần mô phỏng: `SampleDataSeeder` nạp lịch mẫu tháng 8/2026; `WelcomeScreen` nút đăng nhập là `TODO`.

## 11. Dependency
- Dùng: Compose, Room, Retrofit/Moshi/OkHttp (+logging-interceptor), Coroutines, Navigation, Lifecycle.
- Khai báo nhưng không thấy code dùng (grep `firebase` không ra kết quả trong `src/main`): `firebase.bom`, `firebase.ai`, `firebase.appcheck.recaptcha`, plugin `google.services`. Camera, DataStore, Coil, Firestore, Auth, location: đã comment.

## 12. Rủi ro và chưa nhất quán trong code
1. Hai thư mục ViewModel: `viewmodel/` và `viewmodels/`.
2. Tên cột lệch: `boards` camelCase, bảng khác snake_case.
3. File UI rất lớn: `CalendarScreen.kt` 2891, `AIStudyPlannerScreen.kt` 2341, `SettingsScreen.kt` 1710, `AIScreen.kt` 1609 dòng (rule dự án: <200 dòng).
4. `fallbackToDestructiveMigration(true)` có thể xóa sạch dữ liệu.
5. Không thấy `BOOT_COMPLETED` receiver trong `AndroidManifest.xml`; chuông chỉ đặt lại khi mở app (`MyApplication.onCreate`). Chưa kiểm chứng trên máy.
6. `isOverloadOrRateLimit` coi 400/401/403 là lỗi cần chuyển/xoay.

## 13. Thứ tự nên đọc code
`AppDatabase.kt` → `entities/*` → `ScheduleDao.kt` → `EventRepository.kt` → `MyApplication.kt` → `AppNavigation.kt`/`MainScreen.kt` → `NotificationHelper.kt` → `AiConfigHelper.kt` → `AIChatViewModel.kt` → `AIScheduleContextHelper.kt` → `widget/*`.

## 14. Source of truth
Code, `AppDatabase.kt`/`app/schemas`, `build.gradle.kts`, `AndroidManifest.xml` > `docs/*` > ghi chú rời (`project_state.md` phần kỹ thuật đã cũ). Kiểm tra lại code trước khi implement.
