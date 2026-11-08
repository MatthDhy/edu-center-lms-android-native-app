package com.celehub.backend.attendance;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /** Số lần có mặt của học viên trong các lớp đang theo. */
    @Query("""
            SELECT COUNT(r) FROM AttendanceRecord r
            WHERE r.student.id = :studentId AND r.status IN :statuses
              AND r.session.course.id IN (SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE')""")
    long countAttended(@Param("studentId") Long studentId, @Param("statuses") Collection<AttendanceStatus> statuses);

    @Query("SELECT r FROM AttendanceRecord r JOIN FETCH r.student WHERE r.session.id = :sid")
    List<AttendanceRecord> findBySession(@Param("sid") Long sessionId);

    @Query("SELECT r FROM AttendanceRecord r WHERE r.session.id IN :ids")
    List<AttendanceRecord> findBySessionIds(@Param("ids") Collection<Long> ids);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("""
            SELECT r FROM AttendanceRecord r JOIN FETCH r.session s
            WHERE r.student.id = :sid AND s.course.id = :cid ORDER BY s.startTime DESC""")
    List<AttendanceRecord> findMine(@Param("sid") Long studentId, @Param("cid") Long courseId);
}
