# Kiến trúc tổng thể

Trạng thái: đã duyệt 2026-11-07; sửa 2026-11-17 theo D-14 (web localhost là giao diện chính, Zalo chỉ là kênh chat từ xa), D-15 (SQLite cục bộ) và D-16 (Python làm lõi, Go cho cổng Zalo).
Nguồn: tài liệu thiết kế "Dự án Agent của Agent" (03/11/2026) mục 3 đến 6, chỉnh theo quyết định ngày 04/11, 07/11 và 17/11/2026 (xem `.claude/mykit/instruction/DECISIONS.md`).
Mục tiêu, phạm vi và nguyên tắc nằm ở `vision-and-scope.md`; doc này chỉ mô tả cấu trúc.

## 1. Sơ đồ phạm vi 1

```
 Trình duyệt trên laptop                         Zalo (ID của chủ, khi ra ngoài)
 http://127.0.0.1:<port>                                  │
   │ chat + bảng job + duyệt                              ▼
   │                                      CỔNG ZALO [Go] (Za-go, giữ phiên Zalo)
   │                                                      │ HTTP 127.0.0.1 + token chung
   ▼                                                      ▼
 ┌──────────── AGENT CHÍNH = BỘ ĐIỀU PHỐI [Python] (chạy local trên laptop) ──────┐
 │ kênh (web | Zalo) → xác thực → định tuyến lai → duyệt → job → trả về đúng kênh │
 │ web server chỉ nghe 127.0.0.1; khởi chạy và giám sát cổng Zalo                 │
 │ SQLite cục bộ: tin vào, job, trạng thái duyệt, log (không chứa lịch)           │
 └───────┬───────────────────────┬───────────────────────────┬────────────────────┘
         │ OpenAI-compatible     │ tiến trình con, thư mục   │ công cụ MCP
         │                       │ cho phép                  │
         ▼                       ▼                           ▼
     9router               AGENT CODE            AGENT LỊCH (API/MCP, chạy trên mạng)
  (localhost:20128/v1)   claude -p / codex exec    giữ luật "không xoá/dời"
         │                 trên repo + nhánh git              │
         ▼                                                    ▼
   nhà cung cấp model                                 Supabase (DB lịch duy nhất)
                                                                       ▲
                                                my_schedule (Room ⇄ đồng bộ qua HTTPS)
```

Agent chính là bộ điều phối, chạy trên laptop. Giao diện chính là trang web trên localhost. Zalo chỉ để chat qua lại khi chủ ra ngoài (laptop vẫn bật); chat trên web và trên Zalo làm được cùng một việc. Za-go chỉ là adapter cho kênh Zalo.

### Ngôn ngữ (D-16)

Mặc định Python. Chỉ dùng Go ở chỗ bắt buộc hoặc Go tốt hơn rõ rệt.

| Phần | Ngôn ngữ | Lý do |
|---|---|---|
| Lõi agent chính (định tuyến, vòng gọi công cụ qua 9router, duyệt, job, SQLite) | Python | Dễ thử prompt và logic; SDK OpenAI-compatible và MCP sẵn |
| Backend web | Python (cùng tiến trình với lõi) | Không tách thêm tiến trình |
| Giao diện web | HTML/JS | Như nhau với mọi backend |
| Lớp bọc agent code (`claude -p`, `codex exec`) | Python | Tiến trình con, timeout, huỷ làm được bằng asyncio; tách sang Go không có lợi |
| Cổng Zalo | Go | Bắt buộc: Za-go là thư viện Go |
| Agent lịch | Chưa chốt | Chọn cùng nền tảng chạy (doc 20) |
| Agent bộ nhớ (phạm vi 2) | Python | Phải so với Mem0, Graphiti (thư viện Python) |

Quy tắc khi gặp phần mới: viết Python; chỉ chuyển sang Go khi thư viện cần dùng chỉ có ở Go, hoặc đo được Python không đủ (hiệu năng, bộ nhớ), và ghi lý do vào `DECISIONS.md`.

### Cổng Zalo [Go]

- Một file chạy nhỏ, chỉ làm cầu: giữ phiên Zalo qua Za-go, chuyển tin vào cho lõi, gửi tin lõi yêu cầu. Không chứa logic: allowlist, định tuyến, duyệt đều ở lõi Python (một chỗ duy nhất).
- Giao tiếp qua HTTP ở `127.0.0.1`, hai chiều, kèm token chung: cổng `POST` tin vào tới lõi; lõi gọi `POST /send` của cổng để trả lời. Hợp đồng chi tiết ở `zalo-channel.md` (chưa viết).
- Lõi Python khởi chạy cổng như tiến trình con và tự khởi động lại khi cổng chết, nên chủ chỉ chạy một lệnh. Cổng chết thì web vẫn dùng được, chỉ mất kênh Zalo.

