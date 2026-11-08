package com.celehub.backend.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.attendance.AttendanceDtos.*;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseService;
import com.celehub.backend.course.Enrollment;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.security.AuthUser;

/** Ma trận chuyên cần học viên × buổi (mỗi phiên điểm danh = 1 buổi). Vắng > 20% => cảnh báo. */
@Service
public class AttendanceReportService {
    private static final double WARN_ABSENT_PERCENT = 20.0;

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    public AttendanceReportService(AttendanceSessionRepository sessionRepository,
                                   AttendanceRecordRepository recordRepository,
                                   EnrollmentRepository enrollmentRepository, CourseService courseService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
    }

    @Transactional(readOnly = true)
    public MatrixDto matrix(AuthUser me, Long courseId, LocalDate from, LocalDate to) {
        Course c = courseService.requireAccess(me, courseId);
        LocalDateTime a = from == null ? LocalDate.of(2000, 1, 1).atStartOfDay() : from.atStartOfDay();
        LocalDateTime b = to == null ? LocalDate.of(2100, 1, 1).atStartOfDay() : to.plusDays(1).atStartOfDay();
        List<AttendanceSession> sessions = sessionRepository.findByCourseBetween(courseId, a, b);
        List<Long> ids = sessions.stream().map(AttendanceSession::getId).toList();

        Map<String, AttendanceStatus> status = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AttendanceRecord r : recordRepository.findBySessionIds(ids)) {
                status.put(r.getSession().getId() + ":" + r.getStudent().getId(), r.getStatus());
            }
        }
        List<MatrixStudentDto> rows = new ArrayList<>();
        double sumRate = 0;
        int warnings = 0;
        for (Enrollment e : enrollmentRepository.findActiveByCourse(courseId)) {
            Long sid = e.getStudent().getId();
            List<String> cells = new ArrayList<>();
            int present = 0, late = 0, excused = 0, absent = 0;
            for (AttendanceSession s : sessions) {
                AttendanceStatus st = status.getOrDefault(s.getId() + ":" + sid, AttendanceStatus.ABSENT);
                cells.add(st.name());
                switch (st) {
                    case PRESENT -> present++;
                    case LATE -> late++;
                    case EXCUSED -> excused++;
                    default -> absent++;
                }
            }
            int n = sessions.size();
            double absentRate = n == 0 ? 0 : Math.round(absent * 1000.0 / n) / 10.0;
            boolean warn = n > 0 && absentRate > WARN_ABSENT_PERCENT;
            if (warn) warnings++;
            if (n > 0) sumRate += (present + late + excused) * 100.0 / n;
            rows.add(new MatrixStudentDto(sid, e.getStudent().getFullName(), cells, present, late, excused,
                    absent, absentRate, warn));
        }
        double avg = rows.isEmpty() || sessions.isEmpty() ? 0 : Math.round(sumRate / rows.size() * 10.0) / 10.0;
        return new MatrixDto(courseId, c.getCourseName(),
                sessions.stream().map(s -> new MatrixSessionDto(s.getId(), s.getStartTime())).toList(),
                rows, avg, warnings);
    }
}
