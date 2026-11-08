package com.celehub.backend.leave;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.celehub.backend.course.Lesson;
import com.celehub.backend.course.LessonStatus;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT COUNT(r) FROM LeaveRequest r WHERE r.status = :status AND r.lesson.course.teacher.id = :teacherId")
    long countByStatusForTeacher(@Param("teacherId") Long teacherId, @Param("status") LeaveStatus status);

    default long countPendingForTeacher(Long teacherId) {
        return countByStatusForTeacher(teacherId, LeaveStatus.PENDING);
    }

    String DETAIL = """
            SELECT r FROM LeaveRequest r JOIN FETCH r.student JOIN FETCH r.lesson l JOIN FETCH l.course lc JOIN FETCH lc.teacher
            LEFT JOIN FETCH r.makeupLesson m LEFT JOIN FETCH m.course mc LEFT JOIN FETCH mc.teacher """;

    @Query(DETAIL + " WHERE r.student.id = :studentId ORDER BY r.createdAt DESC")
    List<LeaveRequest> findByStudentDetail(@Param("studentId") Long studentId);

    @Query(DETAIL + " WHERE lc.teacher.id = :teacherId ORDER BY r.createdAt DESC")
    List<LeaveRequest> findByTeacherDetail(@Param("teacherId") Long teacherId);

    @Query(DETAIL + " ORDER BY r.createdAt DESC")
    List<LeaveRequest> findAllDetail();

    @Query(DETAIL + " WHERE r.id = :id")
    Optional<LeaveRequest> findDetail(@Param("id") Long id);

    boolean existsByStudentIdAndLessonIdAndStatusIn(Long studentId, Long lessonId, Collection<LeaveStatus> statuses);

    /** Số học viên đã được duyệt học bù vào buổi này (để biết còn chỗ không). */
    @Query("SELECT COUNT(r) FROM LeaveRequest r WHERE r.makeupLesson.id = :lessonId AND r.status = :status")
    long countMakeupForLesson(@Param("lessonId") Long lessonId, @Param("status") LeaveStatus status);

    @Query("""
            SELECT m FROM Lesson m JOIN FETCH m.course c JOIN FETCH c.teacher
            WHERE m.status <> :canceled AND m.startTime >= :from AND m.startTime < :to
              AND m.id IN (SELECT r.makeupLesson.id FROM LeaveRequest r WHERE r.student.id = :sid AND r.status = :approved)
            ORDER BY m.startTime ASC""")
    List<Lesson> findApprovedMakeupLessons(@Param("sid") Long studentId, @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to, @Param("approved") LeaveStatus approved,
                                           @Param("canceled") LessonStatus canceled);

    default List<Lesson> approvedMakeupLessons(Long studentId, LocalDateTime from, LocalDateTime to) {
        return findApprovedMakeupLessons(studentId, from, to, LeaveStatus.APPROVED, LessonStatus.CANCELED);
    }

    @Query("SELECT COUNT(r) FROM LeaveRequest r WHERE r.student.id = :sid AND r.status = :approved AND r.makeupLesson.course.id = :cid")
    long countApprovedMakeupInCourseQ(@Param("sid") Long studentId, @Param("cid") Long courseId,
                                      @Param("approved") LeaveStatus approved);

    default long countApprovedMakeupInCourse(Long studentId, Long courseId) {
        return countApprovedMakeupInCourseQ(studentId, courseId, LeaveStatus.APPROVED);
    }
}
