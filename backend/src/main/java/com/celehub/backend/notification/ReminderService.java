package com.celehub.backend.notification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * - Mỗi giờ: học viên có bài tập hạn trong 24h tới mà chưa nộp -> nhắc 1 lần/bài.
 * - 07:30 mỗi ngày: giảng viên còn bài nộp chưa chấm -> 1 thông báo tổng hợp/ngày.
 * reminder_log (UNIQUE) + INSERT ... ON CONFLICT DO NOTHING đảm bảo không gửi trùng.
 */
@Service
public class ReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public ReminderService(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void remindDueSoon() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT a.id AS aid, a.title, a.deadline, c.course_name, e.student_id
                FROM assignments a
                JOIN courses c ON c.id = a.course_id
                JOIN enrollments e ON e.course_id = c.id AND e.status = 'ACTIVE'
                WHERE a.deadline > ? AND a.deadline <= ?
                  AND NOT EXISTS (SELECT 1 FROM submissions s
                                  WHERE s.assignment_id = a.id AND s.student_id = e.student_id)
                """, Timestamp.valueOf(now), Timestamp.valueOf(now.plusHours(24)));

        int sent = 0;
        for (Map<String, Object> r : rows) {
            try {
                long aid = ((Number) r.get("aid")).longValue();
                long sid = ((Number) r.get("student_id")).longValue();
                if (!claim("DUE_SOON", aid, sid)) continue;
                LocalDateTime dl = ((Timestamp) r.get("deadline")).toLocalDateTime();
                notificationService.create(sid, "Sắp đến hạn: " + r.get("title"),
                        r.get("course_name") + " - hạn nộp " + dl.format(FMT));
                sent++;
            } catch (Exception e) {
                log.warn("Nhắc hạn nộp lỗi: {}", e.getMessage());
            }
        }
        if (sent > 0) log.info("Đã nhắc {} học viên sắp đến hạn nộp bài", sent);
    }

    @Scheduled(cron = "0 30 7 * * *")
    public void remindGrading() {
        long today = LocalDate.now().toEpochDay();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT c.teacher_id, COUNT(*) AS n
                FROM submissions s
                JOIN assignments a ON a.id = s.assignment_id
                JOIN courses c ON c.id = a.course_id
                WHERE s.score IS NULL
                GROUP BY c.teacher_id
                """);
        for (Map<String, Object> r : rows) {
            try {
                long tid = ((Number) r.get("teacher_id")).longValue();
                long n = ((Number) r.get("n")).longValue();
                if (!claim("GRADING_DIGEST", today, tid)) continue;
                notificationService.create(tid, "Bài nộp chờ chấm", "Bạn còn " + n + " bài nộp chưa chấm điểm");
            } catch (Exception e) {
                log.warn("Nhắc chấm bài lỗi: {}", e.getMessage());
            }
        }
    }

    /** true nếu lần đầu ghi nhận (được phép gửi). */
    private boolean claim(String kind, long refId, long userId) {
        return jdbc.update("""
                INSERT INTO reminder_log(kind, ref_id, user_id) VALUES (?, ?, ?)
                ON CONFLICT (kind, ref_id, user_id) DO NOTHING""", kind, refId, userId) == 1;
    }
}