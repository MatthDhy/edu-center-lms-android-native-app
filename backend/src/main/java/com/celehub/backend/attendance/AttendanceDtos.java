package com.celehub.backend.attendance;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record CreateSessionRequest(@NotNull(message = "Chọn lớp học") Long courseId, Long lessonId,
                                       @Min(value = 1, message = "Thời lượng tối thiểu 1 phút")
                                       @Max(value = 60, message = "Thời lượng tối đa 60 phút") Integer durationMinutes) {}

    /** qrPayload đổi mỗi 30 giây: app giảng viên gọi lại /live (3s/lần) và vẽ lại QR khi payload đổi. */
    public record SessionDto(Long id, Long courseId, String courseName, Long lessonId, String pinCode,
                             String qrPayload, int qrExpiresInSeconds, LocalDateTime startTime,
                             LocalDateTime expiresAt, boolean open, long studentCount) {}

    public record LiveStudentDto(Long studentId, String fullName, String avatarUrl, String status,
                                 String method, LocalDateTime checkInTime) {}

    public record LiveDto(SessionDto session, int presentCount, int total, List<LiveStudentDto> students) {}

    /** Gửi 1 trong 2: qrPayload (quét QR) hoặc pinCode (nhập tay). */
    public record CheckInRequest(String qrPayload, String pinCode) {}

    public record CheckInResultDto(Long sessionId, Long courseId, String courseName, String status,
                                   LocalDateTime checkInTime) {}

    public record UpdateRecordRequest(@NotNull(message = "Thiếu sessionId") Long sessionId,
                                      @NotNull(message = "Thiếu studentId") Long studentId,
                                      @NotNull(message = "Thiếu trạng thái") AttendanceStatus status) {}

    public record MyRecordDto(Long sessionId, LocalDateTime startTime, String status, String method) {}

    // ---------- ma trận chuyên cần ----------
    public record MatrixSessionDto(Long id, LocalDateTime startTime) {}

    /** cells[i] ứng với sessions[i]: PRESENT | LATE | EXCUSED | ABSENT. warning = vắng > 20% số buổi. */
    public record MatrixStudentDto(Long studentId, String fullName, List<String> cells, int present, int late,
                                   int excused, int absent, double absentRate, boolean warning) {}

    public record MatrixDto(Long courseId, String courseName, List<MatrixSessionDto> sessions,
                            List<MatrixStudentDto> students, double avgAttendanceRate, int warningCount) {}
}
