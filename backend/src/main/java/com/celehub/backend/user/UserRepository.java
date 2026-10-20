package com.celehub.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);

    long countByRole(Role role);
    long countByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR u.phone LIKE CONCAT('%', :search, '%')) " +
           "ORDER BY u.id DESC")
    List<User> searchUsers(
            @Param("role") Role role,
            @Param("status") UserStatus status,
            @Param("search") String search);
}

