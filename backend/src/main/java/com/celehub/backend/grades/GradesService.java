package com.celehub.backend.grades;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảng điểm học viên, mỗi khóa 1 bảng: hàng điểm quiz, hàng điểm bài tập (quy về thang 10),
 * và trung bình theo tháng (cột "tổng cộng").
 */
@Service
@Transactional(readOnly = true)
public class GradesService {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public record Entry(String title, double score, double maxScore, LocalDateTime date) {}
    public record Monthly(String month, Double quizAvg, Double assignmentAvg, double avg) {}
    public record CourseGrades(Long courseId, String courseName, List<Entry> quizzes, List<Entry> assignments,
                               List<Monthly> monthly, Double overallAvg) {}

    private final JdbcTemplate jdbc;

    public GradesService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CourseGrades> mine(Long studentId, Long courseId) {
        List<Object[]> courses = jdbc.query("""
                SELECT c.id, c.course_name FROM courses c JOIN enrollments e ON e.course_id = c.id
                WHERE e.student_id = ? AND e.status = 'ACTIVE' AND (? = 0 OR c.id = ?) ORDER BY c.id DESC""",
                (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2)}, studentId, courseId == null ? 0L : courseId,
                courseId == null ? 0L : courseId);
        List<CourseGrades> out = new ArrayList<>();
        for (Object[] c : courses) {
            long cid = (Long) c[0];
            List<Entry> quizzes = jdbc.query("""
                    SELECT q.title, qa.score, qa.submitted_at FROM quiz_attempts qa JOIN quizzes q ON q.id = qa.quiz_id
                    WHERE qa.student_id = ? AND q.course_id = ? AND qa.submitted_at IS NOT NULL AND qa.score IS NOT NULL
                    ORDER BY qa.submitted_at""",
                    (rs, i) -> new Entry(rs.getString(1), rs.getDouble(2), 10, rs.getTimestamp(3).toLocalDateTime()), studentId, cid);
            List<Entry> assignments = jdbc.query("""
                    SELECT a.title, s.score, a.max_score, s.graded_at FROM submissions s JOIN assignments a ON a.id = s.assignment_id
                    WHERE s.student_id = ? AND a.course_id = ? AND s.score IS NOT NULL AND s.graded_at IS NOT NULL
                    ORDER BY s.graded_at""",
                    (rs, i) -> new Entry(rs.getString(1), rs.getDouble(2), rs.getDouble(3), rs.getTimestamp(4).toLocalDateTime()), studentId, cid);
            out.add(new CourseGrades(cid, (String) c[1], quizzes, assignments, monthly(quizzes, assignments), overall(quizzes, assignments)));
        }
        return out;
    }

    private static double norm(Entry e) {
        return e.maxScore() <= 0 ? 0 : e.score() * 10.0 / e.maxScore();
    }

    private static Double overall(List<Entry> q, List<Entry> a) {
        List<Double> all = new ArrayList<>();
        q.forEach(e -> all.add(norm(e)));
        a.forEach(e -> all.add(norm(e)));
        return all.isEmpty() ? null : round(all.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private static List<Monthly> monthly(List<Entry> q, List<Entry> a) {
        Map<String, List<Double>> qm = new TreeMap<>();
        Map<String, List<Double>> am = new TreeMap<>();
        q.forEach(e -> qm.computeIfAbsent(e.date().format(MONTH), k -> new ArrayList<>()).add(norm(e)));
        a.forEach(e -> am.computeIfAbsent(e.date().format(MONTH), k -> new ArrayList<>()).add(norm(e)));
        TreeMap<String, Boolean> months = new TreeMap<>();
        qm.keySet().forEach(k -> months.put(k, true));
        am.keySet().forEach(k -> months.put(k, true));
        List<Monthly> out = new ArrayList<>();
        for (String m : months.keySet()) {
            List<Double> x = qm.getOrDefault(m, List.of());
            List<Double> y = am.getOrDefault(m, List.of());
            List<Double> all = new ArrayList<>(x);
            all.addAll(y);
            out.add(new Monthly(m, avg(x), avg(y), round(all.stream().mapToDouble(Double::doubleValue).average().orElse(0))));
        }
        return out;
    }

    private static Double avg(List<Double> l) {
        return l.isEmpty() ? null : round(l.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
