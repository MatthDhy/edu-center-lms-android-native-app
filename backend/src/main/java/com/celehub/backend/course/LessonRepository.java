package com.celehub.backend.course;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    // ---------- Học viên ----------
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE l.status = :scheduled AND l.endTime > :now
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
            ORDER BY l.startTime ASC""")
    List<Lesson> findUpcomingForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now,
                                        @Param("scheduled") LessonStatus scheduled, Pageable pageable);

    default List<Lesson> findUpcomingForStudent(Long studentId, LocalDateTime now, int limit) {
        return findUpcomingForStudent(studentId, now, LessonStatus.SCHEDULED, PageRequest.of(0, limit));
    }

    /** Số buổi ĐÃ diễn ra (đã kết thúc, không bị hủy) của các lớp học viên đang theo. */
    @Query("""
            SELECT COUNT(l) FROM Lesson l
            WHERE l.status <> :canceled AND l.endTime < :now
              AND l.course.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')""")
    long countHeldForStudent(@Param("studentId") Long studentId, @Param("now") LocalDateTime now,
                             @Param("canceled") LessonStatus canceled);

    default long countHeldForStudent(Long studentId, LocalDateTime now) {
        return countHeldForStudent(studentId, now, LessonStatus.CANCELED);
    }

    // ---------- Giảng viên ----------
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c
            WHERE c.teacher.id = :teacherId AND l.status = :scheduled AND l.endTime > :now
            ORDER BY l.startTime ASC""")
    List<Lesson> findUpcomingForTeacher(@Param("teacherId") Long teacherId, @Param("now") LocalDateTime now,
                                        @Param("scheduled") LessonStatus scheduled, Pageable pageable);

    default List<Lesson> findUpcomingForTeacher(Long teacherId, LocalDateTime now, int limit) {
        return findUpcomingForTeacher(teacherId, now, LessonStatus.SCHEDULED, PageRequest.of(0, limit));
    }

    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c
            WHERE c.teacher.id = :teacherId AND l.status <> :canceled
              AND l.startTime >= :from AND l.startTime < :to
            ORDER BY l.startTime ASC""")
    List<Lesson> findForTeacherBetween(@Param("teacherId") Long teacherId, @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to, @Param("canceled") LessonStatus canceled);

    default List<Lesson> findForTeacherBetween(Long teacherId, LocalDateTime from, LocalDateTime to) {
        return findForTeacherBetween(teacherId, from, to, LessonStatus.CANCELED);
    }

    /** Số buổi giảng viên đã dạy (đã kết thúc, không hủy) kể từ mốc `from`. */
    @Query("""
            SELECT COUNT(l) FROM Lesson l
            WHERE l.course.teacher.id = :teacherId AND l.status <> :canceled
              AND l.endTime < :now AND l.startTime >= :from""")
    long countTaughtByTeacher(@Param("teacherId") Long teacherId, @Param("from") LocalDateTime from,
                              @Param("now") LocalDateTime now, @Param("canceled") LessonStatus canceled);

    default long countTaughtByTeacher(Long teacherId, LocalDateTime from, LocalDateTime now) {
        return countTaughtByTeacher(teacherId, from, now, LessonStatus.CANCELED);
    }
}
