# Bản vá backend V6 — điểm danh, nghỉ/bù, học phí, lương, bảng điểm, import Excel, push FCM

## 1. Giải nén đè vào repo (giữ nguyên đường dẫn). Ghi đè các file: `SecurityConfig`, `LessonController`,
`LeaveRequest`, `LeaveRequestRepository`, `AttendanceSession`, `AttendanceRecord`, `AttendanceRecordRepository`,
`NotificationService`, `DeviceTokenRepository`.

## 2. `backend/pom.xml` — thêm 1 dependency (đã có poi-ooxml từ bản V5)
```xml
<dependency>
  <groupId>com.google.firebase</groupId>
  <artifactId>firebase-admin</artifactId>
  <version>9.4.1</version>
</dependency>
```

## 3. `backend/src/main/resources/application.yml` — thêm dưới khóa `app:` (cùng cấp với `jwt:`, `storage:`)
```yaml
  firebase:
    enabled: ${FIREBASE_ENABLED:false}
    credentials-path: ${FIREBASE_CREDENTIALS_PATH:}
    credentials-base64: ${FIREBASE_CREDENTIALS_BASE64:}
  bank:
    id: ${BANK_ID:MB}                       # mã ngân hàng theo VietQR (MB, VCB, TCB, ACB...)
    name: ${BANK_NAME:MB Bank}
    account-number: ${BANK_ACCOUNT_NUMBER:0000000000}
    account-name: ${BANK_ACCOUNT_NAME:TRUNG TAM CELEHUB}
    webhook-secret: ${BANK_WEBHOOK_SECRET:}
  makeup:
    max-class-size: ${MAKEUP_MAX_CLASS_SIZE:30}
```
## 4. `.env` (và docker-compose nếu cần mount file khóa Firebase)
```
FIREBASE_ENABLED=true
FIREBASE_CREDENTIALS_PATH=/app/firebase/service-account.json
BANK_ID=MB
BANK_NAME=MB Bank
BANK_ACCOUNT_NUMBER=0987654321
BANK_ACCOUNT_NAME=TRUNG TAM DAY HOC CELEHUB
BANK_WEBHOOK_SECRET=<chuỗi ngẫu nhiên dài>
```
docker-compose.yml, service backend:
```yaml
    volumes:
      - ./secrets/firebase-service-account.json:/app/firebase/service-account.json:ro
```
(Dockerfile chạy user `celehub`: file khóa phải đọc được, `chmod 644`.)

## 5. API mới
| Nhóm | Endpoint |
|---|---|
| Điểm danh (GV) | `POST /api/attendance/sessions` · `GET /api/attendance/sessions/{id}/live` (3s/lần, kèm `qrPayload` đổi mỗi 30s) · `POST .../{id}/close` · `PATCH /api/attendance/records` · `GET /api/attendance/sessions?courseId=` |
| Điểm danh (HV) | `POST /api/attendance/check-in` `{qrPayload}` hoặc `{pinCode}` · `GET /api/attendance/mine?courseId=` |
| Chuyên cần | `GET /api/attendance/stats?courseId=&from=&to=` (GV/Admin) · `GET /api/admin/attendance?...` (Admin) |
| Nghỉ / bù | `GET /api/leave-requests/suggestions?lessonId=` · `POST /api/leave-requests` · `GET /api/leave-requests/mine` · `GET /api/teacher/leave-requests?status=` · `PATCH /api/leave-requests/{id}/approve|reject` · `GET /api/admin/leave-requests` |
| Lịch | `GET /api/lessons` của học viên đã gộp buổi học bù được duyệt |
| Học phí (HV) | `GET /api/tuition/bills` · `GET /api/tuition/bills/{id}/vietqr` · `GET /api/tuition/receipts/{paymentId}` |
| Học phí (Admin) | `POST /api/admin/tuition/bills` · `GET /api/admin/tuition/bills?status=&courseId=&search=` · `POST .../{id}/confirm-cash` · `DELETE .../{id}` · `POST /api/admin/tuition/simulate-payment` · `POST /api/admin/tuition/send-monthly-reminder` |
| Báo cáo tài chính | `GET /api/admin/finance/overview?month=yyyy-MM` · `GET /api/admin/finance/courses/{courseId}` |
| Webhook ngân hàng | `POST /api/webhooks/bank` (header `Authorization: Apikey <BANK_WEBHOOK_SECRET>`) |
| Lương | `PUT /api/admin/teachers/{id}/rate` · `GET /api/admin/payroll/rates` · `POST /api/admin/payroll/generate?month=` · `GET /api/admin/payroll?month=` · `GET /api/admin/payroll/items` · `PATCH /api/admin/payroll/{periodId}/approve` · `GET /api/payroll?month=` (GV) |
| Bảng điểm (HV) | `GET /api/me/grades[?courseId=]` |
| Import Excel | `POST /api/admin/users/import` (multipart `file`) · `GET /api/admin/users/import/template` |

## 6. Ghi chú
- Học phí quá hạn được **tính theo `due_date`** (không cần job); DB chỉ lưu UNPAID/PAID.
- Học viên học bù được phép điểm danh buổi bù (hệ thống kiểm tra đơn đã duyệt).
- Lương chỉ tính buổi có phiên điểm danh. Giảng viên chưa có đơn giá thì buổi đó bị bỏ qua và báo `skippedNoRate`.
- Chưa làm: xuất PDF phiếu lương/hóa đơn (app/web có thể in từ dữ liệu JSON), chống dò PIN bằng giới hạn số lần thử.
