package com.celehub.backend.course;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.course.dto.LessonDto;
import com.celehub.backend.security.AuthUser;

/** Lịch cá nhân: GET /api/lessons?from=2026-10-01&to=2026-10-31 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    public ApiResponse<List<LessonDto>> schedule(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(lessonService.schedule(me, from, to));
    }
}
