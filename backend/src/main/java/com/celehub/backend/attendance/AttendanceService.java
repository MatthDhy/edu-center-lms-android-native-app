package com.celehub.backend.attendance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.attendance.AttendanceDtos.*;
import com.celehub.backend.common.AppException;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseService;
import com.celehub.backend.course.Enrollment;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.course.Lesson;
import com.celehub.backend.course.LessonRepository;
import com.celehub.backend.course.LessonStatus;
import com.celehub.backend.leave.LeaveRequestRepository;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;

/**
 * Điểm danh QR động + PIN.
 * QR payload = "CLH:{sessionId}:{cửaSổ30s}:{chữKý}"; chữ ký = HMAC-SHA256(qr_token, sessionId:cửaSổ).
 * Server chấp nhận cửa sổ hiện tại và cửa sổ liền trước (chụp màn hình cũ quá 60s là vô hiệu).
 */
@Service
public class AttendanceService {
    static final int WINDOW_SECONDS = 30;
    private static final int LATE_AFTER_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;

    public AttendanceService(AttendanceSessionRepository sessionRepository, AttendanceRecordRepository recordRepository,
                             EnrollmentRepository enrollmentRepository, LessonRepository lessonRepository,
                             LeaveRequestRepository leaveRequestRepository, UserRepository userRepository,
                             CourseService courseService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
    }

    // ===================== GIẢNG VIÊN =====================

    @Transactional
    public SessionDto create(AuthUser me, CreateSessionRequest req) {
        Course c = courseService.requireTeacherOf(me, req.courseId());
        if (Course.CLOSED.equals(c.getStatus())) throw new AppException(HttpStatus.CONFLICT, "Lớp đã đóng");
        LocalDateTime now = LocalDateTime.now();
        int minutes = req.durationMinutes() == null ? 10 : req.durationMinutes();

        Lesson lesson = null;
        List<Lesson> lessons = lessonRepository.findByCourse(c.getId());
        if (req.lessonId() != null) {
            lesson = lessons.stream().filter(l -> l.getId().equals(req.lessonId())).findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Buổi học không thuộc lớp này"));
        } else {   // tự chọn buổi đang diễn ra / sắp diễn ra trong 30 phút
            for (Lesson l : lessons) {
                if (l.getStatus() != LessonStatus.CANCELED && !now.isBefore(l.getStartTime().minusMinutes(30))
                        && now.isBefore(l.getEndTime())) { lesson = l; break; }
            }
        }

        sessionRepository.closeOpen(c.getId(), now);
        String pin = newPin(now);
        AttendanceSession s = sessionRepository.save(new AttendanceSession(c, lesson,
                userRepository.getReferenceById(me.id()), pin, randomHex(32), now, now.plusMinutes(minutes)));
        return toDto(s, now);
    }

    @Transactional(readOnly = true)
    public LiveDto live(AuthUser me, Long sessionId) {
        AttendanceSession s = load(sessionId);
        courseService.requireAccess(me, s.getCourse().getId());
        LocalDateTime now = LocalDateTime.now();

        Map<Long, AttendanceRecord> byStudent = new HashMap<>();
        for (AttendanceRecord r : recordRepository.findBySession(sessionId)) byStudent.put(r.getStudent().getId(), r);

        List<LiveStudentDto> rows = new ArrayList<>();
        Set<Long> seen = new java.util.HashSet<>();
        for (Enrollment e : enrollmentRepository.findActiveByCourse(s.getCourse().getId())) {
            User u = e.getStudent();
            seen.add(u.getId());
            rows.add(liveRow(u, byStudent.get(u.getId())));
        }
        for (AttendanceRecord r : byStudent.values()) {   // học viên học bù (không thuộc lớp)
            if (!seen.contains(r.getStudent().getId())) rows.add(liveRow(r.getStudent(), r));
        }
        int present = (int) rows.stream().filter(x -> !"ABSENT".equals(x.status())).count();
        return new LiveDto(toDto(s, now), present, rows.size(), rows);
    }

    @Transactional
    public void updateRecord(AuthUser me, UpdateRecordRequest req) {
        AttendanceSession s = load(req.sessionId());
        courseService.requireTeacherOf(me, s.getCourse().getId());
        AttendanceRecord r = recordRepository.findBySessionIdAndStudentId(req.sessionId(), req.studentId()).orElse(null);
        if (r == null) {
            if (!enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(s.getCourse().getId(), req.studentId(), Enrollment.ACTIVE)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Học viên không thuộc lớp này");
            }
            recordRepository.save(new AttendanceRecord(s, userRepository.getReferenceById(req.studentId()),
                    req.status(), AttendanceMethod.MANUAL));
        } else {
            r.setStatus(req.status());
            r.setMethod(AttendanceMethod.MANUAL);
        }
    }

