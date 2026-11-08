package com.celehub.backend.grades;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.grades.GradesService.CourseGrades;
import com.celehub.backend.security.AuthUser;

/** "Hồ sơ & Bảng điểm" của học viên: GET /api/me/grades[?courseId=] */
@RestController
@RequestMapping("/api/me")
public class GradesController {
    private final GradesService service;

    public GradesController(GradesService service) {
        this.service = service;
    }

    @GetMapping("/grades")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<CourseGrades>> grades(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(required = false) Long courseId) {
        return ApiResponse.success(service.mine(me.id(), courseId));
    }
}
