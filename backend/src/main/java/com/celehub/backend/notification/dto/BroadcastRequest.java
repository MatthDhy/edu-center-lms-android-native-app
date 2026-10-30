package com.celehub.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** target: ALL | STUDENTS | TEACHERS | COURSE (COURSE bắt buộc có courseId: gửi cho HV đang học + GV của lớp). */
public record BroadcastRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
        @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự") String content,
        @NotNull(message = "Chọn đối tượng nhận") Target target,
        Long courseId) {

    public enum Target { ALL, STUDENTS, TEACHERS, COURSE }
}