    @Transactional
    public void close(AuthUser me, Long sessionId) {
        AttendanceSession s = load(sessionId);
        courseService.requireTeacherOf(me, s.getCourse().getId());
        LocalDateTime now = LocalDateTime.now();
        if (s.isOpen(now)) s.setExpiresAt(now);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> sessions(AuthUser me, Long courseId) {
        courseService.requireAccess(me, courseId);
        LocalDateTime now = LocalDateTime.now();
        long students = enrollmentRepository.countByCourseIdAndStatus(courseId, Enrollment.ACTIVE);
        return sessionRepository.findByCourseDesc(courseId).stream().map(s -> toDto(s, now, students)).toList();
    }

    // ===================== HỌC VIÊN =====================

    @Transactional
    public CheckInResultDto checkIn(AuthUser me, CheckInRequest req) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasQr = req.qrPayload() != null && !req.qrPayload().isBlank();
        boolean hasPin = req.pinCode() != null && !req.pinCode().isBlank();
        if (hasQr == hasPin) throw new AppException(HttpStatus.BAD_REQUEST, "Gửi mã QR hoặc mã PIN");

        AttendanceSession s;
        AttendanceMethod method;
        if (hasQr) {
            s = fromQr(req.qrPayload().trim(), now);
            method = AttendanceMethod.QR;
        } else {
            String pin = req.pinCode().trim();
            if (!pin.matches("\\d{6}")) throw new AppException(HttpStatus.BAD_REQUEST, "Mã PIN gồm 6 chữ số");
            s = sessionRepository.findOpenByPin(pin, now).stream()
                    .filter(x -> allowed(me.id(), x.getCourse().getId())).findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Mã PIN không đúng hoặc phiên đã hết hạn"));
            method = AttendanceMethod.PIN;
        }
        if (!s.isOpen(now)) throw new AppException(HttpStatus.CONFLICT, "Phiên điểm danh đã hết hạn");
        if (!allowed(me.id(), s.getCourse().getId())) throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp này");
        if (recordRepository.findBySessionIdAndStudentId(s.getId(), me.id()).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã điểm danh buổi này rồi");
        }
        Lesson l = s.getLesson();
        AttendanceStatus status = l != null && now.isAfter(l.getStartTime().plusMinutes(LATE_AFTER_MINUTES))
                ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
        AttendanceRecord r = recordRepository.save(new AttendanceRecord(s, userRepository.getReferenceById(me.id()), status, method));
        return new CheckInResultDto(s.getId(), s.getCourse().getId(), s.getCourse().getCourseName(),
                status.name(), r.getCheckInTime());
    }

    @Transactional(readOnly = true)
    public List<MyRecordDto> mine(AuthUser me, Long courseId) {
        return recordRepository.findMine(me.id(), courseId).stream()
                .map(r -> new MyRecordDto(r.getSession().getId(), r.getSession().getStartTime(),
                        r.getStatus().name(), r.getMethod().name())).toList();
    }

    // ===================== helpers =====================

    private boolean allowed(Long studentId, Long courseId) {
        return enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(courseId, studentId, Enrollment.ACTIVE)
                || leaveRequestRepository.countApprovedMakeupInCourse(studentId, courseId) > 0;
    }

    private AttendanceSession fromQr(String payload, LocalDateTime now) {
        String[] p = payload.split(":");
        if (p.length != 4 || !"CLH".equals(p[0])) throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR không hợp lệ");
        long id;
        long window;
        try {
            id = Long.parseLong(p[1]);
            window = Long.parseLong(p[2]);
        } catch (NumberFormatException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR không hợp lệ");
        }
        AttendanceSession s = sessionRepository.findDetail(id)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Mã QR không hợp lệ"));
        long cur = currentWindow();
        if (window != cur && window != cur - 1) throw new AppException(HttpStatus.CONFLICT, "Mã QR đã hết hạn, hãy quét lại mã đang hiển thị");
        byte[] expected = sign(s.getQrToken(), id + ":" + window).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, p[3].getBytes(StandardCharsets.UTF_8))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR không hợp lệ");
        }
        return s;
    }

    private AttendanceSession load(Long id) {
        return sessionRepository.findDetail(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên điểm danh"));
    }

    private LiveStudentDto liveRow(User u, AttendanceRecord r) {
        return new LiveStudentDto(u.getId(), u.getFullName(), u.getAvatarUrl(),
                r == null ? "ABSENT" : r.getStatus().name(), r == null ? null : r.getMethod().name(),
                r == null ? null : r.getCheckInTime());
    }

    private SessionDto toDto(AttendanceSession s, LocalDateTime now) {
        return toDto(s, now, enrollmentRepository.countByCourseIdAndStatus(s.getCourse().getId(), Enrollment.ACTIVE));
    }

    private SessionDto toDto(AttendanceSession s, LocalDateTime now, long students) {
        long w = currentWindow();
        int left = (int) (WINDOW_SECONDS - (System.currentTimeMillis() / 1000) % WINDOW_SECONDS);
        return new SessionDto(s.getId(), s.getCourse().getId(), s.getCourse().getCourseName(),
                s.getLesson() == null ? null : s.getLesson().getId(), s.getPinCode(),
                "CLH:" + s.getId() + ":" + w + ":" + sign(s.getQrToken(), s.getId() + ":" + w), left,
                s.getStartTime(), s.getExpiresAt(), s.isOpen(now), students);
    }

    private static long currentWindow() {
        return (System.currentTimeMillis() / 1000) / WINDOW_SECONDS;
    }

    private String newPin(LocalDateTime now) {
        for (int i = 0; i < 30; i++) {
            String pin = String.format("%06d", RANDOM.nextInt(1_000_000));
            if (sessionRepository.findOpenByPin(pin, now).isEmpty()) return pin;
        }
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được mã PIN, thử lại");
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static String sign(String key, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
