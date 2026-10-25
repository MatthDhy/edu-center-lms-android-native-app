package com.celehub.backend.home;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.assignment.AssignmentRepository;
import com.celehub.backend.assignment.SubmissionRepository;
import com.celehub.backend.attendance.AttendanceStatsService;
import com.celehub.backend.common.AppException;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.course.Lesson;
import com.celehub.backend.course.LessonRepository;
import com.celehub.backend.home.dto.HomeStudentResponse;
import com.celehub.backend.home.dto.HomeTeacherResponse;
import com.celehub.backend.home.dto.LessonSummaryDto;
import com.celehub.backend.home.dto.SubmissionSummaryDto;
import com.celehub.backend.home.dto.TaskDto;
import com.celehub.backend.leave.LeaveRequestRepository;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.quiz.QuizRepository;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.dto.UserDto;

@Service
@Transactional(readOnly = true)
public class HomeService {
    private static final int MAX_TASKS = 5;
    private static final int RECENT_NOTIFICATIONS = 3;
    private static final int RECENT_SUBMISSIONS = 5;
    private static final long URGENT_MINUTES = 24 * 60;

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;
    private final AttendanceStatsService attendanceStatsService;

    public HomeService(UserRepository userRepository, LessonRepository lessonRepository,
                       EnrollmentRepository enrollmentRepository, AssignmentRepository assignmentRepository,
                       SubmissionRepository submissionRepository, QuizRepository quizRepository,
                       LeaveRequestRepository leaveRequestRepository, NotificationService notificationService,
                       AttendanceStatsService attendanceStatsService) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.quizRepository = quizRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.notificationService = notificationService;
        this.attendanceStatsService = attendanceStatsService;
    }

    // ===================== STUDENT =====================

    public HomeStudentResponse student(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        User u = loadUser(userId);

        LessonSummaryDto next = lessonRepository.findUpcomingForStudent(userId, now, 1).stream()
                .findFirst().map(l -> toSummary(l, now, null)).orElse(null);

        List<TaskDto> tasks = new ArrayList<>();
        assignmentRepository.findOpenForStudent(userId, now, PageRequest.of(0, MAX_TASKS)).forEach(a ->
                tasks.add(toTask(a.getId(), "ESSAY", a.getTitle(), a.getCourse().getCourseName(), a.getDeadline(), now)));
        quizRepository.findOpenForStudent(userId, now, PageRequest.of(0, MAX_TASKS)).forEach(q ->
                tasks.add(toTask(q.getId(), "QUIZ", q.getTitle(), q.getCourse().getCourseName(), q.getEndTime(), now)));
        tasks.sort(Comparator.comparing(TaskDto::deadline));
        List<TaskDto> topTasks = tasks.size() > MAX_TASKS ? tasks.subList(0, MAX_TASKS) : tasks;

        return new HomeStudentResponse(
                UserDto.from(u),
                notificationService.unreadCount(userId),
                next,
                attendanceStatsService.studentSummary(userId),
                topTasks,
                notificationService.recent(userId, RECENT_NOTIFICATIONS));
    }

    // ===================== TEACHER =====================

    public HomeTeacherResponse teacher(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        User u = loadUser(userId);

        LessonSummaryDto next = lessonRepository.findUpcomingForTeacher(userId, now, 1).stream()
                .findFirst().map(l -> toTeacherSummary(l, now)).orElse(null);

        List<LessonSummaryDto> todayLessons = lessonRepository
                .findForTeacherBetween(userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .map(l -> toTeacherSummary(l, now)).toList();

        List<SubmissionSummaryDto> recent = submissionRepository
                .findPendingGrading(userId, PageRequest.of(0, RECENT_SUBMISSIONS)).stream()
                .map(s -> new SubmissionSummaryDto(s.getId(), s.getAssignment().getId(), s.getAssignment().getTitle(),
                        s.getAssignment().getCourse().getCourseName(), s.getStudent().getFullName(),
                        s.getSubmittedAt(), s.isLate()))
                .toList();

        return new HomeTeacherResponse(
                UserDto.from(u),
                notificationService.unreadCount(userId),
                next,
                todayLessons,
                submissionRepository.countPendingGrading(userId),
                leaveRequestRepository.countPendingForTeacher(userId),
                lessonRepository.countTaughtByTeacher(userId, today.withDayOfMonth(1).atStartOfDay(), now),
                recent,
                notificationService.recent(userId, RECENT_NOTIFICATIONS));
    }

    // ===================== helpers =====================

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private LessonSummaryDto toTeacherSummary(Lesson l, LocalDateTime now) {
        int students = (int) enrollmentRepository.countByCourseIdAndStatus(l.getCourse().getId(), Course.ACTIVE);
        return toSummary(l, now, students);
    }

    private LessonSummaryDto toSummary(Lesson l, LocalDateTime now, Integer studentCount) {
        Course c = l.getCourse();
        boolean ongoing = !now.isBefore(l.getStartTime());
        long minutes = ongoing ? 0 : Duration.between(now, l.getStartTime()).toMinutes();
        return new LessonSummaryDto(l.getId(), c.getId(), c.getCourseName(), c.getTeacher().getFullName(),
                l.getRoom(), l.getTopic(), l.getStartTime(), l.getEndTime(), minutes, ongoing, studentCount);
    }

    private TaskDto toTask(Long id, String type, String title, String courseName, LocalDateTime deadline, LocalDateTime now) {
        long minutesLeft = Duration.between(now, deadline).toMinutes();
        return new TaskDto(id, type, title, courseName, deadline, minutesLeft, minutesLeft <= URGENT_MINUTES);
    }
}
