# CeLeHub — Trạng thái Backend & Hợp đồng API (dành cho làm giao diện Android)
Cập nhật 30/10/2026 · Nhóm 4

**Ký hiệu:** ✅ đã có trong repo hiện tại · 🆕 có trong gói bản vá `celehub-gd2-gd5-notifications.zip` (cần áp dụng trước khi gọi) · ⬜ chưa làm.
Khi file này mâu thuẫn với `CELEHUB_MASTER_CONTEXT.md` thì **file này đúng hơn** (đã đối chiếu với code thật).

---
## 0. Cách dùng khi nhờ AI viết Android
Dán file này + nói: *“Làm màn [X] cho role [STUDENT/TEACHER], dùng API ở mục 4, model ở mục 5, Retrofit ở mục 6. Java + XML + Material3, màu tím #7B61F0, font Inter như các layout hiện có.”*
Màn nào backend chưa có (⬜) thì dùng **mock** theo mục 7, khi API xong chỉ đổi 1 chỗ gọi Retrofit.

---
## 1. Bảng tiến độ

| Tính năng | Backend | Web Admin | Android |
|---|---|---|---|
| Đăng nhập, ép đổi mật khẩu lần đầu | ✅ | ✅ (login) | ✅ |
| Quên mật khẩu (OTP) | ✅ | – | ⬜ 3 màn có UI nhưng **chưa gọi API** |
| Hồ sơ cá nhân, đổi mật khẩu, avatar | ✅ | – | ✅ đổi MK · ⬜ sửa hồ sơ, đổi avatar |
| Trang chủ Học viên / Giảng viên | ✅ | – | ✅ (1 `HomeFragment` dùng chung 2 role) |
| Thông báo (chuông, danh sách, đã đọc, xóa) | ✅ + 🆕 xóa | 🆕 gửi chung | ⬜ màn danh sách (tab còn Placeholder) |
| Quản lý tài khoản (tạo/khóa/reset) | ✅ | ✅ | – |
| **Xóa tài khoản** | 🆕 | 🆕 | – |
| Lớp học, buổi học, xếp học viên | 🆕 | 🆕 | ⬜ |
| Thời khóa biểu toàn trung tâm (Admin) | 🆕 | 🆕 | – |
| Lịch cá nhân (HV/GV) | 🆕 | – | ⬜ |
| Bài tập: giao, nộp, chấm, nhắc | 🆕 | 🆕 (chỉ xem) | ⬜ |
| Đăng ký thiết bị (FCM token) | 🆕 (chỉ lưu) | – | ⬜ |
| Push FCM thật | ⬜ | – | ⬜ |
| Điểm danh QR + PIN | ⬜ | ⬜ | ⬜ |
| Tài liệu bài giảng | ⬜ | – | ⬜ |
| Xin nghỉ / học bù | ⬜ | ⬜ | ⬜ |
| Quiz (Excel + LaTeX) | ⬜ | ⬜ | ⬜ |
| Học phí VietQR | ⬜ | ⬜ | ⬜ |
| Lương giảng viên | ⬜ | ⬜ | ⬜ |
| Import Excel học viên | ⬜ | ⬜ | – |
| Thống kê chuyên cần, dashboard doanh thu | ⬜ | ⬜ | ⬜ |

---
## 2. Quy ước chung
| Hạng mục | Quy ước |
|---|---|
| Base URL | Emulator `http://10.0.2.2:8080/` · máy thật `http://<IP-LAN>:8080/` (thêm IP vào `network_security_config.xml`) · production `https://...` |
| Header | `Authorization: Bearer <token>` (đã có `AuthInterceptor`) |
| Response | `{ "status": 200, "message": "...", "data": ... }`. Lỗi: `data = null`, đọc `message` (tiếng Việt, hiển thị thẳng cho người dùng) |
| JSON | camelCase. Gson bỏ qua field thừa nên thêm field sau không làm app hỏng |
| Thời gian | `"2026-11-02T18:00:00"` (giờ VN, **không múi giờ**). Tham số ngày: `2026-11-02` |
| Enum | VIẾT HOA (`STUDENT`, `PENDING`, `SCHEDULED`...) |
| Số | Tiền = số nguyên VNĐ → `long`. Điểm = số thập phân → `Double` (có thể null) |
| Phân trang | Chỉ thông báo: `?page=0&size=20` → `data:{content,page,size,totalElements,totalPages}`. Các danh sách khác trả mảng |
| Upload | `multipart/form-data`, tối đa 20MB. **Tên file phải có đuôi** (`bai_nop.jpg`) |
| Mật khẩu | ≥ 8 ký tự, có chữ hoa và chữ thường (Android kiểm tra, backend chỉ kiểm tra độ dài) |

