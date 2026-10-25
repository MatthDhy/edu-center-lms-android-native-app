package com.celehub.backend.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @Query("SELECT COUNT(r) FROM LeaveRequest r WHERE r.status = :status AND r.lesson.course.teacher.id = :teacherId")
    long countByStatusForTeacher(@Param("teacherId") Long teacherId, @Param("status") LeaveStatus status);

    default long countPendingForTeacher(Long teacherId) {
        return countByStatusForTeacher(teacherId, LeaveStatus.PENDING);
    }
}
