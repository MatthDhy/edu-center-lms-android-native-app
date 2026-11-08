package com.celehub.backend.leave;

import java.util.List;

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

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.leave.LeaveDtos.*;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LeaveController {
    private final LeaveService service;

    public LeaveController(LeaveService service) {
        this.service = service;
    }

    // ---- Học viên ----
    @GetMapping("/leave-requests/suggestions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<SuggestionDto>> suggestions(@AuthenticationPrincipal AuthUser me, @RequestParam Long lessonId) {
        return ApiResponse.success(service.suggestions(me, lessonId));
    }

    @PostMapping("/leave-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<LeaveDto> create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateLeaveRequest req) {
        return ApiResponse.success("Đã gửi đơn xin nghỉ", service.create(me, req));
    }

    @GetMapping("/leave-requests/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<LeaveDto>> mine(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(service.mine(me));
    }

    // ---- Giảng viên (đơn của lớp mình) / Admin (toàn trung tâm, chỉ xem) ----
    @GetMapping({"/teacher/leave-requests", "/admin/leave-requests"})
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<LeaveDto>> list(@AuthenticationPrincipal AuthUser me,
                                            @RequestParam(required = false) LeaveStatus status) {
        return ApiResponse.success(service.list(me, status));
    }

    @PatchMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<LeaveDto> approve(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success("Đã duyệt đơn", service.approve(me, id));
    }

    @PatchMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<LeaveDto> reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                        @Valid @RequestBody(required = false) ReviewRequest req) {
        return ApiResponse.success("Đã từ chối đơn", service.reject(me, id, req == null ? null : req.reason()));
    }
}
