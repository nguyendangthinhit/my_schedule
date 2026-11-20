# TÀI LIỆU TOÀN DIỆN VỀ DỰ ÁN MY SCHEDULE (PROJECT STATE & ARCHITECTURE)

**Tên ứng dụng:** My Schedule (Ứng dụng Quản lý Lịch trình & Trợ lý AI Thông minh)  
**Package:** `com.aistudio.myschedule.xcvtq`  
**Namespace:** `com.example`  
**Nền tảng:** Android (Native Kotlin, Jetpack Compose, Material Design 3)  
**Cơ sở dữ liệu:** Android Jetpack Room (SQLite engine trên thiết bị)  
**Tình trạng:** Hoàn thiện toàn diện các tính năng cốt lõi và tích hợp Trợ lý AI thế hệ mới  

---

## 1. MỤC TIÊU VÀ ĐỊNH VỊ DỰ ÁN
**My Schedule** là giải pháp quản lý thời gian cá nhân toàn diện, kết hợp giữa mô hình quản lý lịch biểu khoa học (theo ngày, tuần, tháng, năm) và sức mạnh của các mô hình ngôn ngữ lớn (Google Gemini & Groq LPU). Ứng dụng hoạt động độc lập, bảo mật dữ liệu tuyệt đối trên thiết bị người dùng (on-device Room SQLite) đồng thời tích hợp các tiện ích thực tế như Lịch Âm Dương, Thời tiết GPS và Widgets Màn hình chính.

---

## 2. BẢN ĐỒ CHỨC NĂNG CHI TIẾT (FULL FEATURE SET)

Dự án được xây dựng với 5 phân hệ chính tại thanh điều hướng đáy màn hình (Bottom Navigation):

```
┌────────────────────────────────────────────────────────────────────────┐
│                               MY SCHEDULE                              │
├──────────────┬──────────────┬──────────────┬──────────────┬────────────┤
│  Tab 1:      │  Tab 2:      │  Tab 3:      │  Tab 4:      │  Tab 5:    │
│  Việc cần làm│  Lịch trình  │  Trợ lý AI   │  Thống kê    │  Cài đặt   │
│  (Home)      │  (Calendar)  │  (AI Hub)    │  (Stats)     │  (Settings)│
└──────────────┴──────────────┴──────────────┴──────────────┴────────────┘
```

---

### PHÂN HỆ 1: TRANG "VIỆC CẦN LÀM" (HOME SCREEN)
Trọng tâm quản lý công việc và sự kiện thường nhật:
1. **Sắp xếp thời gian thực (Chronological Order):**
   - Danh sách công việc tự động xếp theo trình tự thời gian từ sáng sớm đến tối muộn (`startTime` tăng dần).
2. **Theo dõi tiến độ hoàn thành (Task Progress & Streaks):**
   - Bộ đếm tổng số việc trong ngày và số việc đã hoàn thành.
   - Thanh tiến độ (Progress Bar) trực quan.
   - Thao tác 1 chạm để đánh dấu hoàn thành (kèm hiệu ứng gạch ngang và đổi màu trạng thái).
3. **Thanh chọn ngày nhanh (Horizontal Week Strip):**
   - Vuốt ngang xem các ngày trong tuần.
   - Nhảy nhanh về Hôm nay (Today), Ngày mai (Tomorrow) hoặc mở hộp thoại chọn ngày bất kỳ.
4. **Bộ lọc & Tìm kiếm tức thì:**
   - Tìm kiếm công việc theo từ khóa tiêu đề hoặc địa điểm.
   - Lọc nhanh theo danh mục (Học tập, Công việc, Gia đình...).
5. **Widget thời tiết tích hợp:**
   - Hiển thị nhiệt độ, biểu tượng thời tiết và địa danh thực tế dựa trên vị trí GPS.

---