Phạm vi 1 giai đoạn đầu gồm 3 khối: agent chính (kèm web), agent code, agent lịch. Calendar_Asisstant (Gmail, Slack, Teams, Google Chat) hoãn, chưa nối và chưa sửa; sau này sẽ ghi vào agent lịch như một client nữa.

Phạm vi 2 (không vẽ): agent bộ nhớ dự án nối vào log job có cấu trúc của agent chính.

## 2. Thành phần và trách nhiệm

| Thành phần | Trách nhiệm | Không làm |
|---|---|---|
| Agent chính | Bộ điều phối: nhận tin từ mọi kênh, xác thực người gửi, định tuyến, xin duyệt, quản lý job, trả kết quả về kênh đã gửi | Không giữ lịch; không tự ghi khi chưa duyệt |
| Giao diện web | Một phần của agent chính, phục vụ ở `127.0.0.1`: khung chat, xem job và log, duyệt hành động ghi | Không mở ra mạng ngoài; không có logic riêng, chỉ gọi lõi của agent chính |
| Cổng Zalo (Go, Za-go) | Tiến trình riêng: giữ phiên Zalo, chuyển tin vào cho lõi, gửi trả lời. Dùng khi chủ không ngồi ở laptop | Không lọc, không định tuyến, không duyệt; mọi logic ở lõi Python |
| 9router | Proxy model cục bộ, API tương thích OpenAI, dự phòng nhiều nhà cung cấp | Không chứa logic nghiệp vụ |
| Agent code | Chạy CLI trên repo cho phép, chỉ đọc mặc định, sửa trên nhánh git riêng | Không đẩy lên nhánh chính |
| Agent lịch | Dịch vụ HTTP/MCP trước DB lịch, chạy trên mạng (không phụ thuộc máy cá nhân bật hay tắt); kiểm tra và áp luật; nhận đẩy/kéo từ my_schedule và gọi công cụ từ agent chính | Không xoá hay dời sự kiện có sẵn |
| Calendar_Asisstant | (Hoãn) Đọc nguồn được cấp phép, trích sự kiện, sau này ghi vào agent lịch | Chưa động đến ở giai đoạn đầu; không quét toàn bộ hộp thư |
| my_schedule | App lịch cá nhân Android; Room là bản sao cục bộ, đồng bộ hai chiều qua API của agent lịch | Không nối thẳng Supabase; không nối trực tiếp với Calendar_Asisstant |
| Supabase | Nguồn sự thật duy nhất cho lịch (Postgres) | Chỉ agent lịch truy cập; agent chính và app không ghi trực tiếp |

## 3. Ranh giới giao diện

Mỗi ranh giới là một điểm thay được mà không sửa phần còn lại.

| Ranh giới | Phía agent chính | Phía kia | Doc chi tiết |
|---|---|---|---|
| Kênh | Giao diện kênh (nhận tin, gửi tin); 2 cài đặt: web và Zalo | Trình duyệt localhost; cổng Zalo (Go) qua HTTP localhost | `web-interface.md`, `zalo-channel.md` (chưa viết) |
| Model | Client OpenAI-compatible | 9router | `model-gateway-9router.md` (chưa viết) |
| Công cụ code | Lớp bọc CLI | `claude`, `codex` | `code-agent-invocation-contract.md` (chưa viết) |
| Công cụ lịch | Client MCP | Agent lịch | `calendar-agent-mcp-tools.md` (nháp) |

## 4. Luồng chính

Luồng 1 đến 3 giống nhau dù tin đến từ web hay Zalo; kết quả trả về đúng kênh đã gửi.

Hội thoại trên web và trên Zalo tách biệt, không chung ngữ cảnh hay lịch sử chat (D-17). Job và log nằm chung SQLite nên bảng job trên web hiện mọi job, kể cả job giao từ Zalo; yêu cầu duyệt chỉ trả lời được ở đúng kênh đã tạo ra nó (D-18). Hành động được làm không cần duyệt vẫn đi qua lớp harness giới hạn phạm vi (D-18).

