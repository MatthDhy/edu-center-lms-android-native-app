package com.celehub.backend.tuition;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.tuition.FinanceDtos.*;
import com.celehub.backend.tuition.TuitionDtos.BillDto;

/** Báo cáo tài chính cho Web Admin: doanh thu theo tháng, tình hình đóng học phí theo lớp, danh sách công nợ. */
@Service
@Transactional(readOnly = true)
public class FinanceQueryService {
    private static final String PAID_SUB =
            "(SELECT bill_id, SUM(amount) AS paid, MAX(paid_at) AS last_paid, MAX(id) AS last_id FROM tuition_payments GROUP BY bill_id)";

    private final JdbcTemplate jdbc;

    public FinanceQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Overview overview(String monthParam) {
        YearMonth ym = parse(monthParam);
        String month = ym.toString();                         // yyyy-MM
        Date first = Date.valueOf(ym.atDay(1));

        long revenue = lng("SELECT COALESCE(SUM(amount),0) FROM tuition_payments WHERE to_char(paid_at,'YYYY-MM') = ?", month);
        long viet = lng("SELECT COALESCE(SUM(amount),0) FROM tuition_payments WHERE method='VIETQR' AND to_char(paid_at,'YYYY-MM') = ?", month);
        long cash = lng("SELECT COALESCE(SUM(amount),0) FROM tuition_payments WHERE method='CASH' AND to_char(paid_at,'YYYY-MM') = ?", month);

        String debtSql = "SELECT COALESCE(SUM(b.amount - COALESCE(p.paid,0)),0), COUNT(*) FROM tuition_bills b LEFT JOIN "
                + PAID_SUB + " p ON p.bill_id = b.id WHERE b.status <> 'PAID'";
        long[] out = jdbc.queryForObject(debtSql, (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)});
        long[] over = jdbc.queryForObject(debtSql + " AND b.due_date IS NOT NULL AND b.due_date < CURRENT_DATE",
                (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)});

        List<MonthRevenue> monthly = jdbc.query("""
                SELECT to_char(m,'YYYY-MM') AS mon, COALESCE(SUM(p.amount),0) AS amt
                FROM generate_series(CAST(CAST(? AS date) - interval '11 months' AS timestamp), CAST(CAST(? AS date) AS timestamp), interval '1 month') m
                LEFT JOIN tuition_payments p ON date_trunc('month', p.paid_at) = m
                GROUP BY m ORDER BY m""", (rs, i) -> new MonthRevenue(rs.getString(1), rs.getLong(2)), first, first);

        return new Overview(month, revenue, viet, cash, out[0], out[1], over[0], over[1], monthly, courses(month, null));
    }

    public CourseFinanceDetail courseDetail(Long courseId, String monthParam) {
        String month = parse(monthParam).toString();
        List<CourseFinance> one = courses(month, courseId);
        if (one.isEmpty()) throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học");
        List<StudentFinance> students = jdbc.query("""
                SELECT u.id, u.full_name, u.email, u.phone,
                       COALESCE(SUM(b.amount),0), COALESCE(SUM(p.paid),0), COUNT(b.id),
                       COALESCE(SUM(CASE WHEN b.status <> 'PAID' AND b.due_date IS NOT NULL AND b.due_date < CURRENT_DATE THEN 1 ELSE 0 END),0)
                FROM enrollments e JOIN users u ON u.id = e.student_id
                LEFT JOIN tuition_bills b ON b.student_id = u.id AND b.course_id = e.course_id
                LEFT JOIN """ + PAID_SUB + """
                 p ON p.bill_id = b.id
                WHERE e.course_id = ? AND e.status = 'ACTIVE'
                GROUP BY u.id, u.full_name, u.email, u.phone
                ORDER BY u.full_name""", (rs, i) -> {
            long billed = rs.getLong(5);
            long paid = rs.getLong(6);
            int bills = rs.getInt(7);
            int overdue = rs.getInt(8);
            String st = bills == 0 ? "NO_BILL" : paid >= billed ? "PAID" : overdue > 0 ? "OVERDUE" : "DEBT";
            return new StudentFinance(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    billed, paid, Math.max(0, billed - paid), bills, overdue, st);
        }, courseId);
        return new CourseFinanceDetail(one.get(0), students);
    }

    /** Danh sách khoản phải thu (tối đa 500 dòng). status: UNPAID | OVERDUE | PAID | '' ; courseId 0 = mọi lớp. */
    public List<BillDto> bills(String status, long courseId, String search) {
        String q = search == null ? "" : search.trim().toLowerCase();
        String like = "%" + q + "%";
        String st = status == null ? "" : status.trim().toUpperCase();
        return jdbc.query("""
                SELECT * FROM (
                  SELECT b.id, b.student_id, u.full_name, u.email, b.course_id, c.course_name, b.title, b.amount,
                         COALESCE(p.paid,0) AS paid, b.due_date,
                         CASE WHEN b.status = 'PAID' THEN 'PAID'
                              WHEN b.due_date IS NOT NULL AND b.due_date < CURRENT_DATE THEN 'OVERDUE'
                              ELSE 'UNPAID' END AS eff,
                         b.transfer_content, b.created_at, p.last_paid, p.last_id
                  FROM tuition_bills b JOIN users u ON u.id = b.student_id JOIN courses c ON c.id = b.course_id
                  LEFT JOIN """ + PAID_SUB + """
                   p ON p.bill_id = b.id
                ) t
                WHERE (? = '' OR t.eff = ?) AND (? = 0 OR t.course_id = ?)
                  AND (? = '' OR LOWER(t.full_name) LIKE ? OR LOWER(t.email) LIKE ? OR LOWER(t.transfer_content) LIKE ?)
                ORDER BY t.id DESC LIMIT 500""", (rs, i) -> {
            java.sql.Timestamp lp = rs.getTimestamp("last_paid");
            long lastId = rs.getLong("last_id");
            return new BillDto(rs.getLong("id"), rs.getLong("student_id"), rs.getString("full_name"), rs.getString("email"),
                    rs.getLong("course_id"), rs.getString("course_name"), rs.getString("title"),
                    rs.getBigDecimal("amount").longValue(), rs.getBigDecimal("paid").longValue(),
                    rs.getDate("due_date") == null ? null : rs.getDate("due_date").toLocalDate(), rs.getString("eff"),
                    rs.getString("transfer_content"), rs.getTimestamp("created_at").toLocalDateTime(),
                    lp == null ? null : lp.toLocalDateTime(), lp == null ? null : lastId);
        }, st, st, courseId, courseId, q, like, like, like);
    }

    // ---------- helpers ----------

    private List<CourseFinance> courses(String month, Long onlyCourse) {
        return jdbc.query("""
                SELECT c.id, c.course_name, t.full_name,
                  (SELECT COUNT(*) FROM enrollments e WHERE e.course_id = c.id AND e.status = 'ACTIVE') AS students,
                  COALESCE(SUM(b.amount),0) AS billed,
                  COALESCE(SUM(p.paid),0) AS paid,
                  COALESCE(SUM(CASE WHEN b.status <> 'PAID' AND b.due_date IS NOT NULL AND b.due_date < CURRENT_DATE
                                    THEN b.amount - COALESCE(p.paid,0) ELSE 0 END),0) AS overdue,
                  COUNT(DISTINCT CASE WHEN b.status = 'PAID' THEN b.student_id END) AS paid_students,
                  COUNT(DISTINCT CASE WHEN b.status <> 'PAID' THEN b.student_id END) AS debt_students,
                  COALESCE((SELECT SUM(p2.amount) FROM tuition_payments p2 JOIN tuition_bills b2 ON b2.id = p2.bill_id
                            WHERE b2.course_id = c.id AND to_char(p2.paid_at,'YYYY-MM') = ?),0) AS revenue_month
                FROM courses c JOIN users t ON t.id = c.teacher_id
                LEFT JOIN tuition_bills b ON b.course_id = c.id
                LEFT JOIN """ + PAID_SUB + """
                 p ON p.bill_id = b.id
                WHERE (? = 0 OR c.id = ?)
                GROUP BY c.id, c.course_name, t.full_name
                ORDER BY c.id DESC""", (rs, i) -> {
            long billed = rs.getBigDecimal("billed").longValue();
            long paid = rs.getBigDecimal("paid").longValue();
            return new CourseFinance(rs.getLong("id"), rs.getString("course_name"), rs.getString("full_name"),
                    rs.getLong("students"), billed, paid, Math.max(0, billed - paid),
                    rs.getBigDecimal("overdue").longValue(), rs.getLong("paid_students"), rs.getLong("debt_students"),
                    rs.getBigDecimal("revenue_month").longValue(),
                    billed == 0 ? 0 : Math.round(paid * 1000.0 / billed) / 10.0);
        }, month, onlyCourse == null ? 0L : onlyCourse, onlyCourse == null ? 0L : onlyCourse);
    }

    private long lng(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }

    private static YearMonth parse(String s) {
        if (s == null || s.isBlank()) return YearMonth.from(LocalDate.now());
        try {
            return YearMonth.parse(s.trim());
        } catch (Exception e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tháng phải có dạng yyyy-MM");
        }
    }
}
