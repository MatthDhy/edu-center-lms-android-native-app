# API: Khóa học (GĐ2) · Bài tập (GĐ5) · Thông báo — hợp đồng cho Android / Web
Khuôn chung `{status,message,data}`, JSON camelCase, thời gian `2026-10-30T23:59:00` (giờ VN), tiền = số nguyên VNĐ.
Mã lỗi: 400 sai dữ liệu · 401 chưa đăng nhập · 403 sai quyền / chưa đổi mật khẩu / không thuộc lớp · 404 · 409 trùng lịch hoặc sai trạng thái · 413 file lớn.

## 1. ADMIN — Lớp học (`/api/admin/**`, chỉ ADMIN)
| Method | Endpoint | Body / Query | Kết quả |
|---|---|---|---|
| GET | `/api/admin/courses?status=&teacherId=&search=` | | `CourseDto[]` |
| POST | `/api/admin/courses` | `{courseName, description?, teacherId, tuitionFee?, totalSessions?}` | `CourseDto` |
| GET | `/api/admin/courses/{id}` | | `CourseDto` |
| PUT | `/api/admin/courses/{id}` | `{courseName?, description?, teacherId?, tuitionFee?, totalSessions?, status?(ACTIVE\|CLOSED)}` | `CourseDto` |
| GET | `/api/admin/courses/{id}/lessons` | | `LessonDto[]` |
| POST | `/api/admin/courses/{id}/lessons` | `{startTime, endTime, room?, topic?}` | `LessonDto` (409 nếu trùng GV/phòng) |
| POST | `/api/admin/courses/{id}/lessons/generate` | `{startDate:"2026-10-26", daysOfWeek:[2,4], startTime:"18:00", endTime:"19:30", room?, topic?, sessions:12}` (1=T2…7=CN) | `LessonDto[]` (1 buổi trùng là hủy cả đợt, 409) |
| PUT | `/api/admin/lessons/{lessonId}` | `{startTime?, endTime?, room?, topic?, status?(SCHEDULED\|DONE\|CANCELED)}` | `LessonDto` (đổi giờ/phòng → báo học viên) |
| DELETE | `/api/admin/lessons/{lessonId}` | | hủy buổi (CANCELED) + báo học viên |
| GET | `/api/admin/courses/{id}/enrollments` | | `EnrolledStudentDto[]` |
| POST | `/api/admin/courses/{id}/enrollments` | `{studentIds:[1,2,3]}` | `{added, skipped}` + báo học viên |
| DELETE | `/api/admin/courses/{id}/enrollments/{studentId}` | | rút khỏi lớp (DROPPED) |

`CourseDto`: `id, courseName, description, teacherId, teacherName, tuitionFee, totalSessions, status, studentCount, lessonCount, heldCount, createdAt`
`LessonDto`: `id, courseId, courseName, teacherName, room, topic, startTime, endTime, status`
`EnrolledStudentDto`: `studentId, fullName, email, phone, avatarUrl, joinedAt`

## 2. STUDENT / TEACHER — Khóa học & lịch
| Method | Endpoint | Role | Ghi chú |
|---|---|---|---|
| GET | `/api/courses/mine` | STUDENT, TEACHER | HV: lớp đang học · GV: lớp phụ trách |
| GET | `/api/courses/{id}` | STUDENT, TEACHER, ADMIN | 403 nếu không thuộc lớp |
| GET | `/api/courses/{id}/lessons` | như trên | Lịch trình của lớp |
| GET | `/api/courses/{id}/students` | TEACHER, ADMIN | Danh sách học viên |
| GET | `/api/lessons?from=2026-10-01&to=2026-10-31` | STUDENT, TEACHER | Lịch cá nhân, tối đa 62 ngày, gồm cả buổi CANCELED |