1. **Lệnh có tiền tố** (`/code`, `/lich`, `/trangthai`...): kênh → xác thực → định tuyến bằng luật, không gọi model → công cụ → kết quả về kênh.
2. **Tin tự nhiên**: xác thực → model qua 9router chọn công cụ và tham số → nếu là hành động ghi thì xin duyệt → duyệt (nút trên web hoặc `/duyet <mã>` trên Zalo) → thực thi → kết quả về kênh.
3. **Job code**: tạo job (mã riêng, lưu SQLite) → `claude -p` hoặc `codex exec` chạy nền trong thư mục cho phép → tóm tắt ngắn về kênh; bản đầy đủ lưu file log, xem được trên web.
4. **Lịch tự động** (hoãn): Gmail/Slack... → Calendar_Asisstant → agent lịch (kiểm trùng, gắn cảnh báo) → Supabase. Chưa làm ở giai đoạn đầu.
5. **Đồng bộ app**: my_schedule đẩy thay đổi từ Room lên và kéo dữ liệu mới về qua API của agent lịch (HTTPS) ⇄ Supabase, xung đột lấy bản `updatedAt` mới hơn. Luật "không xoá/dời" chỉ kiểm ở agent lịch.
6. **Tin Zalo từ ID lạ hoặc từ nhóm**: bỏ qua và ghi log.

## 5. Sở hữu dữ liệu

| Dữ liệu | Nơi giữ | Chủ sở hữu |
|---|---|---|
| Sự kiện, bảng, nhắc nhở, hoàn thành | Supabase (bản sao Room trên điện thoại) | Agent lịch |
| Tin vào, job, trạng thái duyệt, log | SQLite cục bộ trên laptop (D-15) | Agent chính |
| Phiên đăng nhập Zalo | Ngoài repo, quyền file chặt; chỉ cổng Zalo đọc | Cổng Zalo |
| Token chung lõi ↔ cổng Zalo | Biến môi trường hoặc file cấu hình ngoài repo | Agent chính |
| Token OAuth Gmail/Slack... | Phía Calendar_Asisstant | Calendar_Asisstant |
| Log từng job (đầy đủ) | File trên máy | Agent chính |

Đồng bộ chỉ áp cho `events`, `boards`, `reminders`, `event_completions`. `holidays` và `weather_forecasts` sinh lại được nên không đồng bộ. Bảng `events` thêm `source` và `external_id`.

## 6. Ranh giới tin cậy

- Tin cậy: ID Zalo của chủ; người dùng trên laptop qua web ở `127.0.0.1`; thư mục dự án trong danh sách cho phép.
- Web chỉ nghe `127.0.0.1`, không mở ra mạng. Vẫn phải chặn trang web lạ trong trình duyệt gọi vào localhost (kiểm `Origin`/`Host`, token phiên). Chi tiết ở `security-and-threat-model.md`.
- Không tin cậy (chỉ là dữ liệu, không bao giờ là lệnh): nội dung tin nhắn, email, file trong repo, kết quả từ nguồn ngoài.
- Mọi hành động ghi (code, lịch, đẩy git) đi qua duyệt trên web hoặc Zalo. Luật chi tiết ở `security-and-threat-model.md` và `approval-and-permissions.md` (nháp).

## 7. Điểm nối phạm vi 2

Log job có cấu trúc (mã job, dự án, task, thời điểm, kết quả, commit) là điểm nối để agent bộ nhớ nạp lại sau này. Phạm vi 1 chỉ cần ghi log đúng cấu trúc.

## 8. Việc còn mở

- [x] my_schedule đồng bộ qua API của agent lịch, không nối thẳng Supabase (chốt 2026-11-07, ISS-001).
- [x] Agent lịch là dịch vụ chạy trên mạng, không chạy trên máy cá nhân (chốt 2026-11-07, ISS-002). Nền tảng cụ thể (Vercel hay khác) chọn khi làm doc `deployment-and-operations.md`.
- [x] Giao diện chính là web localhost; Zalo chỉ là kênh chat từ xa (chốt 2026-11-17, D-14).
- [x] Job và log: SQLite cục bộ trên laptop (chốt 2026-11-17, D-15).
- [ ] Calendar_Asisstant: hoãn, chưa động đến (ISS-003). Làm lại sau khi 3 khối đầu chạy được.
- [x] Ngôn ngữ: Python làm lõi, Go cho cổng Zalo; phần mới mặc định Python (chốt 2026-11-17, D-16).
- [ ] Ngôn ngữ agent lịch: chọn cùng nền tảng chạy.
- [x] Chat trên web và trên Zalo tách biệt, không liên quan nhau (chốt 2026-11-17, D-17).
- [x] Bảng job trên web hiện mọi job; duyệt ở đúng kênh tạo yêu cầu; hành động không cần duyệt vẫn bị bọc harness (chốt 2026-11-17, D-18).
- [ ] Claude và Codex dùng đăng nhập sẵn có hay đi qua 9router.
- [ ] Tin gửi lúc agent tắt có được Za-go nhận lại không (thử ở Pha 0).
