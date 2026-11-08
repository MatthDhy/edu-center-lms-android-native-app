package com.celehub.backend.tuition;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.celehub.backend.notification.NotificationService;

/** 08:00 ngày 1 hằng tháng: gửi thông báo "Học phí tháng M" cho MỌI học viên đang học (kèm số nợ nếu có). */
@Service
public class TuitionReminderService {
    private static final Logger log = LoggerFactory.getLogger(TuitionReminderService.class);

    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public TuitionReminderService(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 1 * *")
    public void monthlyJob() {
        log.info("Đã gửi thông báo học phí tháng cho {} học viên", sendMonthly());
    }

    /** Mỗi học viên chỉ nhận 1 lần/tháng (reminder_log chặn gửi trùng). */
    public int sendMonthly() {
        LocalDate today = LocalDate.now();
        long ym = today.getYear() * 100L + today.getMonthValue();
        Map<Long, long[]> debt = new HashMap<>();
        jdbc.query("""
                SELECT b.student_id, COUNT(*), COALESCE(SUM(b.amount - COALESCE(p.paid,0)),0)
                FROM tuition_bills b LEFT JOIN (SELECT bill_id, SUM(amount) AS paid FROM tuition_payments GROUP BY bill_id) p ON p.bill_id = b.id
                WHERE b.status <> 'PAID' GROUP BY b.student_id""",
                rs -> { debt.put(rs.getLong(1), new long[]{rs.getLong(2), rs.getLong(3)}); });

        List<Long> students = jdbc.queryForList("""
                SELECT DISTINCT e.student_id FROM enrollments e JOIN courses c ON c.id = e.course_id
                WHERE e.status = 'ACTIVE' AND c.status = 'ACTIVE'""", Long.class);
        int sent = 0;
        for (Long sid : students) {
            try {
                int claimed = jdbc.update("""
                        INSERT INTO reminder_log(kind, ref_id, user_id) VALUES ('TUITION_MONTH', ?, ?)
                        ON CONFLICT (kind, ref_id, user_id) DO NOTHING""", ym, sid);
                if (claimed != 1) continue;
                long[] d = debt.get(sid);
                String content = d == null ? "Bạn không có khoản học phí nào đang nợ. Cảm ơn bạn!"
                        : "Bạn còn " + d[0] + " khoản chưa đóng, tổng " + TuitionService.money(d[1]) + ". Vào mục Học phí để thanh toán.";
                notificationService.create(sid, "Học phí tháng " + today.getMonthValue() + "/" + today.getYear(),
                        content, null, "TUITION", null);
                sent++;
            } catch (Exception e) {
                log.warn("Gửi thông báo học phí cho {} lỗi: {}", sid, e.getMessage());
            }
        }
        return sent;
    }
}
