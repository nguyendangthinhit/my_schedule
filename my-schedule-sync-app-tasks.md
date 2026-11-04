# My Schedule – Việc cần làm phía app để sync 2 chiều với Supabase

Dành cho Claude Code đọc cùng mã nguồn. Phần Supabase (tạo project, bảng, RLS) do người dùng tự làm; ở cuối file có mục "Giao ước với phía Supabase" để hai bên khớp nhau.

**Lưu ý:** file này viết từ `project_state.md`, chưa đọc mã nguồn thật. Tên class, file, cột dưới đây cần đối chiếu lại với code trước khi sửa.

## Mục tiêu và nguyên tắc

- Room vẫn là bản làm việc của app. Widget, chuông báo và chế độ offline vẫn đọc từ Room.
- Supabase (Postgres) là điểm gặp chung với agent bên ngoài (Telegram...). Agent ghi lên server, app kéo về.
- Sync hai chiều, tăng dần, an toàn khi chạy lặp lại (idempotent).
- Không làm hỏng dữ liệu hiện có trên điện thoại khi nâng cấp.
- Xoá luôn là xoá mềm, để thiết bị khác biết bản ghi đã bị xoá.

## Phạm vi bảng

| Bảng Room | Sync? | Ghi chú |
|---|---|---|
| `events` | Có | Bảng chính |
| `boards` | Có | 10 danh mục mặc định phải có id cố định, xem Bước 1 |
| `reminders` | Có | Gắn với event |
| `event_completions` | Có | Lịch sử hoàn thành theo mốc thời gian |
| `holidays` | Không | Dữ liệu cache cục bộ |
| `weather_forecasts` | Không | Cache cục bộ |

---

## Bước 0 – Khảo sát code (không sửa gì)

Ghi lại để các bước sau không bỏ sót:
1. Danh sách Entity, DAO, `AppDatabase` (đang là Room version 3), các foreign key giữa bảng.
2. Mọi chỗ **ghi** vào DB (insert, update, delete): Repository, ViewModel, màn hình thêm sự kiện, `AIScheduleContextHelper` / luồng "AI thêm vào lịch trình", widget, receiver của thông báo.
3. Mọi chỗ **lên lịch chuông** bằng AlarmManager và nơi huỷ chuông.
4. Mọi chỗ **đọc** DB: màn hình, widget, thống kê, hàm `answerDirectLocally`.
5. Chỗ seed 10 danh mục mặc định.

**Xong khi:** có một danh sách ngắn các điểm ghi và đọc, kèm tên file.

## Bước 1 – Đổi schema Room (Room v3 → v4)

Cho mỗi bảng được sync, thêm hoặc đổi:

1. **`id` đổi từ Int tự tăng sang UUID kiểu String.** Lý do: hai nơi cùng tạo bản ghi sẽ trùng id nếu dùng số tự tăng.
2. Thêm `updatedAt: Long` (epoch ms, UTC), cập nhật mỗi lần ghi.
3. Thêm `deletedAt: Long?` (null nghĩa là chưa xoá).
4. Thêm `dirty: Boolean` (true nghĩa là có thay đổi chưa đẩy lên server).
5. Các khoá ngoại (`events.boardId`, `reminders.eventId`, `event_completions.eventId`...) đổi sang kiểu String UUID tương ứng.

**Migration 3→4 (viết tay, không dùng xoá-và-tạo-lại):**
- Tạo bảng mới, sinh UUID cho từng dòng cũ, giữ ánh xạ id cũ → UUID để cập nhật khoá ngoại, copy dữ liệu sang, đổi tên bảng.
- Dữ liệu cũ: `updatedAt = thời điểm migrate`, `dirty = true` (để lần sync đầu đẩy hết lên server), `deletedAt = null`.
- **10 danh mục mặc định:** dùng UUID cố định (hard-code hoặc sinh theo tên, ví dụ UUID v5), đảm bảo mọi thiết bị đều ra cùng id. Nếu không, mỗi lần cài lại sẽ nhân đôi danh mục trên server.
- Xuất schema Room (`exportSchema = true`) và viết **migration test** với một DB v3 mẫu có dữ liệu thật.

**Xong khi:** mở app trên bản dữ liệu cũ không mất dữ liệu nào, khoá ngoại vẫn đúng, migration test qua.

## Bước 2 – Xoá mềm và lớp ghi tập trung

