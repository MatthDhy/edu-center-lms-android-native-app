package com.celehub.backend.attendance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.attendance.AttendanceDtos.*;
import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AttendanceController {
    private final AttendanceService service;
    private final AttendanceReportService reportService;

    public AttendanceController(AttendanceService service, AttendanceReportService reportService) {
        this.service = service;
        this.reportService = reportService;
    }

    // ---- Giảng viên ----
    @PostMapping("/attendance/sessions")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<SessionDto> create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateSessionRequest req) {
        return ApiResponse.success("Đã tạo phiên điểm danh", service.create(me, req));
    }

    /** App giảng viên gọi mỗi 3 giây: danh sách tick + qrPayload mới nhất. */
    @GetMapping("/attendance/sessions/{id}/live")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<LiveDto> live(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(service.live(me, id));
    }

    @PostMapping("/attendance/sessions/{id}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> close(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.close(me, id);
        return ApiResponse.success("Đã đóng phiên điểm danh", null);
    }

    @PatchMapping("/attendance/records")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateRecordRequest req) {
        service.updateRecord(me, req);
        return ApiResponse.success("Đã cập nhật điểm danh", null);
    }

    @GetMapping("/attendance/sessions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<SessionDto>> sessions(@AuthenticationPrincipal AuthUser me, @RequestParam Long courseId) {
        return ApiResponse.success(service.sessions(me, courseId));
    }

    /** Ma trận chuyên cần. Admin dùng /api/admin/attendance, giảng viên dùng /api/attendance/stats. */
    @GetMapping({"/attendance/stats", "/admin/attendance"})
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MatrixDto> stats(@AuthenticationPrincipal AuthUser me, @RequestParam Long courseId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(reportService.matrix(me, courseId, from, to));
    }

    // ---- Học viên ----
    @PostMapping("/attendance/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CheckInResultDto> checkIn(@AuthenticationPrincipal AuthUser me, @RequestBody CheckInRequest req) {
        return ApiResponse.success("Điểm danh thành công", service.checkIn(me, req));
    }

    @GetMapping("/attendance/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<MyRecordDto>> mine(@AuthenticationPrincipal AuthUser me, @RequestParam Long courseId) {
        return ApiResponse.success(service.mine(me, courseId));
    }
}