### Mã lỗi & cách xử lý trên app
| Mã | Ý nghĩa | App làm gì |
|---|---|---|
| 400 | Sai dữ liệu | Hiện `message` ở ô nhập / Toast |
| 401 | Token hết hạn, tài khoản bị khóa / **sai mật khẩu khi login** | Đã có: xóa phiên → về Login (trừ chính API login) |
| 403 | Sai quyền · không thuộc lớp · **chưa đổi mật khẩu** · tài khoản bị khóa lúc login | Nếu `message` chứa “đổi mật khẩu” → mở màn ép đổi (⬜ chưa xử lý ở interceptor); còn lại Toast |
| 404 | Không tìm thấy | Toast |
| 409 | Xung đột (trùng lịch, bài đã chấm không nộp lại được, lớp đã đóng) | Toast `message` |
| 413 | File quá lớn | Toast |

---
## 3. Luồng xác thực
```
Splash → có token? ── không → Login
                   └─ có → mustChangePassword? ── true → ResetPasswordActivity(MODE_FIRST_LOGIN)
                                               └─ false → MainActivity (Fragment theo role)
Login: POST /api/auth/login → lưu token/role/mustChangePassword (SessionManager.saveLogin)
        role = ADMIN → chặn (dùng Web Admin). STUDENT/TEACHER → đi tiếp  ✅ đã làm
Ép đổi: POST /api/auth/force-change-password {newPassword} → setMustChangePassword(false) → MainActivity ✅
Quên MK: ForgotPassword(email) → request → OtpActivity(6 số) → verify → resetToken → ResetPassword(mode forgot) → reset → Login  ⬜ chưa nối
```
Sau khi đăng nhập xong (và có FCM): `POST /api/devices {fcmToken}`; khi đăng xuất: `DELETE /api/devices?token=...` rồi xóa phiên. 🆕

---
## 4. API theo màn hình

