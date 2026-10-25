package com.celehub.backend.attendance;

import java.util.Collection;

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
}
