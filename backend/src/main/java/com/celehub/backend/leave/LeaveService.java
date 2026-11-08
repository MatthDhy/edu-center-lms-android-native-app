package com.celehub.backend.leave;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseService;
import com.celehub.backend.course.Enrollment;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.course.Lesson;
import com.celehub.backend.course.LessonRepository;
import com.celehub.backend.course.LessonStatus;
import com.celehub.backend.course.dto.LessonDto;
import com.celehub.backend.leave.LeaveDtos.*;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.UserRepository;

/**
 * Xin nghỉ & học bù.
 * "Cùng môn" = tên lớp giống nhau sau khi bỏ chữ cái cuối (Toán 11A ~ Toán 11B), cùng tuần với buổi xin nghỉ,
 * còn chỗ (sĩ số + số người đã được duyệt học bù < sĩ số tối đa).
 */
@Service
public class LeaveService {
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final String REF = "LEAVE";

    private final LeaveRequestRepository repository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;
    private final int maxClassSize;

    public LeaveService(LeaveRequestRepository repository, LessonRepository lessonRepository,
                        EnrollmentRepository enrollmentRepository, UserRepository userRepository,
                        CourseService courseService, NotificationService notificationService,
                        @Value("${app.makeup.max-class-size:30}") int maxClassSize) {
        this.repository = repository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.notificationService = notificationService;
        this.maxClassSize = maxClassSize;
    }

    @Transactional(readOnly = true)
    public List<SuggestionDto> suggestions(AuthUser me, Long lessonId) {
        return suggestionsFor(me.id(), requireOwnLesson(me, lessonId));
    }