### PHÂN HỆ 2: TRANG "LỊCH TRÌNH" (CALENDAR SCREEN)
Cung cấp 4 góc nhìn thời gian chuyên nghiệp đáp ứng mọi nhu cầu:
1. **Chế độ Ngày (Day View):**
   - Cột mốc 24 giờ chia theo từng tiếng.
   - Vị trí và độ dài của thẻ sự kiện được tính toán tỉ lệ chính xác theo số phút.
   - Vạch chỉ báo thời gian hiện tại (Current Time Indicator) cập nhật liên tục theo thời gian thực.
2. **Chế độ Tuần (Week View):**
   - Hiển thị 7 ngày trong tuần dạng lưới đa cột trực quan.
   - Chiều cao thanh cuộn thời gian được tối ưu hóa vừa vặn, không bị che khuất phần nội dung chi tiết.
   - Sắp xếp chuẩn xác theo thứ tự giờ giấc diễn ra trong ngày, không bị đảo lộn theo ID tạo.
3. **Chế độ Tháng (Month View):**
   - Lưới lịch tháng kinh điển hiển thị ngày Dương lịch và ngày **Âm lịch Việt Nam**.
   - Các chấm màu sắc đại diện cho các danh mục sự kiện trong ngày.
   - Danh sách các sự kiện trong ngày được chọn hiển thị ngay bên dưới để tra cứu nhanh.
4. **Chế độ Năm (Year View):**
   - Toàn cảnh 12 tháng dạng bản đồ nhiệt (Heat-map), thể hiện mật độ bận rộn qua sắc độ màu.
5. **Tích hợp Ngày lễ Quốc gia & Quốc tế:**
   - Nhận diện các ngày nghỉ lễ Việt Nam (Tết Nguyên Đán, Giỗ Tổ Hùng Vương, 30/4 - 1/5, Quốc Khánh...).

---

### PHÂN HỆ 3: TRANG "TRỢ LÝ AI LỊCH TRÌNH" (AI ASSISTANT HUB)
Được trang bị trí tuệ nhân tạo thế hệ mới kết hợp trực tiếp với cơ sở dữ liệu nội bộ:
1. **Kiến trúc AI Đa Nền Tảng (Dual Provider):**
   - **Google Gemini:** Mặc định chạy `gemini-3.5-flash-lite`, tự động dự phòng sang `gemini-3.1-flash-lite-preview`. Loại bỏ hoàn toàn các model cũ 2.5 trở xuống.
   - **Groq Cloud (Grod LPU):** Mặc định chạy `Groq Llama 3.3 70B Versatile` trên chip LPU chuyên dụng với tốc độ sinh phản hồi siêu tốc (~300–800 tokens/s), giải quyết dứt điểm lỗi máy chủ cao tải (HTTP 503 Overload) của Google.
   - Hỗ trợ thêm các model phụ: `Groq Llama 3.1 8B Instant`, `Groq Mixtral 8x7B`.
2. **Cầu nối Dữ liệu SQLite Nội bộ (AIScheduleContextHelper):**
   - AI được cung cấp toàn bộ dữ liệu lịch trình thực tế của người dùng từ Room SQLite.
   - Có thể hỏi đáp và thống kê chính xác: *"Trong tuần và tuần trước mình có bao nhiêu buổi học?"*, *"Tuần này còn những việc gì chưa xong?"*...
   - Tích hợp lớp trả lời trực tiếp từ dữ liệu nội bộ (`answerDirectLocally`) đảm bảo độ chính xác tuyệt đối ngay cả khi mất mạng.
3. **Cơ chế Lên lịch Tự động Không Giới hạn (Batch Schedule Generator):**
   - Người dùng có thể dán cả danh sách 20, 30, 50+ buổi học/công việc trong cả tháng.
   - AI không bị giới hạn số lượng tạo (dung lượng 8192 tokens), lên lịch đầy đủ 100% tất cả các tuần mà không cắt xén.
   - **Không in mã JSON thô ra chat:** Tự động loại bỏ hoàn toàn các chuỗi JSON `{"title": ...}` khỏi văn bản nói chuyện, chuyển hóa thành **danh sách thẻ sự kiện tương tác**.
   - Người dùng có thể tích chọn từng buổi học, bấm "Chọn tất cả" và nhấn nút **"AI thêm vào lịch trình"** để lưu 1 chạm vào Room Database.
