package com.celehub.backend.assignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.assignment.dto.AssignmentDto;
import com.celehub.backend.assignment.dto.GradeRequest;
import com.celehub.backend.assignment.dto.SubmissionDto;
import com.celehub.backend.assignment.dto.SubmissionsOverviewDto;
import com.celehub.backend.assignment.dto.UpdateAssignmentRequest;
import com.celehub.backend.common.AppException;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseService;
import com.celehub.backend.course.Enrollment;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.quiz.QuizService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.storage.StorageService;
import com.celehub.backend.storage.UploadValidator;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.dto.StudentBriefDto;

@Service
public class AssignmentService {
    private static final long MAX_FILE = 20L * 1024 * 1024;
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final String REF = "ASSIGNMENT";

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final QuizService quizService;

    public AssignmentService(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                             EnrollmentRepository enrollmentRepository, UserRepository userRepository,
                             CourseService courseService, StorageService storageService,
                             NotificationService notificationService, QuizService quizService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.storageService = storageService;
        this.notificationService = notificationService;
        this.quizService = quizService;
    }

    // =====================================================================
    //  GIẢNG VIÊN: giao / sửa / xóa bài
    // =====================================================================

    @Transactional
    public AssignmentDto create(AuthUser me, Long courseId, String title, String description,
                                LocalDateTime deadline, BigDecimal maxScore, boolean graded, MultipartFile file) {
        Course c = courseService.requireTeacherOf(me, courseId);
        String t = requireTitle(title);
        if (deadline == null) throw new AppException(HttpStatus.BAD_REQUEST, "Hạn nộp không được để trống");
        BigDecimal max = normalizeMax(maxScore);

        boolean hasFile = file != null && !file.isEmpty();
        if (hasFile) UploadValidator.require(file, UploadValidator.DOCUMENTS, MAX_FILE, "đính kèm");

        Assignment a = new Assignment(c, t, blankToNull(description), deadline, max, graded);
        if (hasFile) a.setAttachmentUrl(storageService.upload(file, "assignments"));
        assignmentRepository.save(a);

        notificationService.createForUsers(enrollmentRepository.findActiveStudentIds(courseId),
                "Bài tập mới: " + t, c.getCourseName() + " - hạn nộp " + deadline.format(FULL),
                courseId, REF, a.getId());
        return teacherDto(a, enrollmentRepository.countByCourseIdAndStatus(courseId, Enrollment.ACTIVE), LocalDateTime.now());
    }

    @Transactional
    public AssignmentDto update(AuthUser me, Long id, UpdateAssignmentRequest req) {
        Assignment a = loadDetail(id);
        Long courseId = a.getCourse().getId();
        courseService.requireTeacherOf(me, courseId);

        if (req.title() != null) a.setTitle(requireTitle(req.title()));
        if (req.description() != null) a.setDescription(blankToNull(req.description()));
        if (req.maxScore() != null) {
            BigDecimal max = normalizeMax(req.maxScore());
            BigDecimal highest = submissionRepository.maxScoreGiven(id);
            if (highest != null && highest.compareTo(max) > 0) {
                throw new AppException(HttpStatus.CONFLICT, "Đã có bài được chấm " + plain(highest) + " điểm, không thể đặt điểm tối đa thấp hơn");
            }
            a.setMaxScore(max);
        }
        if (req.deadline() != null && !req.deadline().equals(a.getDeadline())) {
            a.setDeadline(req.deadline());
            notificationService.createForUsers(enrollmentRepository.findActiveStudentIds(courseId),
                    "Đổi hạn nộp: " + a.getTitle(), "Hạn nộp mới: " + req.deadline().format(FULL),
                    courseId, REF, a.getId());
        }
        return teacherDto(a, enrollmentRepository.countByCourseIdAndStatus(courseId, Enrollment.ACTIVE), LocalDateTime.now());
    }

    @Transactional
    public AssignmentDto replaceAttachment(AuthUser me, Long id, MultipartFile file) {
        Assignment a = loadDetail(id);
        courseService.requireTeacherOf(me, a.getCourse().getId());
        UploadValidator.require(file, UploadValidator.DOCUMENTS, MAX_FILE, "đính kèm");
        String old = a.getAttachmentUrl();
        a.setAttachmentUrl(storageService.upload(file, "assignments"));
        if (old != null) storageService.delete(old);
        return teacherDto(a, enrollmentRepository.countByCourseIdAndStatus(a.getCourse().getId(), Enrollment.ACTIVE), LocalDateTime.now());
    }