1. Mọi thao tác ghi đi qua **một Repository duy nhất**. Repository tự đặt `updatedAt = now`, `dirty = true`. Không để ViewModel hay widget gọi DAO ghi trực tiếp.
2. Xoá sự kiện, danh mục, nhắc nhở: đổi thành `UPDATE ... SET deletedAt = now, dirty = true`, không `DELETE`.
3. Sửa **mọi truy vấn đọc** thêm `WHERE deletedAt IS NULL`: danh sách việc, lịch 4 chế độ, thống kê, widget, `AIScheduleContextHelper`, `answerDirectLocally`.
4. Dọn dẹp bản ghi đã xoá mềm và đã sync xong: để sau, không cần làm trong đợt này.

**Xong khi:** xoá một sự kiện thì nó biến mất khỏi mọi màn hình và widget nhưng vẫn còn dòng trong DB với `deletedAt`.

## Bước 3 – Tầng remote (Supabase client)

1. Thêm thư viện `supabase-kt` (module postgrest và auth) và serializer phù hợp. Kiểm tra phiên bản mới nhất khi làm.
2. URL và anon key đọc từ `local.properties` qua `BuildConfig`, **không commit** vào git. **Tuyệt đối không dùng `service_role` key trong app.**
3. Tạo DTO cho từng bảng (tên trường snake_case khớp cột Postgres) và hàm chuyển đổi Entity ↔ DTO.
4. Lớp `RemoteDataSource` có: `upsert(list)` và `fetchChangedSince(cursor, pageSize)` cho từng bảng.

**Xong khi:** gọi được một lần upsert và một lần fetch thử trên project Supabase thật.

## Bước 4 – Đăng nhập

1. Màn hình đăng nhập/đăng xuất trong **Cài đặt** (email + mật khẩu hoặc magic link). Cần để Row Level Security biết dữ liệu của ai.
2. Lưu session an toàn (thư viện auth đã hỗ trợ), tự làm mới token.
3. Chưa đăng nhập thì app chạy offline như cũ, không sync, không báo lỗi.

**Xong khi:** đăng nhập xong mọi request tới Supabase mang theo user, đăng xuất thì dừng sync.

## Bước 5 – SyncEngine (phần quan trọng nhất)

Chạy theo thứ tự bảng: `boards` → `events` → `reminders` → `event_completions` (cha trước, con sau).

**Push (đẩy lên):**
1. Lấy các dòng `dirty = true` theo lô (ví dụ 100 dòng).
2. Upsert lên server (khoá chính là UUID, nên chạy lại nhiều lần vẫn an toàn). Dòng xoá mềm cũng được đẩy, mang theo `deletedAt`.
3. Chỉ đặt `dirty = false` cho những dòng đã được server xác nhận, và chỉ khi `updatedAt` của dòng đó chưa đổi trong lúc đang đẩy (tránh mất thay đổi xảy ra giữa chừng).

**Pull (kéo về):**
1. Lưu `lastPulledAt` riêng cho từng bảng (DataStore).
2. Lấy các dòng server có `server_updated_at > lastPulledAt`, sắp xếp tăng dần, phân trang.
3. Với mỗi dòng nhận về:
   - Chưa có ở local → chèn.
   - Có ở local và `dirty = false` → ghi đè bằng bản server.
   - Có ở local và `dirty = true` (xung đột) → **bản có `updatedAt` mới hơn thắng.** Nếu bản local thắng, giữ nguyên và để lần push tiếp theo đẩy lên.
4. Cập nhật `lastPulledAt` bằng `server_updated_at` lớn nhất đã nhận (dùng đồng hồ server, không dùng đồng hồ điện thoại, để tránh lệch giờ).
5. Pull xong phải dùng con trỏ đã lưu; nếu giữa chừng lỗi mạng thì lần sau tiếp tục, không mất.

**Quy tắc chung:**
- Lỗi mạng hoặc 429: thử lại với backoff, không crash.
- Mỗi lần sync ghi log gọn: số dòng đẩy, số dòng kéo, số xung đột, lỗi.
- Áp dụng dữ liệu kéo về trong một transaction Room.

**Xong khi:** thêm, sửa, xoá ở app xuất hiện trên server; chỉnh trực tiếp trên server thì app nhận được; chạy sync hai lần liên tiếp không sinh thay đổi thừa.

## Bước 6 – Khi nào chạy sync

