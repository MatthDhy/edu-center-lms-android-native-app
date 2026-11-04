package com.celehub.backend.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.assignment.Assignment;
import com.celehub.backend.assignment.AssignmentRepository;
import com.celehub.backend.assignment.SubmissionRepository;
import com.celehub.backend.course.Enrollment;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.quiz.Quiz;
import com.celehub.backend.quiz.QuizAttemptRepository;
import com.celehub.backend.quiz.QuizRepository;
import com.celehub.backend.security.AuthUser;

@Service
public class TeacherTaskService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final EnrollmentRepository enrollmentRepository;

    public TeacherTaskService(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                              QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
                              EnrollmentRepository enrollmentRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /** courseId = null: mọi lớp của giảng viên. Mới tạo nhất lên đầu. */
    @Transactional(readOnly = true)
    public List<TeacherTaskDto> list(AuthUser me, Long courseId) {
        long cid = courseId == null ? 0L : courseId;
        Map<Long, Long> totals = new HashMap<>();
        List<TeacherTaskDto> out = new ArrayList<>();

        for (Assignment a : assignmentRepository.findForTeacher(me.id(), cid)) {
            Long cId = a.getCourse().getId();
            long total = totals.computeIfAbsent(cId, id -> enrollmentRepository.countByCourseIdAndStatus(id, Enrollment.ACTIVE));
            out.add(new TeacherTaskDto("ESSAY", a.getId(), cId, a.getCourse().getCourseName(), a.getTitle(),
                    a.getDeadline(), a.isGraded(), true, submissionRepository.countByAssignmentId(a.getId()),
                    submissionRepository.countGraded(a.getId()), total, null, a.getCreatedAt()));
        }
        for (Quiz q : quizRepository.findForTeacher(me.id(), cid)) {
            Long cId = q.getCourse().getId();
            long total = totals.computeIfAbsent(cId, id -> enrollmentRepository.countByCourseIdAndStatus(id, Enrollment.ACTIVE));
            long done = attemptRepository.countSubmitted(q.getId());
            Double avg = attemptRepository.avgScore(q.getId());
            out.add(new TeacherTaskDto("QUIZ", q.getId(), cId, q.getCourse().getCourseName(), q.getTitle(),
                    q.getEndTime(), true, q.isPublished(), done, done, total,
                    avg == null ? null : Math.round(avg * 100) / 100.0, q.getCreatedAt()));
        }
        out.sort(Comparator.comparing(TeacherTaskDto::createdAt, Comparator.reverseOrder()));
        return out;
    }
}