### 4.1 Dùng chung (STUDENT + TEACHER)
| Màn / hành động | Method + Endpoint | Body / Query | `data` |
|---|---|---|---|
| Đăng nhập ✅ | POST `/api/auth/login` | `{email,password}` | `AuthResponse` |
| Ép đổi MK lần đầu ✅ | POST `/api/auth/force-change-password` | `{newPassword}` | null |
| Đổi mật khẩu ✅ | POST `/api/auth/change-password` | `{oldPassword,newPassword}` | null |
| Đăng xuất ✅ | POST `/api/auth/logout` | – | null (no-op; app tự xóa token) |
| Quên MK bước 1 ✅ | POST `/api/auth/forgot-password/request` | `{email}` | null (luôn 200) |
| Quên MK bước 2 ✅ | POST `/api/auth/forgot-password/verify` | `{email,otp}` | `{resetToken}` (OTP sống 10', sai tối đa 5 lần) |
| Quên MK bước 3 ✅ | POST `/api/auth/forgot-password/reset` | `{resetToken,newPassword}` | null |
| Thông tin tôi ✅ | GET `/api/users/me` | – | `UserDto` |
| Hồ sơ + số liệu ✅ | GET `/api/users/me/profile` | – | `ProfileResponse` |
| Sửa hồ sơ ✅ | PATCH `/api/users/me` | `{fullName?,phone?}` (`phone:""` = xóa) | `UserDto` |
| Đổi avatar ✅ | POST `/api/users/me/avatar` | multipart `file` (jpg/png/webp ≤ 5MB) | `UserDto` |
| Xóa avatar ✅ | DELETE `/api/users/me/avatar` | – | `UserDto` |
| Chuông: số chưa đọc ✅ | GET `/api/notifications/unread-count` | – | `{count}` |
| Danh sách thông báo ✅ | GET `/api/notifications` | `page,size,unreadOnly` | `PageResponse<NotificationDto>` |
| Đánh dấu đã đọc ✅ | PATCH `/api/notifications/{id}/read` | – | null |
| Đọc hết ✅ | PATCH `/api/notifications/read-all` | – | `{updated}` |
| Xóa thông báo 🆕 | DELETE `/api/notifications/{id}` | – | null |
| Đăng ký thiết bị 🆕 | POST `/api/devices` | `{fcmToken}` | null |
| Hủy thiết bị 🆕 | DELETE `/api/devices` | `?token=` | null |

### 4.2 Học viên (STUDENT)
| Tab / màn | Endpoint | Ghi chú |
|---|---|---|
| **Trang chủ** ✅ | GET `/api/home/student` | `HomeStudentResponse`: `nextLesson` (null → ẩn), `attendance{attended,held,rate}`, `upcomingTasks[]`, `recentNotifications[]` |
| **Khóa học** — danh sách 🆕 | GET `/api/courses/mine` | `CourseDto[]` (lớp đang học) |
| Chi tiết lớp 🆕 | GET `/api/courses/{id}` | tab Tổng quan |
| Lịch trình của lớp 🆕 | GET `/api/courses/{id}/lessons` | tab Lịch trình |
| Lịch tuần/tháng 🆕 | GET `/api/lessons?from=2026-11-01&to=2026-11-30` | tối đa 62 ngày; có cả buổi `CANCELED` (hiện nhãn “Đã hủy”) |
| Bài tập của lớp 🆕 | GET `/api/courses/{id}/assignments` | tab Bài tập trong chi tiết lớp |
| **Bài tập** — tab Cần làm 🆕 | GET `/api/assignments/mine?status=TODO` | sắp hạn gần nhất lên đầu; `status` = `PENDING` hoặc `OVERDUE` |
| tab Đã nộp 🆕 | GET `/api/assignments/mine?status=SUBMITTED` | chờ chấm |
| tab Đã có điểm 🆕 | GET `/api/assignments/mine?status=GRADED` | |
| Lọc theo môn 🆕 | thêm `&courseId=` | lấy danh sách lớp từ `/api/courses/mine` |
| Chi tiết bài 🆕 | GET `/api/assignments/{id}` | có `mySubmission` (điểm, nhận xét, `fileUrl`) |
| **Nộp bài** 🆕 | POST `/api/assignments/{id}/submit` | multipart `file` (pdf/doc/docx/jpg/png/webp/heic). Quá hạn vẫn nhận, `late=true`. Chưa chấm thì nộp lại được; đã chấm → 409 |
| Mở đề / file bài nộp | dùng `attachmentUrl` / `fileUrl` (link CDN công khai) → Glide hoặc `Intent.ACTION_VIEW` | |

### 4.3 Giảng viên (TEACHER)
| Tab / màn | Endpoint | Ghi chú |
|---|---|---|
| **Trang chủ** ✅ | GET `/api/home/teacher` | `nextLesson`, `todayLessons[]`, `pendingGradingCount`, `pendingLeaveCount`, `lessonsThisMonth`, `recentSubmissions[]`, `recentNotifications[]` |
| **Lịch dạy & Lớp** — lớp phụ trách 🆕 | GET `/api/courses/mine` | |
| Lịch dạy 🆕 | GET `/api/lessons?from&to` | |
| Sĩ số / danh sách HV 🆕 | GET `/api/courses/{id}/students` | `EnrolledStudentDto[]` |
| **Bài tập** — danh sách theo lớp 🆕 | GET `/api/courses/{id}/assignments` | mỗi bài có `submittedCount`, `gradedCount`, `totalStudents` |
| Giao bài mới 🆕 | POST `/api/courses/{courseId}/assignments` | multipart: `title`, `description?`, `deadline`, `maxScore?`, `file?` |
| Sửa bài 🆕 | PUT `/api/assignments/{id}` | JSON `{title?,description?,deadline?,maxScore?}` |
| Đổi / xóa file đề 🆕 | POST / DELETE `/api/assignments/{id}/attachment` | |
| Xóa bài 🆕 | DELETE `/api/assignments/{id}` | xóa luôn bài nộp |
| **Đã nộp / Chưa nộp** 🆕 | GET `/api/assignments/{id}/submissions` | `{assignment, submitted[], notSubmitted[]}` |
| Xem 1 bài nộp 🆕 | GET `/api/submissions/{id}` | |
| **Chấm điểm** 🆕 | PATCH `/api/submissions/{id}/grade` | `{score, feedback?}`, 0 ≤ score ≤ `maxScore`; HV nhận thông báo |
| **Nhắc tất cả** 🆕 | POST `/api/assignments/{id}/remind-all` | `{reminded:n}`, chỉ HV chưa nộp |

---
## 5. Mẫu JSON (đặt tên field Java đúng như dưới đây)
```jsonc
// AuthResponse ✅
{ "token":"eyJ...", "role":"STUDENT", "mustChangePassword":false,
  "user":{ "id":3,"fullName":"Nguyễn Minh Nhật","email":"nhat@gmail.com","phone":"0912345678","avatarUrl":null,
           "role":"STUDENT","status":"ACTIVE","mustChangePassword":false,"createdAt":"2026-10-18T09:00:00" } }

// CourseDto 🆕
{ "id":1,"courseName":"Toán 11A","description":null,"teacherId":2,"teacherName":"Nguyễn Thu Hà",
  "tuitionFee":1500000,"totalSessions":12,"status":"ACTIVE","studentCount":20,"lessonCount":12,"heldCount":3,
  "createdAt":"2026-10-30T08:00:00" }

// LessonDto 🆕   status: SCHEDULED | DONE | CANCELED
{ "id":55,"courseId":1,"courseName":"Toán 11A","teacherId":2,"teacherName":"Nguyễn Thu Hà","room":"P.301",
  "topic":null,"startTime":"2026-11-02T18:00:00","endTime":"2026-11-02T19:30:00","status":"SCHEDULED" }

// EnrolledStudentDto 🆕
{ "studentId":3,"fullName":"Nguyễn Minh Nhật","email":"nhat@gmail.com","phone":"0912345678","avatarUrl":null,"joinedAt":"2026-10-30T09:00:00" }

// AssignmentDto 🆕  (HV: status + mySubmission; GV: 3 số đếm)  HV status: PENDING|OVERDUE|SUBMITTED|GRADED
{ "id":7,"type":"ESSAY","courseId":1,"courseName":"Toán 11A","title":"Hình học tuần 1","description":"Làm bài 1-5",
  "attachmentUrl":"https://xxx.supabase.co/storage/v1/object/public/celehub/assignments/uuid.pdf",
  "deadline":"2026-11-10T23:59:00","maxScore":10.00,"createdAt":"2026-11-02T10:00:00","minutesLeft":8640,
  "status":"SUBMITTED",
  "mySubmission":{ /* SubmissionDto */ },
  "submittedCount":null,"gradedCount":null,"totalStudents":null }

// SubmissionDto 🆕   status: SUBMITTED | GRADED
{ "id":12,"assignmentId":7,"studentId":3,"studentName":"Nguyễn Minh Nhật","studentEmail":"nhat@gmail.com","studentAvatarUrl":null,
  "fileUrl":"https://.../submissions/uuid.jpg","submittedAt":"2026-11-03T20:15:00","late":false,
  "score":8.50,"feedback":"Làm tốt","gradedAt":"2026-11-04T09:00:00","status":"GRADED" }

// SubmissionsOverviewDto 🆕
{ "assignment":{ /* AssignmentDto bản GV */ }, "submitted":[ /* SubmissionDto */ ],
  "notSubmitted":[ { "id":4,"fullName":"Lê Thị B","email":"b@gmail.com","phone":null,"avatarUrl":null } ] }

// NotificationDto ✅
{ "id":9,"title":"Bài tập mới: Hình học tuần 1","content":"Toán 11A - hạn nộp 23:59 10/11/2026","read":false,"createdAt":"2026-11-02T10:00:00" }

// ProfileResponse ✅  (role không liên quan = null)
{ "user":{...}, "student":{ "activeCourses":2,"attended":14,"held":15,"attendanceRate":93.3 }, "teacher":null }
```
**Gson:** dùng `Long`, `Double`, `Integer` (không dùng kiểu nguyên thủy) cho field có thể `null` (`score`, `minutesLeft`, `attendance.rate`, `totalSessions`, `studentCount`...). `Boolean` field tên `read`/`late` → getter `isRead()`/`isLate()`.

### Thông báo tự sinh (để biết khi nào chuông có số mới) 🆕
Được phân công lớp (GV) · xếp / rút khỏi lớp (HV) · hủy / đổi giờ / đổi phòng buổi học (HV) · giao bài mới, đổi hạn nộp, nhắc nộp, có điểm (HV) · có bài nộp mới (GV) · admin gửi thông báo chung.

---
## 6. Retrofit `ApiService` (copy – gồm API đã có và API 🆕)
```java
public interface ApiService {
    // ---- Auth ✅ ----
    @POST("api/auth/login") Call<ApiResponse<AuthResponse>> login(@Body LoginRequest r);
    @POST("api/auth/force-change-password") Call<ApiResponse<Void>> forceChangePassword(@Body ForceChangePasswordRequest r);
    @POST("api/auth/change-password") Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequest r);
    @POST("api/auth/logout") Call<ApiResponse<Void>> logout();
    @POST("api/auth/forgot-password/request") Call<ApiResponse<Void>> forgotRequest(@Body ForgotPasswordRequest r);   // {email}
    @POST("api/auth/forgot-password/verify")  Call<ApiResponse<VerifyOtpResponse>> forgotVerify(@Body VerifyOtpRequest r); // {email,otp} → {resetToken}
    @POST("api/auth/forgot-password/reset")   Call<ApiResponse<Void>> forgotReset(@Body ResetPasswordRequest r);     // {resetToken,newPassword}

    // ---- Cá nhân ✅ ----
    @GET("api/users/me") Call<ApiResponse<UserDto>> getMe();
    @GET("api/users/me/profile") Call<ApiResponse<ProfileResponse>> getProfile();
    @PATCH("api/users/me") Call<ApiResponse<UserDto>> updateMe(@Body UpdateProfileRequest r);          // {fullName?,phone?}
    @Multipart @POST("api/users/me/avatar") Call<ApiResponse<UserDto>> uploadAvatar(@Part MultipartBody.Part file);
    @DELETE("api/users/me/avatar") Call<ApiResponse<UserDto>> deleteAvatar();

    // ---- Trang chủ ✅ ----
    @GET("api/home/student") Call<ApiResponse<HomeStudentResponse>> getHomeStudent();
    @GET("api/home/teacher") Call<ApiResponse<HomeTeacherResponse>> getHomeTeacher();

    // ---- Thông báo ✅ + 🆕 ----
    @GET("api/notifications") Call<ApiResponse<PageResponse<NotificationItem>>> notifications(
            @Query("page") int page, @Query("size") int size, @Query("unreadOnly") boolean unreadOnly);
    @GET("api/notifications/unread-count") Call<ApiResponse<Map<String, Long>>> unreadCount();
    @PATCH("api/notifications/{id}/read") Call<ApiResponse<Void>> markRead(@Path("id") long id);
    @PATCH("api/notifications/read-all") Call<ApiResponse<Map<String, Integer>>> markAllRead();
    @DELETE("api/notifications/{id}") Call<ApiResponse<Void>> deleteNotification(@Path("id") long id);   // 🆕
    @POST("api/devices") Call<ApiResponse<Void>> registerDevice(@Body RegisterDeviceRequest r);          // 🆕 {fcmToken}
    @DELETE("api/devices") Call<ApiResponse<Void>> unregisterDevice(@Query("token") String token);       // 🆕

    // ---- Khóa học & lịch 🆕 ----
    @GET("api/courses/mine") Call<ApiResponse<List<CourseDto>>> myCourses();
    @GET("api/courses/{id}") Call<ApiResponse<CourseDto>> course(@Path("id") long id);
    @GET("api/courses/{id}/lessons") Call<ApiResponse<List<LessonDto>>> courseLessons(@Path("id") long id);
    @GET("api/courses/{id}/students") Call<ApiResponse<List<EnrolledStudentDto>>> courseStudents(@Path("id") long id); // GV
    @GET("api/lessons") Call<ApiResponse<List<LessonDto>>> schedule(@Query("from") String from, @Query("to") String to); // yyyy-MM-dd

    // ---- Bài tập 🆕 ----
    @GET("api/courses/{id}/assignments") Call<ApiResponse<List<AssignmentDto>>> courseAssignments(@Path("id") long courseId);
    @GET("api/assignments/mine") Call<ApiResponse<List<AssignmentDto>>> myAssignments(      // HV
            @Query("courseId") Long courseId, @Query("status") String status);                 // status: TODO|SUBMITTED|GRADED (null = tất cả)
    @GET("api/assignments/{id}") Call<ApiResponse<AssignmentDto>> assignment(@Path("id") long id);

    @Multipart @POST("api/assignments/{id}/submit")                                            // HV nộp bài
    Call<ApiResponse<SubmissionDto>> submit(@Path("id") long id, @Part MultipartBody.Part file);
    // file: MultipartBody.Part.createFormData("file", "bai_nop.jpg", RequestBody.create(bytes, MediaType.parse("image/jpeg")))

    @Multipart @POST("api/courses/{courseId}/assignments")                                     // GV giao bài (truyền null để bỏ qua part tùy chọn)
    Call<ApiResponse<AssignmentDto>> createAssignment(@Path("courseId") long courseId,
            @Part("title") RequestBody title, @Part("description") RequestBody description,
            @Part("deadline") RequestBody deadline,            // "2026-11-10T23:59:00"
            @Part("maxScore") RequestBody maxScore, @Part MultipartBody.Part file);
    @PUT("api/assignments/{id}") Call<ApiResponse<AssignmentDto>> updateAssignment(@Path("id") long id, @Body UpdateAssignmentRequest r);
    @Multipart @POST("api/assignments/{id}/attachment") Call<ApiResponse<AssignmentDto>> replaceAttachment(@Path("id") long id, @Part MultipartBody.Part file);
    @DELETE("api/assignments/{id}/attachment") Call<ApiResponse<AssignmentDto>> removeAttachment(@Path("id") long id);
    @DELETE("api/assignments/{id}") Call<ApiResponse<Void>> deleteAssignment(@Path("id") long id);

    @GET("api/assignments/{id}/submissions") Call<ApiResponse<SubmissionsOverviewDto>> submissions(@Path("id") long id); // GV
    @GET("api/submissions/{id}") Call<ApiResponse<SubmissionDto>> submission(@Path("id") long id);
    @PATCH("api/submissions/{id}/grade") Call<ApiResponse<SubmissionDto>> grade(@Path("id") long id, @Body GradeRequest r); // {score,feedback}
    @POST("api/assignments/{id}/remind-all") Call<ApiResponse<Map<String, Integer>>> remindAll(@Path("id") long id);
}
```
Ghi chú Retrofit: dùng `Long courseId` / `String status` (không phải `long`) để truyền `null` thì Retrofit bỏ query đó.

---
## 7. Backend CÒN THIẾU — API dự kiến để Android dựng UI bằng mock trước
(Hợp đồng sơ bộ, theo `MASTER_CONTEXT` mục 8; sẽ chốt chính thức khi làm giai đoạn đó.)

| Giai đoạn | Việc | Endpoint dự kiến | JSON dự kiến |
|---|---|---|---|
| **GĐ3 Điểm danh** | GV tạo phiên | POST `/api/attendance/sessions` `{courseId,lessonId?,durationMinutes}` | `{sessionId,qrToken,pinCode:"849201",expiresAt}` |
| | GV xem realtime (polling 3s) | GET `/api/attendance/sessions/{id}/live` | `[{studentId,fullName,status:"PRESENT\|ABSENT\|LATE\|EXCUSED",method:"QR\|PIN\|MANUAL"}]` |
| | HV quét / nhập PIN | POST `/api/attendance/check-in` `{qrToken}` hoặc `{pinCode}` | lỗi: hết hạn / sai lớp / đã điểm danh |
| | GV sửa tay, đóng phiên | PATCH `/api/attendance/records`, POST `.../sessions/{id}/close` | |
| **GĐ4** | Tài liệu | GET/POST `/api/courses/{id}/materials`, DELETE `/api/materials/{id}` | `{id,title,description,fileUrl,createdAt}` |
| | Xin nghỉ & học bù | GET `/api/leave-requests/suggestions?lessonId=`, POST `/api/leave-requests` `{lessonId,reason,makeupLessonId?}`, GET `/mine`, GV: GET `/api/teacher/leave-requests?status=PENDING`, PATCH `/{id}/approve` `/reject` | |
| **GĐ6 Quiz** | Tạo/import Excel, xuất bản, làm bài, chấm, phổ điểm | `/api/quizzes/...` (template, import, preview, publish, start, submit, result, stats) | `start` không trả đáp án đúng |
| **GĐ7** | Học phí | GET `/api/tuition/bills`, GET `/api/tuition/bills/{id}/vietqr` | `{billId,courseName,amount,bankName,accountNumber,accountName,transferContent,qrImageUrl}` |
| | Lương GV | GET `/api/payroll?month=2026-10` | tổng + từng ca (giờ × đơn giá) |
| | Thống kê chuyên cần | GET `/api/attendance/stats?courseId=` | ma trận HV × buổi |
| **Khác** | Import Excel học viên | POST `/api/admin/users/import` | |
| | Push FCM thật | cần `firebase-admin` + `google-services.json` | |
| | Chống brute-force login; vô hiệu token khi đổi MK | | |

Trong lúc chờ: tab **Điểm danh / Học phí / Quiz / Lương** trên Home vẫn để Toast như hiện tại; chỉ làm UI + mock.

---
## 8. Android — việc còn thiếu / cần chỉnh (đối chiếu code hiện tại)
1. `ApiService` mới có 6 hàm → thêm theo mục 6, tạo Model Java tương ứng (mục 5).
2. **Bottom nav**: `bottom_nav_menu.xml` đang 5 mục (có “Thông báo”) — đã chốt **4 tab**: Trang chủ · Khóa học · Bài tập · Cá nhân; chuông ở header Home (`btnNotification`) mở màn danh sách thông báo.
3. `MainActivity.fragmentFor`: Khóa học / Bài tập / Thông báo đang là `PlaceholderFragment` → thay bằng Fragment thật; **cùng 1 tab, 2 role**: chọn Fragment theo `SessionManager.getRole()`.
4. Quên mật khẩu: `ForgotPasswordActivity` gọi `forgotRequest` rồi mở `OtpActivity`; `OtpActivity` gọi `forgotVerify` và **truyền `resetToken` qua Intent**; `ResetPasswordActivity` (mode forgot) gọi `forgotReset`. Hiện cả 3 chỉ là UI giả.
5. `EditProfileFragment`: `UPDATE_SUPPORTED = false` đã lỗi thời — backend đã có `PATCH /api/users/me`; nút “Đổi ảnh đại diện” → `uploadAvatar`. Khi `UPDATE_SUPPORTED=false` thì nút Lưu vẫn đang hiện (nên ẩn).
6. `ProfileFragment`: dòng `getDisplayCode() + " • Học viên"` và `getInitials()` đang cố định theo học viên → với TEACHER hiện “Giảng viên”, bỏ mã `HV0001`.
7. `AuthInterceptor` chỉ xử lý 401. Thêm: 403 có `message` chứa “đổi mật khẩu” → mở `ResetPasswordActivity(MODE_FIRST_LOGIN)`.
8. `HomeTeacherResponse` chưa khai báo `todayLessons`, `pendingGradingCount`, `pendingLeaveCount`, `lessonsThisMonth`, `recentSubmissions` — backend đã trả, thêm field để dựng widget “Công việc cần xử lý” cho GV.
9. `HomeStudentResponse` chưa khai báo `attendance{attended,held,rate}` — dùng cho khối “Tiến độ chuyên cần”.
10. Đếm ngược “Bắt đầu sau N phút”: server chỉ trả `minutesUntilStart` lúc gọi API → tự tính lại từ `startTime` bằng đồng hồ máy.
11. Avatar: nếu `avatarUrl != null` dùng Glide (thêm dependency), null thì giữ chữ cái đầu.
12. Chụp ảnh nộp bài: xin quyền Camera, lưu JPEG, đặt tên `bai_nop.jpg`, nén dưới 20MB.
13. Sau khi dùng HTTPS: bỏ khối cleartext trong `network_security_config.xml`.

---
## 9. Dữ liệu test nhanh (không cần SQL)
1. Chạy backend → đăng nhập Web Admin (`admin@celehub.edu.vn`).
2. **Giảng viên**: tạo 1 tài khoản TEACHER; **Học viên**: tạo 2–3 tài khoản STUDENT (mật khẩu tạm xem trong mail / `docker compose logs backend` nếu `MAIL_ENABLED=false`).
3. **Lớp học → Tạo lớp** → chọn giảng viên → vào lớp → **Sinh lịch lặp** (ví dụ T2 + T4, 18:00–19:30, 12 buổi) → **Xếp học viên**.
4. Đăng nhập app bằng GV: giao 1 bài tập; bằng HV: nộp bài; GV: chấm điểm → HV thấy điểm + chuông có thông báo.
5. Web Admin → **Gửi thông báo** để thử chuông.
Muốn thấy “Ca học tiếp theo” ngay: tạo lớp với buổi bắt đầu trong vài chục phút tới.

---
## 10. Lỗi thường gặp
| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Web Admin login báo **403** (không có `message` tiếng Việt, chỉ “Request failed with status code 403”) | Spring từ chối CORS: web mở bằng địa chỉ không nằm trong danh sách (vd `127.0.0.1`, IP LAN, domain) | Dùng bản vá nginx (bỏ header `Origin`) và `CORS_ORIGINS`; hoặc mở bằng `http://localhost:3000` |
| Login báo 403 kèm “Tài khoản đã bị khóa” | Tài khoản `LOCKED` | Mở khóa ở Web Admin |
| Mọi API trả 403 “Bắt buộc đổi mật khẩu” | Chưa đổi mật khẩu lần đầu | Gọi `force-change-password` |
| 401 sau khi đang dùng | Token hết hạn (7 ngày) hoặc tài khoản bị khóa | App tự về Login |
| Upload ảnh báo 413 | nginx giới hạn 1MB mặc định | Dùng `nginx.conf` mới (`client_max_body_size 25m`) |
| Upload báo 400 “Định dạng tệp không được hỗ trợ” | Tên file không có đuôi / sai đuôi | Đặt `bai_nop.jpg` |
| Xóa tài khoản báo 409 | Tài khoản đã có lớp / bài nộp / điểm danh | Dùng **Khóa** thay vì xóa |
| Tạo lịch báo 409 “trùng lịch” | GV hoặc phòng đã bận | Đổi giờ/phòng; thông báo ghi rõ buổi bị trùng |
