package com.celehub.backend.course;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.course.dto.CourseDto;
import com.celehub.backend.course.dto.EnrolledStudentDto;
import com.celehub.backend.course.dto.LessonDto;
import com.celehub.backend.security.AuthUser;

/** Tab "Khóa học" của STUDENT và TEACHER. */
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    private final LessonService lessonService;
    private final EnrollmentService enrollmentService;

    public CourseController(CourseService courseService, LessonService lessonService,
                            EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.lessonService = lessonService;
        this.enrollmentService = enrollmentService;
    }

    /** HV: lớp đang học · GV: lớp đang phụ trách. */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER')")
    public ApiResponse<List<CourseDto>> mine(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(courseService.mine(me));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<CourseDto> detail(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(courseService.detail(me, id));
    }

    @GetMapping("/{id}/lessons")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<List<LessonDto>> lessons(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(lessonService.listByCourse(me, id));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<EnrolledStudentDto>> students(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(enrollmentService.roster(me, id));
    }
}