## 3. Bài tập
| Method | Endpoint | Role | Body |
|---|---|---|---|
| POST | `/api/courses/{courseId}/assignments` | TEACHER (của lớp) | **multipart**: `title`, `description?`, `deadline`, `maxScore?`(mặc định 10), `file?` |
| GET | `/api/courses/{courseId}/assignments` | STUDENT, TEACHER, ADMIN | HV thấy `status`, GV thấy `submittedCount/gradedCount/totalStudents` |
| GET | `/api/assignments/mine?courseId=&status=` | STUDENT | `status`: `TODO` (Cần làm) · `SUBMITTED` (Đã nộp) · `GRADED` (Đã có điểm) · rỗng = tất cả |
| GET | `/api/assignments/{id}` | STUDENT, TEACHER, ADMIN | HV có `mySubmission` |
| PUT | `/api/assignments/{id}` | TEACHER | JSON `{title?, description?, deadline?, maxScore?}` (đổi hạn → báo học viên) |
| POST | `/api/assignments/{id}/attachment` | TEACHER | multipart `file` (thay đề) |
| DELETE | `/api/assignments/{id}/attachment` | TEACHER | |
| DELETE | `/api/assignments/{id}` | TEACHER | xóa luôn bài nộp + file |
| POST | `/api/assignments/{id}/submit` | STUDENT | multipart `file` (pdf, doc, docx, jpg, png, webp, heic ≤ 20MB). Quá hạn vẫn nhận, `late=true`. Nộp lại được khi chưa chấm |
| GET | `/api/assignments/{id}/submissions` | TEACHER, ADMIN | `{assignment, submitted:[SubmissionDto], notSubmitted:[StudentBriefDto]}` |
| GET | `/api/submissions/{id}` | TEACHER/ADMIN của lớp, hoặc HV chủ bài | `SubmissionDto` |
| PATCH | `/api/submissions/{id}/grade` | TEACHER | `{score, feedback?}` (0 ≤ score ≤ maxScore) → báo học viên |
| POST | `/api/assignments/{id}/remind-all` | TEACHER | → `{reminded: n}` (chỉ HV chưa nộp) |

`AssignmentDto`: `id, type("ESSAY"), courseId, courseName, title, description, attachmentUrl, deadline, maxScore, createdAt, minutesLeft(âm = quá hạn), status, mySubmission, submittedCount, gradedCount, totalStudents`
HV `status`: `PENDING` · `OVERDUE` · `SUBMITTED` · `GRADED`
`SubmissionDto`: `id, assignmentId, studentId, studentName, studentEmail, studentAvatarUrl, fileUrl, submittedAt, late, score, feedback, gradedAt, status(SUBMITTED|GRADED)`
> Android: khi tạo `MultipartBody.Part` cho ảnh chụp phải đặt tên file có đuôi (`bai_nop.jpg`), nếu không backend trả 400.

## 4. Thông báo
| Method | Endpoint | Role | Ghi chú |
|---|---|---|---|
| GET | `/api/notifications?page=0&size=20&unreadOnly=false` | mọi role | phân trang, mới nhất trước |
| GET | `/api/notifications/unread-count` | | `{count}` |
| PATCH | `/api/notifications/{id}/read` | | chỉ của mình |
| PATCH | `/api/notifications/read-all` | | `{updated}` |
| DELETE | `/api/notifications/{id}` | | chỉ của mình |
| POST | `/api/admin/notifications/broadcast` | ADMIN | `{title, content?, target:"ALL\|STUDENTS\|TEACHERS\|COURSE", courseId?}` → `{recipients}` |
| POST | `/api/devices` | mọi role | `{fcmToken}` — gọi sau đăng nhập (hiện chỉ lưu token) |
| DELETE | `/api/devices?token=...` | mọi role | gọi khi đăng xuất |

Sự kiện tự tạo thông báo: được phân công lớp (GV) · xếp/rút khỏi lớp (HV) · hủy/đổi giờ/đổi phòng buổi học (HV) · giao bài mới, đổi hạn nộp, nhắc nộp, có điểm (HV) · có bài nộp mới (GV).
