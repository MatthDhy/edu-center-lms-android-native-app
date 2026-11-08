package com.celehub.backend.leave;

import java.time.LocalDateTime;

import com.celehub.backend.course.Lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class LeaveDtos {
    private LeaveDtos() {}

    public record CreateLeaveRequest(@NotNull(message = "Chọn buổi học xin nghỉ") Long lessonId,
                                     @NotBlank(message = "Vui lòng nhập lý do") @Size(max = 1000, message = "Lý do tối đa 1000 ký tự") String reason,
                                     Long makeupLessonId) {}

    public record ReviewRequest(@Size(max = 1000, message = "Lý do tối đa 1000 ký tự") String reason) {}

    public record SuggestionDto(Long lessonId, Long courseId, String courseName, String teacherName, String room,
                                LocalDateTime startTime, LocalDateTime endTime, int availableSeats) {}

    public record LeaveDto(Long id, Long studentId, String studentName,
                           Long lessonId, Long courseId, String courseName, LocalDateTime lessonStart, LocalDateTime lessonEnd, String room,
                           Long makeupLessonId, String makeupCourseName, LocalDateTime makeupStart, LocalDateTime makeupEnd, String makeupRoom,
                           String reason, String status, String rejectReason, LocalDateTime createdAt) {
        public static LeaveDto from(LeaveRequest r) {
            Lesson l = r.getLesson();
            Lesson m = r.getMakeupLesson();
            return new LeaveDto(r.getId(), r.getStudent().getId(), r.getStudent().getFullName(),
                    l.getId(), l.getCourse().getId(), l.getCourse().getCourseName(), l.getStartTime(), l.getEndTime(), l.getRoom(),
                    m == null ? null : m.getId(), m == null ? null : m.getCourse().getCourseName(),
                    m == null ? null : m.getStartTime(), m == null ? null : m.getEndTime(), m == null ? null : m.getRoom(),
                    r.getReason(), r.getStatus().name(), r.getRejectReason(), r.getCreatedAt());
        }
    }
}
