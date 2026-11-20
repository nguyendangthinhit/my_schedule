# Thuật ngữ và sổ quyết định

Trạng thái: đã duyệt 2026-11-07; thêm D-14 đến D-19 ngày 2026-11-17.
Nguồn: `vision-and-scope.md`, `system-architecture.md`, `.claude/mykit/instruction/DECISIONS.md`, `ISSUES.md`.
Doc này chỉ tra cứu: định nghĩa ngắn và danh sách quyết định đã chốt. Lý do chi tiết nằm ở doc nguồn; khi lệch nhau thì sửa doc này theo `DECISIONS.md`.

## 1. Thuật ngữ

### Thành phần

| Thuật ngữ | Nghĩa |
|---|---|
| Agent chính | Bộ điều phối chạy trên laptop: nhận tin từ web hoặc Zalo, xác thực người gửi, định tuyến, xin duyệt, quản lý job, trả kết quả |
| Giao diện web | Trang web của agent chính ở `127.0.0.1`: chat, xem job và log, duyệt. Giao diện chính khi ngồi ở laptop |
| Kênh | Nơi chủ chat với agent chính: web (localhost) hoặc Zalo (khi ra ngoài). Hai kênh làm được cùng việc nhưng hội thoại tách biệt (D-17) |
| Agent code | Lớp bọc `claude -p` / `codex exec`, chạy trên repo trong danh sách cho phép; chỉ đọc mặc định, sửa trên nhánh git riêng |
| Agent lịch | Dịch vụ API/MCP chạy trên mạng, đứng trước DB lịch duy nhất; giữ luật "không xoá/dời" |
| Calendar_Asisstant | Dự án có sẵn, lấy lịch từ Gmail, Slack, Teams, Google Chat. Hoãn, chưa nối với agent lịch (tên giữ đúng chính tả của thư mục) |
| my_schedule | App lịch cá nhân Android. Room là bản sao cục bộ, đồng bộ hai chiều qua API của agent lịch |
| Supabase | Postgres làm DB lịch duy nhất; chỉ agent lịch truy cập |
| Za-go | Thư viện Go không chính thức để gửi nhận tin Zalo; dùng bên trong cổng Zalo |
| Cổng Zalo | Tiến trình Go nhỏ do lõi Python khởi chạy: giữ phiên Zalo qua Za-go, chuyển tin hai chiều với lõi qua HTTP `127.0.0.1` kèm token chung; không chứa logic |
| Lõi | Phần Python của agent chính: định tuyến, gọi model, duyệt, job, SQLite, backend web |
| 9router | Proxy model cục bộ (`localhost:20128/v1`), API tương thích OpenAI, dự phòng nhiều nhà cung cấp |

### Khái niệm vận hành

| Thuật ngữ | Nghĩa |
|---|---|
| Allowlist | Danh sách ID Zalo được nhận lệnh (hiện chỉ ID của chủ). Tin từ ID lạ hoặc nhóm bị bỏ qua và ghi log |
| Định tuyến lai | Lệnh có tiền tố (`/code`, `/lich`, `/trangthai`, `/duyet`) đi thẳng bằng luật, không gọi model; tin tự nhiên mới gọi model chọn công cụ |
| Duyệt (`/duyet <mã>`) | Bước xác nhận trên web (nút duyệt) hoặc Zalo (`/duyet <mã>`) trước mọi hành động ghi (sửa code, ghi lịch, đẩy git) |
| Job | Một lần chạy agent code: có mã riêng, lưu SQLite, chạy nền; tóm tắt ngắn về kênh đã gửi, bản đầy đủ lưu file log (xem được trên web) |
| Log job có cấu trúc | Bản ghi gồm mã job, dự án, task, thời điểm, kết quả, commit; là điểm nối cho phạm vi 2 |
| Chỉ đọc mặc định | Mọi công cụ khởi đầu ở quyền đọc; ghi cần duyệt |
| Kit doc-first | Bộ tài liệu đặt trong repo để agent code đọc trước khi sửa. Mô tả đầy đủ ở `doc-first-kit.md` (chưa viết) |
| Pha 0 / spike | Thử nhanh trước khi viết mã: Za-go, 9router, `claude -p`, `codex exec` |
| Phạm vi 1 / 2 | 1: agent chính, agent code, agent lịch. 2: agent bộ nhớ dự án (NCKH lần 2), chỉ chừa chỗ nối |