4. **Tính năng AI chuyên sâu:**
   - **AI Study Planner:** Công cụ phân bổ thời gian ôn thi và học tập theo môn học, số tín chỉ và hạn nộp bài.
   - **AI Weekly Review:** Tổng kết năng suất tuần, đánh giá tỉ lệ hoàn thành và đưa ra lời khuyên cải thiện tuần tới.
5. **Cơ chế Tự phục hồi & Xoay vòng Key (Key Rotation & Auto-Failover):**
   - Hỗ trợ nhập danh sách nhiều API Key cùng lúc, tự động đổi key khác khi gặp lỗi giới hạn 429 Rate Limit.
   - Khi Gemini gặp lỗi 503 cao tải, hệ thống tự động chuyển tiếp liền mạch sang Groq Llama 3.3 70B.

---

### PHÂN HỆ 4: TRANG "THỐNG KÊ & HIỆU SUẤT" (STATS SCREEN)
Phân tích dữ liệu cá nhân giúp tối ưu hóa thói quen:
1. **Chỉ số năng suất (Productivity Score):**
   - Tính điểm phần trăm hoàn thành công việc theo tuần và theo tháng.
2. **Biểu đồ phân bổ thời gian theo Danh mục:**
   - Trực quan hóa tỉ trọng thời gian dành cho Học tập, Công việc, Sức khỏe, Gia đình...
3. **Phân tích khung giờ tập trung (Peak Focus Hours):**
   - Chỉ ra khoảng thời gian nào trong ngày người dùng làm việc và học tập hiệu quả nhất.
4. **Chuỗi ngày duy trì thói quen (Habit Streak):**
   - Ghi nhận số ngày liên tiếp hoàn thành trọn vẹn các mục tiêu đề ra.

---

### PHÂN HỆ 5: TRANG "CÀI ĐẶT & HỆ THỐNG" (SETTINGS SCREEN)
Trung tâm cấu hình và quản trị ứng dụng:
1. **Quản lý Danh mục sự kiện (Board Management):**
   - Khởi tạo sẵn **10 danh mục chuẩn mực lưu trực tiếp trong SQLite**: *Công việc, Học tập, Đi chơi, Cuộc họp, Ngày lễ, Cá nhân, Sức khỏe, Tài chính, Việc cần làm, Gia đình*.
   - Cho phép thêm, xóa danh mục tùy chỉnh với bảng màu mở rộng gồm 18 màu phong phú (trắng, pastel, indigo, teal, amber...).
2. **Cấu hình Trợ lý AI & API Keys:**
   - Chuyển đổi nền tảng độc lập giữa **Groq Cloud** và **Google Gemini**.
   - Quản lý danh sách API Key dự phòng với trạng thái hiển thị rõ ràng (*Sẵn sàng* hoặc *Rate Limit*).
   - Bảng mô tả chi tiết tên model đang chạy và cơ chế tự động chuyển tiếp khi có sự cố.
3. **Cài đặt Thông báo & Âm thanh nhắc nhở:**
   - Bật/tắt thông báo đẩy trước giờ sự kiện diễn ra.
   - Bật/tắt chuông báo âm thanh nhắc nhở.
   - Nút "Thử nghiệm thông báo & chuông báo ngay" để kiểm tra hoạt động của hệ thống chuông báo.
4. **Tùy biến Giao diện (Theme System):**
   - Hỗ trợ 3 chế độ: **Sáng (Light)**, **Tối (Dark)** và **Theo hệ thống (System)**.
5. **Lịch Âm & Thời tiết:**
   - Bật/tắt hiển thị ngày Âm lịch trên toàn bộ các chế độ xem lịch.
   - Bật/tắt hiển thị thời tiết GPS (tự động yêu cầu quyền vị trí và mở GPS khi cần).

---

