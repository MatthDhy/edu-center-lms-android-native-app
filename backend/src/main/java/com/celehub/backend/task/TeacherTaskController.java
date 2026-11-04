package com.celehub.backend.task;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;

/** Tab "Bài tập" của giảng viên: danh sách gộp bài tập + quiz. */
@RestController
@RequestMapping("/api/teacher")
public class TeacherTaskController {
    private final TeacherTaskService service;

    public TeacherTaskController(TeacherTaskService service) {
        this.service = service;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<TeacherTaskDto>> tasks(@AuthenticationPrincipal AuthUser me,
                                                   @RequestParam(required = false) Long courseId) {
        return ApiResponse.success(service.list(me, courseId));
    }
}