    @Transactional
    public LeaveDto create(AuthUser me, CreateLeaveRequest req) {
        Lesson l = requireOwnLesson(me, req.lessonId());
        if (repository.existsByStudentIdAndLessonIdAndStatusIn(me.id(), l.getId(),
                Set.of(LeaveStatus.PENDING, LeaveStatus.APPROVED))) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã gửi đơn xin nghỉ cho buổi này rồi");
        }
        Lesson makeup = null;
        if (req.makeupLessonId() != null) {
            makeup = suggestionsFor(me.id(), l).stream().filter(s -> s.lessonId().equals(req.makeupLessonId())).findFirst()
                    .flatMap(s -> lessonRepository.findDetail(s.lessonId()))
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Buổi học bù không hợp lệ hoặc đã hết chỗ"));
        }
        LeaveRequest r = repository.save(new LeaveRequest(userRepository.getReferenceById(me.id()), l, makeup, req.reason().trim()));
        notificationService.create(l.getCourse().getTeacher().getId(), "Đơn xin nghỉ mới",
                userRepository.getReferenceById(me.id()).getFullName() + " xin nghỉ " + l.getCourse().getCourseName()
                        + " (" + l.getStartTime().format(FULL) + ")", l.getCourse().getId(), REF, r.getId());
        return LeaveDto.from(repository.findDetail(r.getId()).orElse(r));
    }

    @Transactional(readOnly = true)
    public List<LeaveDto> mine(AuthUser me) {
        return repository.findByStudentDetail(me.id()).stream().map(LeaveDto::from).toList();
    }

    /** TEACHER: đơn của lớp mình · ADMIN: toàn trung tâm. status = null: tất cả. */
    @Transactional(readOnly = true)
    public List<LeaveDto> list(AuthUser me, LeaveStatus status) {
        List<LeaveRequest> all = me.role() == Role.ADMIN ? repository.findAllDetail() : repository.findByTeacherDetail(me.id());
        return all.stream().filter(r -> status == null || r.getStatus() == status).map(LeaveDto::from).toList();
    }

    @Transactional
    public LeaveDto approve(AuthUser me, Long id) {
        LeaveRequest r = loadPending(me, id);
        Lesson m = r.getMakeupLesson();
        if (m != null) {
            long taken = repository.countMakeupForLesson(m.getId(), LeaveStatus.APPROVED);
            long seats = enrollmentRepository.countByCourseIdAndStatus(m.getCourse().getId(), Enrollment.ACTIVE) + taken;
            if (seats >= maxClassSize) throw new AppException(HttpStatus.CONFLICT, "Lớp học bù đã hết chỗ");
        }
        r.approve(userRepository.getReferenceById(me.id()));
        notificationService.create(r.getStudent().getId(), "Đơn xin nghỉ được duyệt",
                r.getLesson().getCourse().getCourseName() + " (" + r.getLesson().getStartTime().format(FULL) + ")"
                        + (m == null ? "" : " - học bù: " + m.getCourse().getCourseName() + " " + m.getStartTime().format(FULL)),
                r.getLesson().getCourse().getId(), REF, r.getId());
        return LeaveDto.from(r);
    }

    @Transactional
    public LeaveDto reject(AuthUser me, Long id, String reason) {
        LeaveRequest r = loadPending(me, id);
        String why = reason == null || reason.isBlank() ? null : reason.trim();
        r.reject(userRepository.getReferenceById(me.id()), why);
        notificationService.create(r.getStudent().getId(), "Đơn xin nghỉ bị từ chối",
                r.getLesson().getCourse().getCourseName() + (why == null ? "" : ": " + why),
                r.getLesson().getCourse().getId(), REF, r.getId());
        return LeaveDto.from(r);
    }

    /** Các buổi học bù đã duyệt của học viên (gộp vào lịch cá nhân). */
    @Transactional(readOnly = true)
    public List<LessonDto> approvedMakeupLessons(Long studentId, LocalDate from, LocalDate to) {
        return repository.approvedMakeupLessons(studentId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().map(LessonDto::from).toList();
    }

    // ---------- helpers ----------

    private LeaveRequest loadPending(AuthUser me, Long id) {
        LeaveRequest r = repository.findDetail(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn xin nghỉ"));
        courseService.requireTeacherOf(me, r.getLesson().getCourse().getId());
        if (r.getStatus() != LeaveStatus.PENDING) throw new AppException(HttpStatus.CONFLICT, "Đơn đã được xử lý");
        return r;
    }

    private Lesson requireOwnLesson(AuthUser me, Long lessonId) {
        Lesson l = lessonRepository.findDetail(lessonId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi học"));
        if (!enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(l.getCourse().getId(), me.id(), Enrollment.ACTIVE)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học này");
        }
        if (l.getStatus() != LessonStatus.SCHEDULED || !l.getStartTime().isAfter(LocalDateTime.now())) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ xin nghỉ được các buổi chưa diễn ra");
        }
        return l;
    }

    private List<SuggestionDto> suggestionsFor(Long studentId, Lesson l) {
        LocalDate monday = l.getStartTime().toLocalDate().with(DayOfWeek.MONDAY);
        String key = normalize(l.getCourse().getCourseName());
        LocalDateTime now = LocalDateTime.now();
        return lessonRepository.findAllBetween(monday.atStartOfDay(), monday.plusDays(7).atStartOfDay()).stream()
                .filter(x -> x.getStatus() == LessonStatus.SCHEDULED && x.getStartTime().isAfter(now))
                .filter(x -> !x.getCourse().getId().equals(l.getCourse().getId()))
                .filter(x -> Course.ACTIVE.equals(x.getCourse().getStatus()))
                .filter(x -> normalize(x.getCourse().getCourseName()).equals(key))
                .map(x -> {
                    long seatsUsed = enrollmentRepository.countByCourseIdAndStatus(x.getCourse().getId(), Enrollment.ACTIVE)
                            + repository.countMakeupForLesson(x.getId(), LeaveStatus.APPROVED);
                    return new SuggestionDto(x.getId(), x.getCourse().getId(), x.getCourse().getCourseName(),
                            x.getCourse().getTeacher().getFullName(), x.getRoom(), x.getStartTime(), x.getEndTime(),
                            (int) Math.max(0, maxClassSize - seatsUsed));
                })
                .filter(s -> s.availableSeats() > 0)
                .toList();
    }

    /** "Toán 11A" -> "toán 11" ; "Hóa 10" -> "hóa 10". */
    static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("(?<=\\d)[A-Za-z]$", "").trim().toLowerCase();
    }
}
