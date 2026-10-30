package com.celehub.backend.course;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.course.dto.CourseDto;
import com.celehub.backend.course.dto.CreateCourseRequest;
import com.celehub.backend.course.dto.UpdateCourseRequest;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final NotificationService notificationService;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository,
                         EnrollmentRepository enrollmentRepository, LessonRepository lessonRepository,
                         NotificationService notificationService) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.notificationService = notificationService;
    }

    // ===================== ADMIN =====================

    @Transactional(readOnly = true)
    public List<CourseDto> adminList(String status, Long teacherId, String search) {
        String s = status == null ? "" : status.trim().toUpperCase();
        String q = search == null ? "" : search.trim().toLowerCase();
        long t = teacherId == null ? 0L : teacherId;
        return courseRepository.search(s, t, q).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CourseDto adminGet(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public CourseDto create(CreateCourseRequest req) {
        User teacher = requireTeacher(req.teacherId());
        Course c = new Course(teacher, req.courseName().trim(), blankToNull(req.description()),
                BigDecimal.valueOf(req.tuitionFee() == null ? 0L : req.tuitionFee()), req.totalSessions());
        courseRepository.save(c);
        notificationService.create(teacher.getId(), "Bạn được phân công lớp mới", c.getCourseName());
        return toDto(c);
    }

    @Transactional
    public CourseDto update(Long id, UpdateCourseRequest req) {
        Course c = load(id);
        if (req.courseName() != null) {
            String n = req.courseName().trim();
            if (n.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "Tên lớp không được để trống");
            c.setCourseName(n);
        }
        if (req.description() != null) c.setDescription(blankToNull(req.description()));
        if (req.tuitionFee() != null) c.setTuitionFee(BigDecimal.valueOf(req.tuitionFee()));
        if (req.totalSessions() != null) c.setTotalSessions(req.totalSessions());
        if (req.status() != null) c.setStatus(req.status());

        if (req.teacherId() != null && !req.teacherId().equals(c.getTeacher().getId())) {
            User t = requireTeacher(req.teacherId());
            // Giảng viên mới không được trùng giờ với các buổi sắp tới của lớp này
            for (Lesson l : lessonRepository.findFutureScheduled(c.getId(), LocalDateTime.now(), LessonStatus.SCHEDULED)) {
                List<Lesson> overlap = lessonRepository.findTeacherOverlaps(
                        t.getId(), l.getStartTime(), l.getEndTime(), 0L, LessonStatus.CANCELED);
                if (!overlap.isEmpty()) {
                    throw new AppException(HttpStatus.CONFLICT, "Giảng viên mới bị trùng lịch với lớp "
                            + overlap.get(0).getCourse().getCourseName() + " vào " + l.getStartTime());
                }
            }
            c.setTeacher(t);
            notificationService.create(t.getId(), "Bạn được phân công lớp mới", c.getCourseName());
        }
        return toDto(c);
    }

    // ===================== STUDENT / TEACHER =====================

    @Transactional(readOnly = true)
    public List<CourseDto> mine(AuthUser me) {
        List<Course> list = switch (me.role()) {
            case TEACHER -> courseRepository.findByTeacher(me.id());
            case STUDENT -> courseRepository.findByStudent(me.id());
            case ADMIN -> courseRepository.search("", 0L, "");
        };
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CourseDto detail(AuthUser me, Long id) {
        return toDto(requireAccess(me, id));
    }

    // ===================== Dùng chung cho các service khác =====================

    @Transactional(readOnly = true)
    public Course load(Long id) {
        return courseRepository.findWithTeacher(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học"));
    }

    /** ADMIN: luôn được. TEACHER: phải là giảng viên của lớp. STUDENT: phải đang học lớp. */
    @Transactional(readOnly = true)
    public Course requireAccess(AuthUser me, Long courseId) {
        Course c = load(courseId);
        boolean ok = switch (me.role()) {
            case ADMIN -> true;
            case TEACHER -> c.getTeacher().getId().equals(me.id());
            case STUDENT -> enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(
                    courseId, me.id(), Enrollment.ACTIVE);
        };
        if (!ok) throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học này");
        return c;
    }

    /** Chỉ giảng viên phụ trách lớp mới được thao tác (giao bài, chấm bài...). */
    @Transactional(readOnly = true)
    public Course requireTeacherOf(AuthUser me, Long courseId) {
        Course c = load(courseId);
        if (me.role() != Role.TEACHER || !c.getTeacher().getId().equals(me.id())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ giảng viên phụ trách lớp mới được thực hiện thao tác này");
        }
        return c;
    }

    // ===================== helpers =====================

    private User requireTeacher(Long teacherId) {
        User t = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giảng viên"));
        if (t.getRole() != Role.TEACHER) throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản này không phải giảng viên");
        if (t.getStatus() != UserStatus.ACTIVE) throw new AppException(HttpStatus.BAD_REQUEST, "Tài khoản giảng viên đang bị khóa");
        return t;
    }

    private CourseDto toDto(Course c) {
        long students = enrollmentRepository.countByCourseIdAndStatus(c.getId(), Enrollment.ACTIVE);
        long lessons = lessonRepository.countActiveByCourse(c.getId(), LessonStatus.CANCELED);
        long held = lessonRepository.countHeldByCourse(c.getId(), LocalDateTime.now(), LessonStatus.CANCELED);
        return new CourseDto(c.getId(), c.getCourseName(), c.getDescription(), c.getTeacher().getId(),
                c.getTeacher().getFullName(), c.getTuitionFee().longValue(), c.getTotalSessions(), c.getStatus(),
                students, lessons, held, c.getCreatedAt());
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
