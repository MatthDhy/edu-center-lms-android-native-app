# CeLeHub backend — bản vá theo CELEHUB_SCREEN_FLOWS.md (V5)

Giải nén đè vào repo (giữ nguyên đường dẫn). Sau đó làm 4 chỉnh sửa tay nhỏ bên dưới.

## A. Sửa tay (4 chỗ)

### 1. `backend/pom.xml` — thêm Apache POI (đọc file Excel quiz)
```xml
<dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.3.0</version></dependency>
```

### 2. `AssignmentController.java`
- `create(...)`: thêm tham số trước `file` và truyền vào service:
```java
@RequestParam(defaultValue = "true") boolean graded,
...
assignmentService.create(me, courseId, title, description, deadline, maxScore, graded, file)
```
- `mine(...)`: thêm `@RequestParam(required = false) String type` và gọi `assignmentService.mine(me, courseId, status, type)`.

### 3. `notification/ReminderService.java` — hàm `remindGrading()`: thêm điều kiện vào SQL
```sql
WHERE s.score IS NULL AND a.graded = TRUE
```

### 4. Web Admin (gửi thông báo kèm ảnh)
`src/api/client.js`:
```js
broadcast: (data) => data instanceof FormData
  ? apiClient.post('/api/admin/notifications/broadcast', data, { headers: { 'Content-Type': 'multipart/form-data' } })
  : apiClient.post('/api/admin/notifications/broadcast', data),
```
`BroadcastPage.jsx`: thêm `const [image, setImage] = useState(null)`, ô `<input type="file" accept="image/*" onChange={e=>setImage(e.target.files?.[0]??null)} />`;
trong `send()` nếu có ảnh thì dựng `FormData` (title, content, target, courseId, image) rồi gọi `notificationApi.broadcast(fd)`, không có ảnh giữ JSON như cũ.

## B. File trong gói
- Mới: `V5__ui_revision.sql`, `NotificationType`, toàn bộ package `quiz/` (Quiz, Question, QuestionOption, QuizAttempt, StudentAnswer, repo, `QuizDtos`, `QuizExcelService`, `QuizService`, `QuizController`, `QuizAutoSubmitJob`), package `task/`.
- Thay thế: `Notification*`, `AdminNotificationController`, `Assignment`, `AssignmentDto`, `AssignmentService`, `AssignmentRepository`, `SubmissionRepository`, `UploadValidator`.

## C. API mới / đổi (Android dùng)
| API | Ghi chú |
|---|---|
| `GET /api/notifications?type=GENERAL\|CLASS` | item có thêm `type, courseId, imageUrl, refType, refId` |
| `GET /api/notifications/unread-count` | `{general, class, total, count}` |
| `PATCH /api/notifications/read-all?type=` | đọc hết theo loại (bỏ trống = tất cả) |
| `POST /api/admin/notifications/broadcast` | JSON như cũ, hoặc multipart (`title, content, target, courseId, image`) |
| `POST /api/courses/{id}/assignments` | thêm field multipart `graded` (mặc định true) |
| `GET /api/assignments/mine?type=ESSAY\|QUIZ\|ALL` | mặc định ESSAY (app cũ không vỡ). Quiz: `type=QUIZ`, `quizStatus`, `quizScore`, `exitCount`, `durationMinutes`, `attemptId`. Quiz đã nộp có `status=GRADED` (tab "Đã có điểm"), bỏ lỡ = `OVERDUE` |
| `PATCH /api/submissions/{id}/grade` | 409 nếu bài `graded=false` |
| `GET /api/teacher/tasks?courseId=` | danh sách gộp bài tập + quiz của GV |
| `GET /api/quizzes/template` | tải file Excel mẫu |
| `POST /api/courses/{id}/quizzes` (JSON) | tạo nháp: `title, description, durationMinutes, startTime, endTime, questions[{content, explanation, options[{content, correct}]}]` |
| `POST /api/courses/{id}/quizzes/import` (multipart) | `title, durationMinutes, startTime?, endTime?, file` (.xlsx), lỗi trả theo dòng |
| `GET /api/courses/{id}/quizzes`, `GET /api/quizzes/{id}/preview` | GV/Admin |
| `PUT /api/quizzes/{id}`, `PUT /api/quizzes/{id}/questions`, `DELETE /api/quizzes/{id}` | chỉ khi chưa ai làm |
| `POST /api/quizzes/{id}/publish` | cần ≥1 câu, có endTime; báo học viên |
| `GET /api/quizzes/{id}/results` | tóm tắt, phổ điểm (10 cột), từng HV (kèm `exitCount`, `autoSubmitted`), thống kê từng câu |
| `GET /api/quizzes/{id}/info` | HV: dữ liệu màn Lobby |
| `POST /api/quizzes/{id}/start` | bắt đầu / **tiếp tục** (mỗi lần gọi khi đang làm dở = +1 lần thoát). `finished=true` nếu đã bị tự nộp |
| `PUT /api/quiz-attempts/{id}/answers` | `{questionId, optionId}` (optionId null = bỏ chọn), lưu mỗi lần chọn |
| `POST /api/quiz-attempts/{id}/submit` | nộp & chấm (idempotent) |
| `GET /api/quiz-attempts/{id}/review` | xem lại từng câu, chỉ sau khi nộp |

Quy tắc quiz: 1 lần/HV (UNIQUE); `endsAt = min(bắt đầu + thời lượng, endTime)` do server tính; thoát tối đa 2 lần, lần thứ 3 tự nộp; job mỗi phút tự nộp bài quá giờ; mất mạng không tính là thoát (vì chỉ tăng khi app gọi `start`). Điểm = số câu đúng × 10 / tổng câu.
