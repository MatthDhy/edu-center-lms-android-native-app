package com.celehub.backend.attendance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    @Query("""
            SELECT s FROM AttendanceSession s JOIN FETCH s.course c JOIN FETCH c.teacher LEFT JOIN FETCH s.lesson
            WHERE s.id = :id""")
    Optional<AttendanceSession> findDetail(@Param("id") Long id);

    @Query("SELECT s FROM AttendanceSession s JOIN FETCH s.course c WHERE s.pinCode = :pin AND s.expiresAt > :now")
    List<AttendanceSession> findOpenByPin(@Param("pin") String pin, @Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM AttendanceSession s LEFT JOIN FETCH s.lesson
            WHERE s.course.id = :courseId AND s.startTime >= :from AND s.startTime < :to
            ORDER BY s.startTime ASC""")
    List<AttendanceSession> findByCourseBetween(@Param("courseId") Long courseId, @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    @Query("SELECT s FROM AttendanceSession s WHERE s.course.id = :courseId ORDER BY s.startTime DESC")
    List<AttendanceSession> findByCourseDesc(@Param("courseId") Long courseId);

    /** Đóng mọi phiên đang mở của lớp (mỗi lớp chỉ 1 phiên mở, để PIN không nhầm). */
    @Modifying
    @Query("UPDATE AttendanceSession s SET s.expiresAt = :now WHERE s.course.id = :courseId AND s.expiresAt > :now")
    int closeOpen(@Param("courseId") Long courseId, @Param("now") LocalDateTime now);
}