    @Transactional
    public AssignmentDto removeAttachment(AuthUser me, Long id) {
        Assignment a = loadDetail(id);
        courseService.requireTeacherOf(me, a.getCourse().getId());
        String old = a.getAttachmentUrl();
        a.setAttachmentUrl(null);
        if (old != null) storageService.delete(old);
        return teacherDto(a, enrollmentRepository.countByCourseIdAndStatus(a.getCourse().getId(), Enrollment.ACTIVE), LocalDateTime.now());
    }

    @Transactional
    public void delete(AuthUser me, Long id) {
        Assignment a = loadDetail(id);
        courseService.requireTeacherOf(me, a.getCourse().getId());

        List<String> urls = new ArrayList<>();
        if (a.getAttachmentUrl() != null) urls.add(a.getAttachmentUrl());
        for (Submission s : submissionRepository.findByAssignment(id)) {
            if (s.getFileUrl() != null) urls.add(s.getFileUrl());
        }
        assignmentRepository.delete(a);   // bài nộp bị xóa theo (ON DELETE CASCADE trong DB)
        urls.forEach(storageService::delete);
    }

    // =====================================================================
    //  ĐỌC: danh sách / chi tiết
    // =====================================================================

    @Transactional(readOnly = true)
    public List<AssignmentDto> listByCourse(AuthUser me, Long courseId) {
        courseService.requireAccess(me, courseId);
        List<Assignment> list = assignmentRepository.findByCourse(courseId);
        LocalDateTime now = LocalDateTime.now();
        if (me.role() == Role.STUDENT) {
            Map<Long, Submission> subs = mySubmissions(me.id(), list);
            return list.stream().map(a -> studentDto(a, subs.get(a.getId()), now)).toList();
        }
        long total = enrollmentRepository.countByCourseIdAndStatus(courseId, Enrollment.ACTIVE);
        return list.stream().map(a -> teacherDto(a, total, now)).toList();
    }

    /**
     * Tab "Bài tập" của học viên.
     * type: ESSAY (mặc định, giữ tương thích app cũ) | QUIZ | ALL (gộp bài tập + quiz).
     * status: TODO (chưa nộp / chưa làm / quá hạn) | SUBMITTED (bài tự luận chờ chấm) | GRADED
     *         (đã có điểm — quiz đã nộp nằm ở đây vì chấm tự động) | rỗng = tất cả.
     */
    @Transactional(readOnly = true)
    public List<AssignmentDto> mine(AuthUser me, Long courseId, String status, String type) {
        String t = type == null || type.isBlank() ? "ESSAY" : type.trim().toUpperCase();
        if (!Set.of("ESSAY", "QUIZ", "ALL").contains(t)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "type chỉ nhận ESSAY, QUIZ hoặc ALL");
        }
        LocalDateTime now = LocalDateTime.now();
        List<AssignmentDto> all = new ArrayList<>();
        if (!"QUIZ".equals(t)) {
            List<Assignment> list = assignmentRepository.findAllForStudent(me.id(), courseId == null ? 0L : courseId);
            if (!list.isEmpty()) {
                Map<Long, Submission> subs = mySubmissions(me.id(), list);
                list.forEach(a -> all.add(studentDto(a, subs.get(a.getId()), now)));
            }
        }
        if (!"ESSAY".equals(t)) all.addAll(quizService.mineAsTasks(me, courseId));

