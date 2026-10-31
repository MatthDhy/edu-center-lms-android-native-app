package com.celehub.backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    @Query("""
            SELECT m FROM Material m LEFT JOIN FETCH m.uploadedBy
            WHERE m.course.id = :courseId ORDER BY m.createdAt DESC, m.id DESC""")
    List<Material> findByCourse(@Param("courseId") Long courseId);

    @Query("SELECT m FROM Material m JOIN FETCH m.course c JOIN FETCH c.teacher LEFT JOIN FETCH m.uploadedBy WHERE m.id = :id")
    Optional<Material> findDetail(@Param("id") Long id);
}