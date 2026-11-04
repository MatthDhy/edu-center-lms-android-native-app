package com.celehub.backend.quiz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
import com.celehub.backend.common.AppException;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseService;
import com.celehub.backend.course.Enrollment;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.quiz.QuizDtos.*;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.UserRepository;

@Service
public class QuizService {
    /** Số lần được quay lại bài đang làm dở; lần thứ 3 trở đi thì tự nộp. */
    public static final int MAX_EXITS = 2;
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final String REF = "QUIZ";

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final StudentAnswerRepository answerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;
    private final QuizExcelService excelService;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       QuestionOptionRepository optionRepository, QuizAttemptRepository attemptRepository,
                       StudentAnswerRepository answerRepository, EnrollmentRepository enrollmentRepository,
                       UserRepository userRepository, CourseService courseService,
                       NotificationService notificationService, QuizExcelService excelService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.notificationService = notificationService;
        this.excelService = excelService;
    }

    private record Grade(int correct, int total, BigDecimal score) {}

    // =====================================================================
    //  GIẢNG VIÊN
    // =====================================================================

    @Transactional
    public QuizDto create(AuthUser me, Long courseId, CreateQuizRequest req) {
        Course c = courseService.requireTeacherOf(me, courseId);
        validateWindow(req.startTime(), req.endTime());
        Quiz q = quizRepository.save(new Quiz(c, req.title().trim(), blankToNull(req.description()),
                req.durationMinutes(), req.startTime(), req.endTime()));
        int n = saveQuestions(q, req.questions());
        return toDto(q, n);
    }

    /** Tạo quiz từ file .xlsx; lỗi trả 400 kèm số dòng. */
    @Transactional
    public QuizDto importExcel(AuthUser me, Long courseId, String title, String description, Integer durationMinutes,
                               LocalDateTime startTime, LocalDateTime endTime, MultipartFile file) {
        if (title == null || title.isBlank()) throw bad("Tên đề không được để trống");
        if (durationMinutes == null || durationMinutes < 1 || durationMinutes > 600) throw bad("Thời gian làm bài phải từ 1 đến 600 phút");
        courseService.requireTeacherOf(me, courseId);
        List<QuizQuestionInput> questions = excelService.parse(file);
        return create(me, courseId, new CreateQuizRequest(title, description, durationMinutes, startTime, endTime, questions));
    }

    public byte[] template() {
        return excelService.template();
    }

    @Transactional(readOnly = true)
    public List<QuizDto> listByCourse(AuthUser me, Long courseId) {
        if (me.role() == Role.STUDENT) throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách quiz của lớp");
        courseService.requireAccess(me, courseId);
        return quizRepository.findByCourse(courseId).stream()
                .map(q -> toDto(q, questionRepository.countByQuizId(q.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public QuizDetailDto preview(AuthUser me, Long quizId) {
        return detail(loadManage(me, quizId));
    }

    @Transactional
    public QuizDto update(AuthUser me, Long quizId, UpdateQuizRequest req) {
        Quiz q = loadManage(me, quizId);
        if (req.title() != null || req.durationMinutes() != null || req.startTime() != null) requireNoAttempts(q);
        if (req.title() != null) {
            String t = req.title().trim();
            if (t.isEmpty()) throw bad("Tên đề không được để trống");
            q.setTitle(t);
        }
        if (req.description() != null) q.setDescription(blankToNull(req.description()));
        if (req.durationMinutes() != null) q.setDurationMinutes(req.durationMinutes());
        if (req.startTime() != null) q.setStartTime(req.startTime());
        if (req.endTime() != null) q.setEndTime(req.endTime());
        validateWindow(q.getStartTime(), q.getEndTime());
        return toDto(q, questionRepository.countByQuizId(q.getId()));
    }

    /** Thay toàn bộ câu hỏi (chỉ khi chưa có ai làm). */
    @Transactional
    public QuizDetailDto replaceQuestions(AuthUser me, Long quizId, List<QuizQuestionInput> inputs) {
        Quiz q = loadManage(me, quizId);
        requireNoAttempts(q);
        if (q.isPublished() && (inputs == null || inputs.isEmpty())) {
            throw new AppException(HttpStatus.CONFLICT, "Quiz đã xuất bản phải có ít nhất 1 câu hỏi");
        }
        questionRepository.deleteByQuiz(q.getId());
        Quiz fresh = quizRepository.findDetail(quizId).orElseThrow();
        saveQuestions(fresh, inputs);
        return detail(fresh);
    }

    @Transactional
    public QuizDto publish(AuthUser me, Long quizId) {
        Quiz q = loadManage(me, quizId);
        long n = questionRepository.countByQuizId(q.getId());
        if (q.isPublished()) return toDto(q, n);
        if (n == 0) throw new AppException(HttpStatus.CONFLICT, "Quiz chưa có câu hỏi nào");
        if (q.getDurationMinutes() == null || q.getDurationMinutes() < 1) throw new AppException(HttpStatus.CONFLICT, "Chưa đặt thời gian làm bài");
        if (q.getEndTime() == null) throw new AppException(HttpStatus.CONFLICT, "Chưa đặt thời gian đóng quiz");
        if (!q.getEndTime().isAfter(LocalDateTime.now())) throw new AppException(HttpStatus.CONFLICT, "Thời gian đóng quiz phải ở tương lai");
        q.setPublished(true);
        Long courseId = q.getCourse().getId();
        notificationService.createForUsers(enrollmentRepository.findActiveStudentIds(courseId),
                "Quiz mới: " + q.getTitle(),
                q.getCourse().getCourseName() + " - đóng lúc " + q.getEndTime().format(FULL),
                courseId, REF, q.getId());
        return toDto(q, n);
    }

    @Transactional
    public void delete(AuthUser me, Long quizId) {
        Quiz q = loadManage(me, quizId);
        requireNoAttempts(q);
        quizRepository.delete(q);
    }

    @Transactional(readOnly = true)
    public QuizResultsDto results(AuthUser me, Long quizId) {
        Quiz q = loadManage(me, quizId);
        Long courseId = q.getCourse().getId();
        List<Enrollment> roster = enrollmentRepository.findActiveByCourse(courseId);
        List<QuizAttempt> attempts = attemptRepository.findByQuiz(quizId);
        Map<Long, QuizAttempt> byStudent = attempts.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity(), (x, y) -> x));

        List<StudentResultDto> rows = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Enrollment e : roster) {
            var u = e.getStudent();
            seen.add(u.getId());
            rows.add(row(u.getId(), u.getFullName(), u.getEmail(), byStudent.get(u.getId())));
        }
        for (QuizAttempt a : attempts) {   // học viên đã rút lớp nhưng có bài làm
            if (seen.add(a.getStudent().getId())) {
                rows.add(row(a.getStudent().getId(), a.getStudent().getFullName(), a.getStudent().getEmail(), a));
            }
        }
        rows.sort(Comparator.comparing(StudentResultDto::fullName, String.CASE_INSENSITIVE_ORDER));

        List<BigDecimal> scores = attempts.stream().filter(a -> a.getSubmittedAt() != null && a.getScore() != null)
                .map(QuizAttempt::getScore).toList();
        BigDecimal avg = scores.isEmpty() ? null
                : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        int[] hist = new int[10];
        for (BigDecimal s : scores) hist[Math.max(0, Math.min(9, s.intValue()))]++;
        int inProgress = (int) attempts.stream().filter(a -> a.getSubmittedAt() == null).count();

        Map<Long, long[]> stats = new HashMap<>();
        for (Object[] r : answerRepository.questionStats(quizId)) {
            stats.put((Long) r[0], new long[]{((Number) r[1]).longValue(), ((Number) r[2]).longValue()});
        }
        List<QuestionStatDto> qStats = new ArrayList<>();
        for (Question qu : questionRepository.findByQuizWithOptions(quizId)) {
            long[] s = stats.getOrDefault(qu.getId(), new long[]{0, 0});
            Double rate = scores.isEmpty() ? null : Math.round(s[1] * 1000.0 / scores.size()) / 10.0;
            qStats.add(new QuestionStatDto(qu.getId(), qu.getOrderIndex(), qu.getContent(), s[0], s[1], rate));
        }

        return new QuizResultsDto(toDto(q, qStats.size()), rows.size(), scores.size(), inProgress, avg,
                scores.stream().max(Comparator.naturalOrder()).orElse(null),
                scores.stream().min(Comparator.naturalOrder()).orElse(null),
                Arrays.stream(hist).boxed().toList(), rows, qStats);
    }

    private StudentResultDto row(Long id, String name, String email, QuizAttempt a) {
        if (a == null) return new StudentResultDto(id, name, email, "NOT_STARTED", null, 0, false, null, null, null);
        boolean done = a.getSubmittedAt() != null;
        Long dur = done ? Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds() : null;
        return new StudentResultDto(id, name, email, done ? "SUBMITTED" : "IN_PROGRESS", done ? a.getScore() : null,
                a.getExitCount(), a.isAutoSubmitted(), a.getStartedAt(), a.getSubmittedAt(), dur);
    }

    // =====================================================================
    //  HỌC VIÊN
    // =====================================================================

    /** Thông tin cho màn Quiz Lobby (số câu, thời gian, quy chế). */
    @Transactional(readOnly = true)
    public QuizDto info(AuthUser me, Long quizId) {
        Quiz q = loadForStudent(me, quizId);
        return toDto(q, questionRepository.countByQuizId(quizId));
    }

    /** Quiz của học viên dưới dạng item của tab Bài tập (gộp với bài tự luận). */
    @Transactional(readOnly = true)
    public List<AssignmentDto> mineAsTasks(AuthUser me, Long courseId) {
        LocalDateTime now = LocalDateTime.now();
        List<Quiz> quizzes = quizRepository.findPublishedForStudent(me.id(), courseId == null ? 0L : courseId, now);
        if (quizzes.isEmpty()) return List.of();
        List<Long> ids = quizzes.stream().map(Quiz::getId).toList();
        Map<Long, QuizAttempt> attempts = attemptRepository.findByStudentAndQuizIds(me.id(), ids).stream()
                .collect(Collectors.toMap(a -> a.getQuiz().getId(), Function.identity(), (x, y) -> x));
        return quizzes.stream().map(q -> quizTask(q, attempts.get(q.getId()), now)).toList();
    }

    private AssignmentDto quizTask(Quiz q, QuizAttempt a, LocalDateTime now) {
        String status;
        String quizStatus;
        BigDecimal score = null;
        if (a != null && a.getSubmittedAt() != null) {
            status = "GRADED";
            quizStatus = "SUBMITTED";
            score = a.getScore();
        } else if (a != null) {
            status = "PENDING";
            quizStatus = "IN_PROGRESS";
        } else if (q.getEndTime() != null && !now.isBefore(q.getEndTime())) {
            status = "OVERDUE";
            quizStatus = "MISSED";
        } else {
            status = "PENDING";
            quizStatus = "NOT_STARTED";
        }
        Course c = q.getCourse();
        Long minutesLeft = q.getEndTime() == null ? null : Duration.between(now, q.getEndTime()).toMinutes();
        return new AssignmentDto(q.getId(), "QUIZ", c.getId(), c.getCourseName(), q.getTitle(), q.getDescription(),
                null, q.getEndTime(), BigDecimal.TEN, q.getCreatedAt(), minutesLeft, status, null,
                null, null, null, true, quizStatus, score, a == null ? 0 : a.getExitCount(),
                q.getDurationMinutes(), a == null ? null : a.getId());
    }

    /**
     * Bắt đầu hoặc TIẾP TỤC quiz. Mỗi lần gọi khi đang có bài làm dở = 1 lần thoát (exitCount++).
     * Hết giờ hoặc exitCount > 2 thì server chấm và trả finished = true.
     * App chỉ gọi 1 lần mỗi khi mở lại màn làm bài (không gọi lặp khi chỉ xoay màn hình / đổi câu).
     */
    @Transactional
    public QuizSessionDto start(AuthUser me, Long quizId) {
        Quiz q = loadForStudent(me, quizId);
        LocalDateTime now = LocalDateTime.now();
        QuizAttempt a = attemptRepository.lockByQuizAndStudent(quizId, me.id()).orElse(null);

        if (a == null) {
            if (q.getStartTime() != null && now.isBefore(q.getStartTime())) throw new AppException(HttpStatus.CONFLICT, "Quiz chưa mở");
            if (q.getEndTime() != null && !now.isBefore(q.getEndTime())) throw new AppException(HttpStatus.CONFLICT, "Quiz đã đóng");
            if (questionRepository.countByQuizId(quizId) == 0) throw new AppException(HttpStatus.CONFLICT, "Quiz chưa có câu hỏi");
            LocalDateTime ends = now.plusMinutes(durationOf(q));
            if (q.getEndTime() != null && q.getEndTime().isBefore(ends)) ends = q.getEndTime();
            a = attemptRepository.save(new QuizAttempt(q, userRepository.getReferenceById(me.id()), now, ends));
            return running(a, q, now);
        }
        if (a.getSubmittedAt() != null) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã hoàn thành quiz này. Mỗi quiz chỉ được làm một lần");
        }
        if (a.getEndsAt() != null && !now.isBefore(a.getEndsAt())) {
            return finished(a, q, finalizeAttempt(a, true, now));
        }
        a.incrementExit();
        if (a.getExitCount() > MAX_EXITS) {
            return finished(a, q, finalizeAttempt(a, true, now));
        }
        return running(a, q, now);
    }

    /** Lưu đáp án tự động mỗi lần chọn. Hết giờ thì chấm luôn (noRollbackFor để giữ kết quả tự nộp). */
    @Transactional(noRollbackFor = AppException.class)
    public void saveAnswer(AuthUser me, Long attemptId, AnswerRequest req) {
        QuizAttempt a = ownAttemptForUpdate(me, attemptId);
        if (a.getSubmittedAt() != null) throw new AppException(HttpStatus.CONFLICT, "Bài đã được nộp");
        LocalDateTime now = LocalDateTime.now();
        if (a.getEndsAt() != null && !now.isBefore(a.getEndsAt())) {
            finalizeAttempt(a, true, now);
            throw new AppException(HttpStatus.CONFLICT, "Đã hết giờ, bài đã được nộp tự động");
        }
        Long quizId = a.getQuiz().getId();
        if (!questionRepository.existsByIdAndQuizId(req.questionId(), quizId)) throw bad("Câu hỏi không thuộc quiz này");
        if (req.optionId() != null && !optionRepository.existsByIdAndQuestionId(req.optionId(), req.questionId())) {
            throw bad("Đáp án không thuộc câu hỏi này");
        }
        StudentAnswer sa = answerRepository.findByAttemptIdAndQuestionId(attemptId, req.questionId()).orElse(null);
        if (sa == null) answerRepository.save(new StudentAnswer(attemptId, req.questionId(), req.optionId()));
        else sa.setSelectedOptionId(req.optionId());
    }

    @Transactional
    public QuizResultDto submit(AuthUser me, Long attemptId) {
        QuizAttempt a = ownAttemptForUpdate(me, attemptId);
        if (a.getSubmittedAt() != null) return resultOf(a, grade(a.getQuiz().getId(), a.getId()));
        return finalizeAttempt(a, false, LocalDateTime.now());
    }

    /** Xem lại từng câu — chỉ sau khi đã nộp. Học viên chỉ xem bài của mình; GV/Admin xem bài của lớp mình. */
    @Transactional(readOnly = true)
    public QuizReviewDto review(AuthUser me, Long attemptId) {
        QuizAttempt a = attemptRepository.findDetail(attemptId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bài làm"));
        Quiz q = a.getQuiz();
        if (me.role() == Role.STUDENT) {
            if (!a.getStudent().getId().equals(me.id())) throw new AppException(HttpStatus.FORBIDDEN, "Đây không phải bài làm của bạn");
        } else {
            courseService.requireAccess(me, q.getCourse().getId());
        }
        if (a.getSubmittedAt() == null) throw new AppException(HttpStatus.CONFLICT, "Chỉ xem lại được sau khi đã nộp bài");

        List<Question> qs = questionRepository.findByQuizWithOptions(q.getId());
        Map<Long, Long> sel = selectedMap(a.getId());
        List<ReviewQuestionDto> out = new ArrayList<>();
        for (Question qu : qs) {
            Long s = sel.get(qu.getId());
            Boolean ok = s == null ? null : qu.getOptions().stream().anyMatch(o -> o.getId().equals(s) && o.isCorrect());
            out.add(new ReviewQuestionDto(qu.getId(), qu.getOrderIndex(), qu.getContent(), qu.getExplanation(),
                    qu.getOptions().stream().map(o -> new OptionDto(o.getId(), o.getContent(), o.isCorrect())).toList(), s, ok));
        }
        return new QuizReviewDto(resultOf(a, grade(q.getId(), a.getId())), q.getTitle(), a.getExitCount(), out);
    }

    // ---------- job tự nộp bài hết giờ ----------

    @Transactional(readOnly = true)
    public List<Long> findExpiredAttemptIds() {
        return attemptRepository.findExpiredIds(LocalDateTime.now());
    }

    @Transactional
    public void autoSubmitOne(Long attemptId) {
        QuizAttempt a = attemptRepository.lockById(attemptId).orElse(null);
        if (a == null || a.getSubmittedAt() != null) return;
        LocalDateTime now = LocalDateTime.now();
        if (a.getEndsAt() == null || now.isBefore(a.getEndsAt())) return;
        finalizeAttempt(a, true, now);
        Quiz q = a.getQuiz();
        notificationService.create(a.getStudent().getId(), "Quiz đã được tự nộp",
                q.getTitle() + ": hết giờ làm bài, hệ thống đã nộp và chấm các câu bạn đã chọn.",
                q.getCourse().getId(), REF, q.getId());
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    private Quiz loadManage(AuthUser me, Long quizId) {
        Quiz q = quizRepository.findDetail(quizId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy quiz"));
        if (me.role() != Role.ADMIN) courseService.requireTeacherOf(me, q.getCourse().getId());
        return q;
    }

    private Quiz loadForStudent(AuthUser me, Long quizId) {
        Quiz q = quizRepository.findDetail(quizId).filter(Quiz::isPublished)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy quiz"));
        if (!enrollmentRepository.existsByCourseIdAndStudentIdAndStatus(q.getCourse().getId(), me.id(), Enrollment.ACTIVE)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc lớp học này");
        }
        return q;
    }

    private QuizAttempt ownAttemptForUpdate(AuthUser me, Long attemptId) {
        QuizAttempt a = attemptRepository.lockById(attemptId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bài làm"));
        if (!a.getStudent().getId().equals(me.id())) throw new AppException(HttpStatus.FORBIDDEN, "Đây không phải bài làm của bạn");
        return a;
    }

    private void requireNoAttempts(Quiz q) {
        if (attemptRepository.countByQuizId(q.getId()) > 0) {
            throw new AppException(HttpStatus.CONFLICT, "Đã có học viên làm bài, không thể sửa phần này");
        }
    }

    private int saveQuestions(Quiz q, List<QuizQuestionInput> inputs) {
        if (inputs == null) return 0;
        int i = 0;
        for (QuizQuestionInput in : inputs) {
            i++;
            String content = in.content() == null ? "" : in.content().trim();
            if (content.isEmpty()) throw bad("Câu " + i + ": nội dung không được để trống");
            List<QuizOptionInput> opts = in.options() == null ? List.of() : in.options();
            if (opts.size() < 2 || opts.size() > 6) throw bad("Câu " + i + ": cần từ 2 đến 6 đáp án");
            if (opts.stream().filter(QuizOptionInput::correct).count() != 1) throw bad("Câu " + i + ": phải có đúng 1 đáp án đúng");
            Question question = new Question(q, content, blankToNull(in.explanation()), i);
            for (QuizOptionInput o : opts) {
                String oc = o.content() == null ? "" : o.content().trim();
                if (oc.isEmpty()) throw bad("Câu " + i + ": đáp án không được để trống");
                if (oc.length() > 500) throw bad("Câu " + i + ": đáp án tối đa 500 ký tự");
                question.addOption(new QuestionOption(question, oc, o.correct()));
            }
            questionRepository.save(question);
        }
        return inputs.size();
    }

    private QuizDetailDto detail(Quiz q) {
        List<Question> qs = questionRepository.findByQuizWithOptions(q.getId());
        return new QuizDetailDto(toDto(q, qs.size()), qs.stream().map(x -> new QuestionDto(x.getId(), x.getOrderIndex(),
                x.getContent(), x.getExplanation(),
                x.getOptions().stream().map(o -> new OptionDto(o.getId(), o.getContent(), o.isCorrect())).toList())).toList());
    }

    private QuizDto toDto(Quiz q, long questionCount) {
        Course c = q.getCourse();
        return new QuizDto(q.getId(), c.getId(), c.getCourseName(), q.getTitle(), q.getDescription(),
                q.getDurationMinutes(), q.getStartTime(), q.getEndTime(), q.isPublished(), questionCount, q.getCreatedAt());
    }

    private QuizSessionDto running(QuizAttempt a, Quiz q, LocalDateTime now) {
        List<Question> qs = questionRepository.findByQuizWithOptions(q.getId());
        long remaining = a.getEndsAt() == null ? 0 : Math.max(0, Duration.between(now, a.getEndsAt()).getSeconds());
        List<StudentQuestionDto> list = qs.stream().map(x -> new StudentQuestionDto(x.getId(), x.getOrderIndex(), x.getContent(),
                x.getOptions().stream().map(o -> new PublicOptionDto(o.getId(), o.getContent())).toList())).toList();
        return new QuizSessionDto(false, false, a.getId(), q.getId(), q.getTitle(), durationOf(q), remaining,
                a.getExitCount(), MAX_EXITS, a.getEndsAt(), list, selectedMap(a.getId()), null, null, null);
    }

    private QuizSessionDto finished(QuizAttempt a, Quiz q, QuizResultDto r) {
        return new QuizSessionDto(true, r.autoSubmitted(), a.getId(), q.getId(), q.getTitle(), durationOf(q), 0,
                a.getExitCount(), MAX_EXITS, a.getEndsAt(), null, null, r.score(), r.correctCount(), r.totalQuestions());
    }

    private QuizResultDto finalizeAttempt(QuizAttempt a, boolean auto, LocalDateTime now) {
        Grade g = grade(a.getQuiz().getId(), a.getId());
        LocalDateTime at = a.getEndsAt() != null && now.isAfter(a.getEndsAt()) ? a.getEndsAt() : now;
        a.finish(g.score(), at, auto);
        return resultOf(a, g);
    }

    private QuizResultDto resultOf(QuizAttempt a, Grade g) {
        return new QuizResultDto(a.getId(), a.getQuiz().getId(), a.getScore(), g.correct(), g.total(),
                a.isAutoSubmitted(), a.getSubmittedAt());
    }

    /** Điểm thang 10 = số câu đúng × 10 / tổng số câu (câu bỏ trống tính sai). */
    private Grade grade(Long quizId, Long attemptId) {
        List<Question> qs = questionRepository.findByQuizWithOptions(quizId);
        Map<Long, Long> sel = selectedMap(attemptId);
        int correct = 0;
        for (Question qu : qs) {
            final Long s = sel.get(qu.getId());
            if (s != null && qu.getOptions().stream().anyMatch(o -> o.getId().equals(s) && o.isCorrect())) correct++;
        }
        BigDecimal score = qs.isEmpty() ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(correct).multiply(BigDecimal.TEN)
                        .divide(BigDecimal.valueOf(qs.size()), 2, RoundingMode.HALF_UP);
        return new Grade(correct, qs.size(), score);
    }

    private Map<Long, Long> selectedMap(Long attemptId) {
        Map<Long, Long> m = new HashMap<>();
        for (StudentAnswer sa : answerRepository.findByAttemptId(attemptId)) {
            if (sa.getSelectedOptionId() != null) m.put(sa.getQuestionId(), sa.getSelectedOptionId());
        }
        return m;
    }

    private static int durationOf(Quiz q) {
        return q.getDurationMinutes() == null ? 30 : q.getDurationMinutes();
    }

    private static void validateWindow(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) throw bad("Thời gian đóng phải sau thời gian mở");
    }

    private static AppException bad(String msg) {
        return new AppException(HttpStatus.BAD_REQUEST, msg);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
