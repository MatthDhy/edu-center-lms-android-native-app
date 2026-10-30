package com.celehub.backend.assignment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.assignment.dto.AssignmentDto;
import com.celehub.backend.assignment.dto.GradeRequest;
import com.celehub.backend.assignment.dto.SubmissionDto;
import com.celehub.backend.assignment.dto.SubmissionsOverviewDto;
import com.celehub.backend.assignment.dto.UpdateAssignmentRequest;
import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AssignmentController {
    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // ---------------- Giảng viên: giao bài ----------------

    /** multipart/form-data: title, description?, deadline (2026-10-30T23:59:00), maxScore?, file? */
    @PostMapping(value = "/courses/{courseId}/assignments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<AssignmentDto> create(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.success("Đã giao bài tập",
                assignmentService.create(me, courseId, title, description, deadline, maxScore, file));
    }

    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<AssignmentDto> update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                             @Valid @RequestBody UpdateAssignmentRequest req) {
        return ApiResponse.success("Đã cập nhật bài tập", assignmentService.update(me, id, req));
    }

    @PostMapping(value = "/assignments/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<AssignmentDto> replaceAttachment(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                        @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Đã cập nhật tệp đính kèm", assignmentService.replaceAttachment(me, id, file));
    }

    @DeleteMapping("/assignments/{id}/attachment")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<AssignmentDto> removeAttachment(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success("Đã xóa tệp đính kèm", assignmentService.removeAttachment(me, id));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        assignmentService.delete(me, id);
        return ApiResponse.success("Đã xóa bài tập", null);
    }

    // ---------------- Đọc ----------------

    @GetMapping("/courses/{courseId}/assignments")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<List<AssignmentDto>> listByCourse(@AuthenticationPrincipal AuthUser me,
                                                         @PathVariable Long courseId) {
        return ApiResponse.success(assignmentService.listByCourse(me, courseId));
    }

    /** Tab Bài tập của học viên: ?courseId=&status=TODO|SUBMITTED|GRADED */
    @GetMapping("/assignments/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<AssignmentDto>> mine(@AuthenticationPrincipal AuthUser me,
                                                 @RequestParam(required = false) Long courseId,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.success(assignmentService.mine(me, courseId, status));
    }

    @GetMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<AssignmentDto> detail(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(assignmentService.detail(me, id));
    }

    // ---------------- Học viên: nộp bài ----------------

    /** multipart/form-data, part tên "file" (pdf/doc/docx/jpg/png/webp/heic ≤ 20MB). Tên file phải có đuôi. */
    @PostMapping(value = "/assignments/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SubmissionDto> submit(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                             @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Đã nộp bài", assignmentService.submit(me, id, file));
    }

    // ---------------- Giảng viên: chấm bài ----------------

    @GetMapping("/assignments/{id}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<SubmissionsOverviewDto> submissions(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(assignmentService.submissions(me, id));
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<SubmissionDto> submissionDetail(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(assignmentService.submissionDetail(me, id));
    }

    @PatchMapping("/submissions/{id}/grade")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<SubmissionDto> grade(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                            @Valid @RequestBody GradeRequest req) {
        return ApiResponse.success("Đã chấm điểm", assignmentService.grade(me, id, req));
    }

    @PostMapping("/assignments/{id}/remind-all")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Integer>> remindAll(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success("Đã gửi nhắc nhở", assignmentService.remindAll(me, id));
    }
}