1. **WorkManager** định kỳ (ví dụ mỗi 15 phút, ràng buộc có mạng).
2. Sync ngay (có debounce vài giây) sau khi người dùng ghi dữ liệu.
3. Sync khi mở app hoặc khi app quay lại foreground.
4. Nút **"Đồng bộ ngay"** trong Cài đặt, kèm hiển thị "lần đồng bộ cuối", trạng thái (đang chạy / lỗi / ok) và số thay đổi đang chờ đẩy.
5. (Để sau, không bắt buộc) Supabase Realtime để nhận thay đổi từ agent tức thì.

## Bước 7 – Chuông báo, thông báo và widget sau khi sync

Đây là chỗ dễ sót nhất: sự kiện do agent thêm hoặc sửa phải thực sự kêu chuông.

1. Sau mỗi lần pull có thay đổi: với event mới hoặc đổi giờ/nhắc → **đặt lại AlarmManager**; với event bị xoá mềm → **huỷ chuông** tương ứng.
2. Làm mới cả 3 widget (2x2, 4x2, 2x1) sau khi pull.
3. Sau khi máy khởi động lại hoặc khi app cập nhật, kiểm tra luồng đặt lại chuông có đọc từ dữ liệu đã sync không.

**Xong khi:** thêm sự kiện từ server (mô phỏng agent) rồi mở app thì chuông được đặt và widget cập nhật.

## Bước 8 – Luồng AI và agent trong app

1. `AIScheduleContextHelper` và `answerDirectLocally` vẫn đọc Room (nhanh, dùng được offline).
2. Luồng "AI thêm vào lịch trình" ghi qua Repository, nên tự được sync.
3. Giữ nguyên nguyên tắc: AI chỉ đề xuất, người dùng xác nhận rồi mới ghi. Thao tác xoá hoặc dời lịch từ agent bên ngoài nên cần xác nhận.
4. Cập nhật lại mô tả riêng tư trong app và trong `project_state.md`: dữ liệu lịch **có lưu trên server Supabase** của người dùng và được gửi cho nhà cung cấp AI khi hỏi AI. Bỏ cụm "bảo mật tuyệt đối trên thiết bị".

## Bước 9 – Bảo mật và dọn dẹp

1. Build bản dùng thật với `debuggable = false`.
2. API key của Gemini/Groq nên lưu bằng EncryptedSharedPreferences hoặc Keystore, không để dạng thô.
3. Kiểm tra repo không chứa key Supabase hay key AI (`git grep`).
4. Không log nội dung lịch hoặc token ra Logcat ở bản release.

## Bước 10 – Nghiệm thu

Chạy lần lượt và ghi kết quả:
1. Cài app lên dữ liệu cũ, nâng cấp, không mất dữ liệu.
2. Thêm sự kiện offline, bật mạng, nó lên server.
3. Sửa sự kiện trên server, app nhận đúng.
4. Xoá sự kiện ở app, server đánh dấu xoá, app thứ hai (nếu có) cũng ẩn.
5. Cùng sửa một sự kiện ở hai nơi, bản mới hơn thắng, không mất dữ liệu ngoài dự kiến.
6. Sync hai lần liên tiếp không tạo thay đổi mới.
7. Sự kiện do server thêm vẫn kêu chuông đúng giờ.
8. Đăng xuất thì app vẫn dùng offline bình thường.
9. Cài lại app trên máy mới, đăng nhập, kéo toàn bộ dữ liệu về, không nhân đôi 10 danh mục.

---

## Giao ước với phía Supabase (để hai bên khớp nhau)

Người dùng sẽ tạo phần này; Claude Code phía app chỉ cần các điểm sau để code khớp:

- Mỗi bảng sync có cột: `id uuid primary key`, `user_id uuid` (mặc định `auth.uid()`), `updated_at`, `deleted_at`, và **`server_updated_at timestamptz` do trigger tự đặt bằng `now()` mỗi lần insert/update** (dùng làm con trỏ pull).
- Tên cột snake_case, ví dụ `start_time`, `board_id`, `is_completed`.
- Bật Row Level Security: user chỉ đọc và ghi được dòng có `user_id = auth.uid()`.
- Agent bên ngoài dùng một token hoặc user riêng, tách quyền đọc và ghi.
- Phần migration phía Supabase do người dùng nhờ Claude Code viết riêng, sau khi phần app ở đây ổn.