        String f = status == null ? "" : status.trim().toUpperCase();
        Comparator<AssignmentDto> byDeadlineAsc = Comparator.comparing(AssignmentDto::deadline,
                Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()));
        switch (f) {
            case "TODO":
                return all.stream().filter(x -> "PENDING".equals(x.status()) || "OVERDUE".equals(x.status()))
                        .sorted(byDeadlineAsc).toList();
            case "SUBMITTED":
                return all.stream().filter(x -> "SUBMITTED".equals(x.status())).sorted(byDeadlineAsc.reversed()).toList();
            case "GRADED":
                return all.stream().filter(x -> "GRADED".equals(x.status())).sorted(byDeadlineAsc.reversed()).toList();
            case "":
                return all.stream().sorted(byDeadlineAsc).toList();
            default:
                throw new AppException(HttpStatus.BAD_REQUEST, "status chỉ nhận TODO, SUBMITTED hoặc GRADED");
        }
    }

    @Transactional(readOnly = true)
    public AssignmentDto detail(AuthUser me, Long id) {
        Assignment a = loadDetail(id);
        Long courseId = a.getCourse().getId();
        courseService.requireAccess(me, courseId);
        LocalDateTime now = LocalDateTime.now();
        if (me.role() == Role.STUDENT) {
            Submission s = submissionRepository.findByAssignmentIdAndStudentId(id, me.id()).orElse(null);
            return studentDto(a, s, now);
        }
        return teacherDto(a, enrollmentRepository.countByCourseIdAndStatus(courseId, Enrollment.ACTIVE), now);
    }

    // =====================================================================
    //  HỌC VIÊN: nộp bài
    // =====================================================================

    @Transactional
    public SubmissionDto submit(AuthUser me, Long assignmentId, MultipartFile file) {
        Assignment a = loadDetail(assignmentId);
        Long courseId = a.getCourse().getId();
        if (!enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(courseId, me.id(), Enrollment.ACTIVE)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học này");
        }
        UploadValidator.require(file, UploadValidator.SUBMISSIONS, MAX_FILE, "bài làm");

        LocalDateTime now = LocalDateTime.now();
        boolean late = a.getDeadline() != null && now.isAfter(a.getDeadline());
        Submission existing = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, me.id()).orElse(null);
        if (existing != null && existing.getScore() != null) {
            throw new AppException(HttpStatus.CONFLICT, "Bài đã được chấm điểm, không thể nộp lại");
        }

        String url = storageService.upload(file, "submissions");
        Submission s;
        String oldUrl = null;
        if (existing == null) {
            User student = userRepository.getReferenceById(me.id());
            s = submissionRepository.save(new Submission(a, student, url, now, late));
            notificationService.create(a.getCourse().getTeacher().getId(), "Có bài nộp mới",
                    student.getFullName() + " đã nộp \"" + a.getTitle() + "\"" + (late ? " (trễ hạn)" : ""),
                    courseId, REF, assignmentId);
        } else {
            oldUrl = existing.getFileUrl();
            existing.resubmit(url, now, late);
            s = existing;
        }
        if (oldUrl != null) storageService.delete(oldUrl);
        return SubmissionDto.from(s);
    }

    /**
     * Học viên xóa bài nộp của mình (để nộp lại file khác). Chỉ xóa được khi CHƯA được chấm điểm.
     * File trên storage bị xóa theo. Sau khi xóa, trạng thái bài quay về PENDING/OVERDUE.
     */
    @Transactional
    public void deleteMySubmission(AuthUser me, Long assignmentId) {
        loadDetail(assignmentId);
        Submission s = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, me.id())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Bạn chưa nộp bài này"));
        if (s.getScore() != null) {
            throw new AppException(HttpStatus.CONFLICT, "Bài đã được chấm điểm, không thể xóa");
        }
        String url = s.getFileUrl();
        submissionRepository.delete(s);
        if (url != null) storageService.delete(url);
    }

    // =====================================================================
    //  GIẢNG VIÊN: xem bài nộp, chấm điểm, nhắc nhở
    // =====================================================================

    @Transactional(readOnly = true)
    public SubmissionsOverviewDto submissions(AuthUser me, Long assignmentId) {
        Assignment a = loadDetail(assignmentId);
        Long courseId = a.getCourse().getId();
        if (me.role() == Role.STUDENT) throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách bài nộp");
        courseService.requireAccess(me, courseId);

        List<Submission> subs = submissionRepository.findByAssignment(assignmentId);
        Set<Long> submittedIds = new HashSet<>();
        subs.forEach(s -> submittedIds.add(s.getStudent().getId()));

        List<StudentBriefDto> notSubmitted = enrollmentRepository.findActiveByCourse(courseId).stream()
                .map(Enrollment::getStudent)
                .filter(u -> !submittedIds.contains(u.getId()))
                .map(StudentBriefDto::from).toList();

        long total = enrollmentRepository.countByCourseIdAndStatus(courseId, Enrollment.ACTIVE);
        return new SubmissionsOverviewDto(teacherDto(a, total, LocalDateTime.now()),
                subs.stream().map(SubmissionDto::from).toList(), notSubmitted);
    }

    @Transactional(readOnly = true)
    public SubmissionDto submissionDetail(AuthUser me, Long submissionId) {
        Submission s = submissionRepository.findDetail(submissionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp"));
        if (me.role() == Role.STUDENT) {
            if (!s.getStudent().getId().equals(me.id())) throw new AppException(HttpStatus.FORBIDDEN, "Đây không phải bài nộp của bạn");
        } else {
            courseService.requireAccess(me, s.getAssignment().getCourse().getId());
        }
        return SubmissionDto.from(s);
    }

    @Transactional
    public SubmissionDto grade(AuthUser me, Long submissionId, GradeRequest req) {
        Submission s = submissionRepository.findDetail(submissionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bài nộp"));
        Assignment a = s.getAssignment();
        courseService.requireTeacherOf(me, a.getCourse().getId());
        if (!a.isGraded()) {
            throw new AppException(HttpStatus.CONFLICT, "Bài tập này không chấm điểm");
        }

        BigDecimal score = req.score().setScale(2, RoundingMode.HALF_UP);
        if (score.compareTo(a.getMaxScore()) > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Điểm không được vượt quá " + plain(a.getMaxScore()));
        }
        s.grade(score, blankToNull(req.feedback()), LocalDateTime.now());
        notificationService.create(s.getStudent().getId(), "Bài tập đã được chấm điểm",
                a.getTitle() + ": " + plain(score) + "/" + plain(a.getMaxScore()),
                a.getCourse().getId(), REF, a.getId());
        return SubmissionDto.from(s);
    }

    @Transactional
    public Map<String, Integer> remindAll(AuthUser me, Long assignmentId) {
        Assignment a = loadDetail(assignmentId);
        Long courseId = a.getCourse().getId();
        courseService.requireTeacherOf(me, courseId);

        Set<Long> submitted = new HashSet<>();
        submissionRepository.findByAssignment(assignmentId).forEach(s -> submitted.add(s.getStudent().getId()));
        List<Long> pending = enrollmentRepository.findActiveStudentIds(courseId).stream()
                .filter(sid -> !submitted.contains(sid)).toList();

        String when = a.getDeadline() == null ? "" : " - hạn nộp " + a.getDeadline().format(FULL);
        notificationService.createForUsers(pending, "Nhắc nộp bài: " + a.getTitle(),
                a.getCourse().getCourseName() + when, courseId, REF, assignmentId);
        return Map.of("reminded", pending.size());
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    private Assignment loadDetail(Long id) {
        return assignmentRepository.findDetail(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập"));
    }

    private Map<Long, Submission> mySubmissions(Long studentId, List<Assignment> list) {
        if (list.isEmpty()) return Map.of();
        List<Long> ids = list.stream().map(Assignment::getId).toList();
        return submissionRepository.findByStudentAndAssignmentIds(studentId, ids).stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity(), (x, y) -> x));
    }

    private AssignmentDto studentDto(Assignment a, Submission s, LocalDateTime now) {
        String status;
        if (s == null) {
            status = a.getDeadline() != null && now.isAfter(a.getDeadline()) ? "OVERDUE" : "PENDING";
        } else {
            // bài không chấm điểm không bao giờ có score nên luôn dừng ở SUBMITTED
            status = s.getScore() != null ? "GRADED" : "SUBMITTED";
        }
        return dto(a, status, s == null ? null : SubmissionDto.from(s), null, null, null, now);
    }

    private AssignmentDto teacherDto(Assignment a, long totalStudents, LocalDateTime now) {
        return dto(a, null, null, submissionRepository.countByAssignmentId(a.getId()),
                submissionRepository.countGraded(a.getId()), totalStudents, now);
    }

    private AssignmentDto dto(Assignment a, String status, SubmissionDto mine,
                              Long submitted, Long graded, Long total, LocalDateTime now) {
        Course c = a.getCourse();
        Long minutesLeft = a.getDeadline() == null ? null : Duration.between(now, a.getDeadline()).toMinutes();
        return new AssignmentDto(a.getId(), "ESSAY", c.getId(), c.getCourseName(), a.getTitle(), a.getDescription(),
                a.getAttachmentUrl(), a.getDeadline(), a.getMaxScore(), a.getCreatedAt(), minutesLeft, status, mine,
                submitted, graded, total, a.isGraded(), null, null, null, null, null);
    }

    private static String requireTitle(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "Tiêu đề không được để trống");
        if (t.length() > 200) throw new AppException(HttpStatus.BAD_REQUEST, "Tiêu đề tối đa 200 ký tự");
        return t;
    }

    private static BigDecimal normalizeMax(BigDecimal max) {
        BigDecimal m = max == null ? BigDecimal.TEN : max;
        if (m.compareTo(BigDecimal.ZERO) <= 0 || m.compareTo(new BigDecimal("999.99")) > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Điểm tối đa không hợp lệ");
        }
        return m.setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
