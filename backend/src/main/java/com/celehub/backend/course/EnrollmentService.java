package com.celehub.backend.course;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.course.dto.EnrolledStudentDto;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, UserRepository userRepository,
                             CourseService courseService, NotificationService notificationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.notificationService = notificationService;
    }

    /** Danh sách học viên đang học (ADMIN hoặc GV của lớp). */
    @Transactional(readOnly = true)
    public List<EnrolledStudentDto> roster(AuthUser me, Long courseId) {
        if (me.role() == Role.STUDENT) throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách lớp");
        courseService.requireAccess(me, courseId);
        return enrollmentRepository.findActiveByCourse(courseId).stream().map(EnrolledStudentDto::from).toList();
    }

    /** Xếp nhiều học viên vào lớp. Học viên không hợp lệ / đã ở trong lớp được tính vào "skipped". */
    @Transactional
    public Map<String, Integer> enroll(Long courseId, List<Long> studentIds) {
        Course c = courseService.load(courseId);
        if (Course.CLOSED.equals(c.getStatus())) throw new AppException(HttpStatus.CONFLICT, "Lớp đã đóng");

        int added = 0;
        int skipped = 0;
        List<Long> notify = new ArrayList<>();
        for (Long sid : new LinkedHashSet<>(studentIds)) {
            User u = sid == null ? null : userRepository.findById(sid).orElse(null);
            if (u == null || u.getRole() != Role.STUDENT || u.getStatus() != UserStatus.ACTIVE) {
                skipped++;
                continue;
            }
            Optional<Enrollment> existing = enrollmentRepository.findByCourseIdAndStudentId(courseId, sid);
            if (existing.isPresent()) {
                if (Enrollment.ACTIVE.equals(existing.get().getStatus())) {
                    skipped++;
                    continue;
                }
                existing.get().setStatus(Enrollment.ACTIVE);
            } else {
                enrollmentRepository.save(new Enrollment(c, u));
            }
            added++;
            notify.add(sid);
        }
        notificationService.createForUsers(notify, "Bạn đã được xếp vào lớp mới", "Lớp: " + c.getCourseName());
        return Map.of("added", added, "skipped", skipped);
    }

    /** Rút học viên khỏi lớp (đặt DROPPED, giữ lại lịch sử bài nộp/điểm danh). */
    @Transactional
    public void remove(Long courseId, Long studentId) {
        Course c = courseService.load(courseId);
        Enrollment e = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .filter(x -> Enrollment.ACTIVE.equals(x.getStatus()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Học viên không có trong lớp"));
        e.setStatus(Enrollment.DROPPED);
        notificationService.create(studentId, "Bạn đã được rút khỏi lớp", "Lớp: " + c.getCourseName());
    }
}
