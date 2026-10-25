package com.celehub.backend.assignment;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** Bài nộp chưa chấm (score IS NULL) thuộc các lớp của giảng viên. */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.course.teacher.id = :teacherId AND s.score IS NULL")
    long countPendingGrading(@Param("teacherId") Long teacherId);

    @Query("""
            SELECT s FROM Submission s JOIN FETCH s.student JOIN FETCH s.assignment a JOIN FETCH a.course c
            WHERE c.teacher.id = :teacherId AND s.score IS NULL
            ORDER BY s.submittedAt DESC""")
    List<Submission> findPendingGrading(@Param("teacherId") Long teacherId, Pageable pageable);
}