## 3. TIỆN ÍCH MÀN HÌNH CHÍNH (ANDROID APP WIDGETS)
Ứng dụng đi kèm 3 mẫu Widget màn hình chính chuẩn Android:
1. **Widget Vuông (2x2 - Small Widget):**
   - Hiển thị ngày tháng hiện tại, số lượng công việc còn lại và sự kiện kế tiếp sắp diễn ra.
2. **Widget Dạng Bảng Ngang (4x2 - Compact Schedule Widget):**
   - Hiển thị danh sách các việc cần làm hôm nay kèm giờ giấc và danh mục.
3. **Widget Thanh Gọn (2x1 - Micro Widget):**
   - Thanh trạng thái tối giản hiển thị nhanh công việc quan trọng nhất tiếp theo.
4. **Cơ chế tương tác Widget:**
   - Bấm vào biểu tượng dấu cộng `(+)`: Mở nhanh màn hình Thêm sự kiện.
   - Bấm vào khoảng trống / nền của Widget: Mở ứng dụng và điều hướng thẳng vào trang **"Việc cần làm" (Home Screen)**.

---

## 4. KIẾN TRÚC KỸ THUẬT & CƠ SỞ DỮ LIỆU (TECHNICAL STACK)

### 1. Kiến trúc tổng thể:
- **Ngôn ngữ:** 100% Kotlin với Kotlin Coroutines & StateFlow.
- **Giao diện:** 100% Jetpack Compose (Declarative UI), Material Design 3 (M3).
- **Mô hình kiến trúc:** MVVM (Model - View - ViewModel) kết hợp Clean Repository Pattern.
- **Điều hướng:** Jetpack Navigation Compose với hệ thống quản lý Backstack mượt mà.

### 2. Mô hình Cơ sở dữ liệu (Room SQLite Database):
- **Tên Database:** `schedule_database` (Room version 3).
- **Các bảng dữ liệu (Tables / Entities):**
  - `events`: Lưu trữ toàn bộ thông tin sự kiện/công việc (`id`, `title`, `date`, `startTime`, `endTime`, `location`, `reminderNote`, `boardId`, `isCompleted`).
  - `boards`: Lưu trữ danh mục sự kiện (`id`, `name`, `colorHex`, `isVisible`, `sortOrder`).
  - `reminders`: Lưu trữ các mốc thời gian báo thức/nhắc nhở trước sự kiện.
  - `event_completions`: Lưu lịch sử hoàn thành sự kiện theo từng mốc thời gian.
  - `holidays`: Lưu trữ danh mục ngày nghỉ lễ.
  - `weather_forecasts`: Bộ nhớ đệm dự báo nhiệt độ theo giờ.
- **Bảo mật & Toàn vẹn dữ liệu:** Dữ liệu lưu trữ hoàn toàn nội bộ trong phân vùng `Internal Storage` của ứng dụng (`/data/data/com.aistudio.myschedule.xcvtq/databases/`), không gửi dữ liệu người dùng ra máy chủ bên thứ ba trừ khi người dùng chủ động gửi câu hỏi đến AI.

### 3. Tích hợp Mạng & Dịch vụ bên ngoài (Network & Services):
- **Retrofit & Moshi:** Kết nối RESTful API với Google Gemini API v1beta và Groq Cloud OpenAI-compatible endpoint.
- **Open-Meteo Weather API:** Lấy thông tin nhiệt độ và dự báo thời tiết trực tiếp không cần API key.
- **Android NotificationManager & AlarmManager:** Quản lý hàng đợi chuông báo và thông báo nhắc việc chính xác từng phút.

---

## 5. TỔNG KẾT
Ứng dụng **My Schedule** hiện đã hoàn thiện một hệ sinh thái đầy đủ: từ việc ghi chép, lập lịch, xem lịch 4 chế độ, ghim widget ra màn hình chính, cho đến trợ lý AI phân tích lịch trình học tập/làm việc thông minh, tốc độ cao và chống nghẽn server. Toàn bộ mã nguồn được thiết kế sạch sẽ, hiện đại và tuân thủ các tiêu chuẩn cao nhất của Android.
