package com.celehub.backend.course;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /** Lịch của học viên trong khoảng [from, to) — gồm cả buổi đã hủy để app hiển thị nhãn "Đã hủy". */
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE l.startTime >= :from AND l.startTime < :to
              AND c.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')
            ORDER BY l.startTime ASC""")
    List<Lesson> findScheduleForStudent(@Param("studentId") Long studentId, @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

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

    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE c.teacher.id = :teacherId AND l.startTime >= :from AND l.startTime < :to
            ORDER BY l.startTime ASC""")
    List<Lesson> findScheduleForTeacher(@Param("teacherId") Long teacherId, @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    // ---------- Admin / dùng chung ----------
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE c.id = :courseId ORDER BY l.startTime ASC""")
    List<Lesson> findByCourse(@Param("courseId") Long courseId);

    @Query("SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher WHERE l.id = :id")
    Optional<Lesson> findDetail(@Param("id") Long id);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.course.id = :courseId AND l.status <> :canceled")
    long countActiveByCourse(@Param("courseId") Long courseId, @Param("canceled") LessonStatus canceled);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.course.id = :courseId AND l.status <> :canceled AND l.endTime < :now")
    long countHeldByCourse(@Param("courseId") Long courseId, @Param("now") LocalDateTime now,
                           @Param("canceled") LessonStatus canceled);

    @Query("""
            SELECT l FROM Lesson l
            WHERE l.course.id = :courseId AND l.status = :scheduled AND l.startTime > :now
            ORDER BY l.startTime ASC""")
    List<Lesson> findFutureScheduled(@Param("courseId") Long courseId, @Param("now") LocalDateTime now,
                                     @Param("scheduled") LessonStatus scheduled);

    /** Buổi của CÙNG giảng viên bị chồng giờ [from, to). excludeId = 0 nếu đang tạo mới. */
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE l.status <> :canceled AND l.startTime < :to AND l.endTime > :from
              AND l.id <> :excludeId AND c.teacher.id = :teacherId
            ORDER BY l.startTime ASC""")
    List<Lesson> findTeacherOverlaps(@Param("teacherId") Long teacherId, @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to, @Param("excludeId") Long excludeId,
                                     @Param("canceled") LessonStatus canceled);

    /** Buổi bị chồng giờ trong CÙNG phòng (so sánh không phân biệt hoa thường). */
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE l.status <> :canceled AND l.startTime < :to AND l.endTime > :from
              AND l.id <> :excludeId AND LOWER(l.room) = LOWER(:room)
            ORDER BY l.startTime ASC""")
    List<Lesson> findRoomOverlaps(@Param("room") String room, @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to, @Param("excludeId") Long excludeId,
                                  @Param("canceled") LessonStatus canceled);

    /** Thời khóa biểu toàn trung tâm trong [from, to) — dùng cho trang Timetable của Web Admin. */
    @Query("""
            SELECT l FROM Lesson l JOIN FETCH l.course c JOIN FETCH c.teacher
            WHERE l.startTime >= :from AND l.startTime < :to
            ORDER BY l.startTime ASC""")
    List<Lesson> findAllBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
