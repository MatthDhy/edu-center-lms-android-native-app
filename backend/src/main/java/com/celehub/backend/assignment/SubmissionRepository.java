package com.celehub.backend.assignment;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    long countByAssignmentId(Long assignmentId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.id = :assignmentId AND s.score IS NOT NULL")
    long countGraded(@Param("assignmentId") Long assignmentId);

    @Query("SELECT MAX(s.score) FROM Submission s WHERE s.assignment.id = :assignmentId")
    BigDecimal maxScoreGiven(@Param("assignmentId") Long assignmentId);

    @Query("""
            SELECT s FROM Submission s JOIN FETCH s.student
            WHERE s.assignment.id = :assignmentId
            ORDER BY s.submittedAt DESC""")
    List<Submission> findByAssignment(@Param("assignmentId") Long assignmentId);

    @Query("""
            SELECT s FROM Submission s JOIN FETCH s.student JOIN FETCH s.assignment a JOIN FETCH a.course c JOIN FETCH c.teacher
            WHERE s.id = :id""")
    Optional<Submission> findDetail(@Param("id") Long id);

    @Query("SELECT s FROM Submission s WHERE s.student.id = :studentId AND s.assignment.id IN :ids")
    List<Submission> findByStudentAndAssignmentIds(@Param("studentId") Long studentId,
                                                   @Param("ids") Collection<Long> ids);
}
