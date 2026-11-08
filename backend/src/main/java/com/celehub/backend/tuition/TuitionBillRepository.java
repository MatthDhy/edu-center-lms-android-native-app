package com.celehub.backend.tuition;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TuitionBillRepository extends JpaRepository<TuitionBill, Long> {

    @Query("SELECT b FROM TuitionBill b JOIN FETCH b.student JOIN FETCH b.course WHERE b.id = :id")
    Optional<TuitionBill> findDetail(@Param("id") Long id);

    @Query("SELECT b FROM TuitionBill b JOIN FETCH b.student JOIN FETCH b.course WHERE b.transferContent = :content")
    Optional<TuitionBill> findByTransferContentDetail(@Param("content") String content);

    @Query("SELECT b FROM TuitionBill b JOIN FETCH b.course WHERE b.student.id = :studentId ORDER BY b.createdAt DESC, b.id DESC")
    List<TuitionBill> findByStudent(@Param("studentId") Long studentId);

    boolean existsByTransferContent(String content);

    boolean existsByStudentIdAndCourseIdAndTitle(Long studentId, Long courseId, String title);
}
