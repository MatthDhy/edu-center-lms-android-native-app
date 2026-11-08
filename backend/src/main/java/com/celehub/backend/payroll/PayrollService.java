package com.celehub.backend.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.UserRepository;

/**
 * Lương giảng viên = Σ (số giờ buổi dạy × đơn giá/giờ tại ngày dạy).
 * Chỉ tính buổi đã kết thúc, không hủy, và có phiên điểm danh (gắn buổi hoặc cùng lớp trong khung giờ buổi đó).
 */
@Service
public class PayrollService {
    public record RateDto(Long teacherId, String fullName, String email, Long hourlyRate) {}
    public record TeacherTotal(Long teacherId, String fullName, long lessons, double hours, long amount) {}
    public record ItemDto(Long id, Long teacherId, String teacherName, Long lessonId, String description,
                          LocalDate workDate, double hours, long rate, long amount) {}
    public record PeriodDto(Long periodId, String month, String status, long total, List<TeacherTotal> teachers) {}
    public record GenerateResult(Long periodId, String month, int items, int skippedNoRate) {}
    /** status: NOT_READY (chưa duyệt) | APPROVED. */
    public record MyPayrollDto(String month, String status, long total, double hours, List<ItemDto> items) {}

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PayrollService(JdbcTemplate jdbc, UserRepository userRepository, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ---------- đơn giá ----------

    @Transactional(readOnly = true)
    public List<RateDto> rates() {
        return jdbc.query("""
                SELECT u.id, u.full_name, u.email,
                  (SELECT r.hourly_rate FROM teacher_rates r WHERE r.teacher_id = u.id AND r.effective_from <= CURRENT_DATE
                   ORDER BY r.effective_from DESC LIMIT 1) AS rate
                FROM users u WHERE u.role = 'TEACHER' AND u.status = 'ACTIVE' ORDER BY u.full_name""",
                (rs, i) -> new RateDto(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getBigDecimal(4) == null ? null : rs.getBigDecimal(4).longValue()));
    }

    @Transactional
    public void setRate(Long teacherId, long hourlyRate, LocalDate effectiveFrom) {
        var t = userRepository.findById(teacherId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giảng viên"));
        if (t.getRole() != Role.TEACHER) throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản này không phải giảng viên");
        if (hourlyRate < 0) throw new AppException(HttpStatus.BAD_REQUEST, "Đơn giá không hợp lệ");
        jdbc.update("""
                INSERT INTO teacher_rates(teacher_id, hourly_rate, effective_from) VALUES (?, ?, ?)
                ON CONFLICT (teacher_id, effective_from) DO UPDATE SET hourly_rate = EXCLUDED.hourly_rate""",
                teacherId, BigDecimal.valueOf(hourlyRate), java.sql.Date.valueOf(effectiveFrom == null ? LocalDate.now() : effectiveFrom));
    }

    // ---------- bảng lương tháng ----------

    @Transactional
    public GenerateResult generate(String monthParam) {
        String month = month(monthParam);
        Map<String, Object> period = findPeriod(month);
        Long periodId;
        if (period == null) {
            periodId = jdbc.queryForObject("INSERT INTO payroll_periods(month) VALUES (?) RETURNING id", Long.class, month);
        } else {
            if ("APPROVED".equals(period.get("status"))) throw new AppException(HttpStatus.CONFLICT, "Bảng lương tháng này đã duyệt, không thể tính lại");
            periodId = ((Number) period.get("id")).longValue();
            jdbc.update("DELETE FROM payroll_items WHERE period_id = ?", periodId);
        }
        String base = """
                FROM lessons l JOIN courses c ON c.id = l.course_id
                WHERE l.status <> 'CANCELED' AND l.end_time < NOW() AND to_char(l.start_time,'YYYY-MM') = ?
                  AND EXISTS (SELECT 1 FROM attendance_sessions s WHERE s.lesson_id = l.id
                              OR (s.course_id = l.course_id AND s.start_time >= l.start_time - interval '1 hour' AND s.start_time < l.end_time))""";
        int items = jdbc.update("""
                INSERT INTO payroll_items(period_id, teacher_id, lesson_id, description, work_date, hours, rate, amount)
                SELECT ?, c.teacher_id, l.id, c.course_name || ' - ' || to_char(l.start_time,'DD/MM HH24:MI'),
                       CAST(l.start_time AS date),
                       ROUND(CAST(EXTRACT(EPOCH FROM (l.end_time - l.start_time)) / 3600.0 AS numeric), 2),
                       r.rate,
                       ROUND(CAST(EXTRACT(EPOCH FROM (l.end_time - l.start_time)) / 3600.0 AS numeric) * r.rate, 0)
                FROM lessons l JOIN courses c ON c.id = l.course_id
                JOIN LATERAL (SELECT tr.hourly_rate AS rate FROM teacher_rates tr
                              WHERE tr.teacher_id = c.teacher_id AND tr.effective_from <= CAST(l.start_time AS date)
                              ORDER BY tr.effective_from DESC LIMIT 1) r ON TRUE
                WHERE l.status <> 'CANCELED' AND l.end_time < NOW() AND to_char(l.start_time,'YYYY-MM') = ?
                  AND EXISTS (SELECT 1 FROM attendance_sessions s WHERE s.lesson_id = l.id
                              OR (s.course_id = l.course_id AND s.start_time >= l.start_time - interval '1 hour' AND s.start_time < l.end_time))""",
                periodId, month);
        Integer all = jdbc.queryForObject("SELECT COUNT(*) " + base, Integer.class, month);
        return new GenerateResult(periodId, month, items, Math.max(0, (all == null ? 0 : all) - items));
    }

    @Transactional(readOnly = true)
    public PeriodDto view(String monthParam) {
        String month = month(monthParam);
        Map<String, Object> p = findPeriod(month);
        if (p == null) return new PeriodDto(null, month, "NONE", 0, List.of());
        long id = ((Number) p.get("id")).longValue();
        List<TeacherTotal> rows = jdbc.query("""
                SELECT i.teacher_id, u.full_name, COUNT(*), COALESCE(SUM(i.hours),0), COALESCE(SUM(i.amount),0)
                FROM payroll_items i JOIN users u ON u.id = i.teacher_id
                WHERE i.period_id = ? GROUP BY i.teacher_id, u.full_name ORDER BY u.full_name""",
                (rs, i) -> new TeacherTotal(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getDouble(4), rs.getBigDecimal(5).longValue()), id);
        return new PeriodDto(id, month, (String) p.get("status"), rows.stream().mapToLong(TeacherTotal::amount).sum(), rows);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> items(String monthParam, Long teacherId) {
        Map<String, Object> p = findPeriod(month(monthParam));
        if (p == null) return List.of();
        return queryItems(((Number) p.get("id")).longValue(), teacherId);
    }

    @Transactional
    public void approve(Long periodId) {
        Map<String, Object> p;
        try {
            p = jdbc.queryForMap("SELECT month, status FROM payroll_periods WHERE id = ?", periodId);
        } catch (EmptyResultDataAccessException e) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bảng lương");
        }
        if ("APPROVED".equals(p.get("status"))) return;
        jdbc.update("UPDATE payroll_periods SET status = 'APPROVED', approved_at = NOW() WHERE id = ?", periodId);
        String month = (String) p.get("month");
        List<Long> teachers = jdbc.queryForList("SELECT DISTINCT teacher_id FROM payroll_items WHERE period_id = ?", Long.class, periodId);
        for (Long t : teachers) {
            notificationService.create(t, "Bảng lương tháng " + month + " đã được duyệt",
                    "Vào Cá nhân → Bảng lương để xem chi tiết từng ca dạy.", null, "PAYROLL", periodId);
        }
    }

    // ---------- giảng viên ----------

    @Transactional(readOnly = true)
    public MyPayrollDto mine(Long teacherId, String monthParam) {
        String month = month(monthParam);
        Map<String, Object> p = findPeriod(month);
        if (p == null || !"APPROVED".equals(p.get("status"))) return new MyPayrollDto(month, "NOT_READY", 0, 0, List.of());
        List<ItemDto> items = queryItems(((Number) p.get("id")).longValue(), teacherId);
        return new MyPayrollDto(month, "APPROVED", items.stream().mapToLong(ItemDto::amount).sum(),
                items.stream().mapToDouble(ItemDto::hours).sum(), items);
    }

    // ---------- helpers ----------

    private List<ItemDto> queryItems(long periodId, Long teacherId) {
        return jdbc.query("""
                SELECT i.id, i.teacher_id, u.full_name, i.lesson_id, i.description, i.work_date, i.hours, i.rate, i.amount
                FROM payroll_items i JOIN users u ON u.id = i.teacher_id
                WHERE i.period_id = ? AND (? = 0 OR i.teacher_id = ?) ORDER BY i.work_date, i.id""",
                (rs, n) -> new ItemDto(rs.getLong(1), rs.getLong(2), rs.getString(3), (Long) rs.getObject(4), rs.getString(5),
                        rs.getDate(6) == null ? null : rs.getDate(6).toLocalDate(), rs.getDouble(7),
                        rs.getBigDecimal(8).longValue(), rs.getBigDecimal(9).longValue()),
                periodId, teacherId == null ? 0L : teacherId, teacherId == null ? 0L : teacherId);
    }

    private Map<String, Object> findPeriod(String month) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT id, status FROM payroll_periods WHERE month = ?", month);
        return l.isEmpty() ? null : l.get(0);
    }

    private static String month(String s) {
        if (s == null || s.isBlank()) return YearMonth.now().toString();
        try {
            return YearMonth.parse(s.trim()).toString();
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tháng phải có dạng yyyy-MM");
        }
    }
}