### Lịch và dữ liệu

| Thuật ngữ | Nghĩa |
|---|---|
| MCP | Giao thức công cụ mà agent chính dùng để gọi agent lịch (`list_events`, `create_event`...) |
| Luật "không xoá/dời" | Agent lịch chỉ tạo mới và gắn cảnh báo trùng, không xoá hay dời sự kiện có sẵn |
| `source` | Cột của `events`: nguồn sự kiện (gmail, slack, app) |
| `external_id` | Cột của `events`: mã ở nguồn ngoài, để chạy lại không tạo trùng |
| `updatedAt` | Mốc thời gian dùng giải xung đột đồng bộ: lấy bản mới hơn |
| RLS | Row Level Security của Postgres. Hiện không dùng làm đường truy cập của app (xem D-08) |
| Bảng đồng bộ | `events`, `boards`, `reminders`, `event_completions`. `holidays` và `weather_forecasts` sinh lại được nên không đồng bộ |

## 2. Sổ quyết định

Chỉ thêm, không xoá. Mã D-xx dùng để các doc khác trích dẫn. Bản gốc theo thời gian ở `DECISIONS.md`.

| Mã | Ngày | Quyết định | Doc liên quan |
|---|---|---|---|
| D-01 | 03/11 | Định tuyến lai: lệnh tiền tố đi bằng luật, tin tự nhiên gọi model | `system-architecture.md` |
| D-02 | 03/11 | Mặc định chỉ đọc; ghi code, ghi lịch, đẩy git phải duyệt trên Zalo | `approval-and-permissions.md` (chưa viết) |
| D-03 | 03/11 | Chỉ nhận lệnh từ ID Zalo của chủ; nội dung tin, email, file repo chỉ là dữ liệu, không là lệnh | `security-and-threat-model.md` (chưa viết) |
| D-04 | 03/11 | Mọi lời gọi model của agent chính đi qua 9router | `model-gateway-9router.md` (chưa viết) |
| D-05 | 03/11 | Kênh và công cụ code nằm sau giao diện riêng để thay được | `system-architecture.md` mục 3 |
| D-06 | 04/11 | Một DB lịch duy nhất (Supabase) sau agent lịch API/MCP; agent chính không giữ lịch riêng | `system-architecture.md` |
| D-07 | 04/11 | Agent chính giữ SQLite cục bộ chỉ cho job và log | `job-queue-and-logging.md` (chưa viết) |
| D-08 | 04/11, chốt lại 07/11 | my_schedule và Calendar_Asisstant chỉ gặp nhau qua agent lịch. 07/11: my_schedule đồng bộ qua API của agent lịch, không nối thẳng Supabase (ISS-001); RLS/trigger phía DB để sau | `calendar-sync-my-schedule.md` (chưa viết) |
| D-09 | 04/11 | `events` thêm `source` và `external_id` | `calendar-data-model.md` (chưa viết) |
| D-10 | 05/11 | Bộ doc riêng 24 loại cho dự án agent thay bộ 11 loại; viết từng doc, doc cần nghĩ thì dừng và ghi câu hỏi mở; hoãn bước 2 đến 4 của setup bộ kit | `doc-map.md` |
| D-11 | 07/11 | Agent lịch chạy như dịch vụ trên mạng, không đặt trên máy cá nhân, để điện thoại đồng bộ khi máy tắt. Nền tảng chưa chọn (ISS-002) | `deployment-and-operations.md` (chưa viết) |
| D-12 | 07/11 | Giai đoạn đầu chỉ làm 3 khối: agent lịch (mức đơn giản), agent code, agent chính. Calendar_Asisstant hoãn, chưa sửa, chưa nối (ISS-003) | `development-roadmap.md` |
| D-13 | 07/11 | Chốt thứ tự các pha lộ trình (Pha 0 spike, 1 agent chính, 2 agent code, 3 agent lịch, 4 ghép, 5 đánh giá); Pha 3 độc lập, được phép làm song song. Số tuần và cổng nghiệm thu chi tiết vẫn mở | `development-roadmap.md` |
| D-14 | 17/11 | Agent chính là bộ điều phối chạy local trên laptop, giao diện chính là web ở `127.0.0.1` (chat, job, duyệt). Zalo chỉ là kênh chat từ xa khi chủ ra ngoài (laptop vẫn bật), làm được cùng việc như chat trên web; Za-go chỉ là adapter. Thay mục "giao diện web riêng" trong danh sách ngoài phạm vi | `system-architecture.md`, `web-interface.md` (chưa viết) |
| D-15 | 17/11 | Job, trạng thái duyệt và log lưu SQLite cục bộ trên laptop (chốt điểm còn mở của D-07) | `job-queue-and-logging.md` |
| D-16 | 17/11 | Lõi agent chính (kèm backend web, lớp bọc agent code) viết bằng Python; cổng Zalo viết bằng Go vì Za-go là thư viện Go, chạy thành tiến trình riêng, nối với lõi qua HTTP localhost. Phần mới mặc định Python, chỉ dùng Go khi bắt buộc hoặc tốt hơn rõ rệt và ghi lý do | `system-architecture.md` mục 1 |
| D-17 | 17/11 | Hội thoại trên web và trên Zalo tách biệt, không chung ngữ cảnh hay lịch sử chat | `system-architecture.md` mục 4 |
| D-18 | 17/11 | Bảng job trên web hiện mọi job (kể cả job giao từ Zalo); yêu cầu duyệt chỉ trả lời được ở kênh đã tạo ra nó; hành động được làm không cần duyệt vẫn bị bọc harness để agent không tự làm mọi việc | `system-architecture.md` mục 4, `approval-and-permissions.md` |
| D-19 | 17/11 | Mã app my_schedule: bản chính ở ổ C (Android Studio), bản clone ở ổ D chỉ để đọc, `git pull` trước khi đọc; việc cần máy ảo ghi chú để Claude Code bên dự án app làm; codebase agent và bộ kit sẽ push lên git để đồng bộ; `my_schedule/` nằm trong `.gitignore` của repo `Agent` (hai repo tách nhau, không submodule) | `DECISIONS.md` |

## 3. Quyết định đang chờ

Chưa chốt, đã có đề xuất hoặc đang chờ thông tin. Chi tiết vấn đề lâu dài ở `ISSUES.md`.

| Chủ đề | Đề xuất / tình trạng | Chờ |
|---|---|---|
| Ngôn ngữ agent lịch | Chọn cùng nền tảng chạy | Doc 20 |
| Claude và Codex | Đăng nhập sẵn có, hoặc đi qua 9router | Kiểm `--help` |
| Bot Zalo | Tài khoản riêng hay tài khoản chính | User |
| Model cho agent chính | Chọn sau khi mua API; cần hỗ trợ gọi công cụ | User |
| Tin gửi lúc agent tắt | Za-go có nhận lại khi bật không | Spike Pha 0 |
| Nền tảng chạy agent lịch | Vercel là ứng viên | Doc 20 |
| Số tuần và cổng nghiệm thu lộ trình | | Doc 4 |
| Calendar_Asisstant | Đổi đích ghi sang agent lịch, thêm `source`/`external_id` | Sau khi 3 khối đầu chạy (ISS-003) |
