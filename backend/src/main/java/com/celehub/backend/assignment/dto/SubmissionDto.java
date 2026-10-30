package com.celehub.backend.assignment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.celehub.backend.assignment.Submission;
import com.celehub.backend.user.User;

/** status: SUBMITTED (chưa chấm) | GRADED. Gọi from() trong transaction. */
public record SubmissionDto(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String studentEmail,
        String studentAvatarUrl,
        String fileUrl,
        LocalDateTime submittedAt,
        boolean late,
        BigDecimal score,
        String feedback,
        LocalDateTime gradedAt,
        String status) {

    public static SubmissionDto from(Submission s) {
        User st = s.getStudent();
        return new SubmissionDto(s.getId(), s.getAssignment().getId(), st.getId(), st.getFullName(), st.getEmail(),
                st.getAvatarUrl(), s.getFileUrl(), s.getSubmittedAt(), s.isLate(), s.getScore(), s.getFeedback(),
                s.getGradedAt(), s.getScore() == null ? "SUBMITTED" : "GRADED");
    }
}
