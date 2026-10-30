package com.celehub.backend.course;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.course.dto.CourseDto;
import com.celehub.backend.course.dto.CreateCourseRequest;
import com.celehub.backend.course.dto.CreateLessonRequest;
import com.celehub.backend.course.dto.EnrollRequest;
import com.celehub.backend.course.dto.EnrolledStudentDto;
import com.celehub.backend.course.dto.GenerateLessonsRequest;
import com.celehub.backend.course.dto.LessonDto;
import com.celehub.backend.course.dto.UpdateCourseRequest;
import com.celehub.backend.course.dto.UpdateLessonRequest;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

/** /api/admin/** chỉ ROLE_ADMIN (cấu hình ở SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminCourseController {
    private final CourseService courseService;
    private final LessonService lessonService;
    private final EnrollmentService enrollmentService;

    public AdminCourseController(CourseService courseService, LessonService lessonService,
                                 EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.lessonService = lessonService;
        this.enrollmentService = enrollmentService;
    }

    // ---- Lớp học ----
    @GetMapping("/courses")
    public ApiResponse<List<CourseDto>> list(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long teacherId,
                                             @RequestParam(required = false) String search) {
        return ApiResponse.success(courseService.adminList(status, teacherId, search));
    }

    @PostMapping("/courses")
    public ApiResponse<CourseDto> create(@Valid @RequestBody CreateCourseRequest req) {
        return ApiResponse.success("Đã tạo lớp học", courseService.create(req));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<CourseDto> get(@PathVariable Long id) {
        return ApiResponse.success(courseService.adminGet(id));
    }

    @PutMapping("/courses/{id}")
    public ApiResponse<CourseDto> update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest req) {
        return ApiResponse.success("Đã cập nhật lớp học", courseService.update(id, req));
    }

    // ---- Buổi học ----
    @GetMapping("/courses/{id}/lessons")
    public ApiResponse<List<LessonDto>> lessons(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(lessonService.listByCourse(me, id));
    }

    @PostMapping("/courses/{id}/lessons")
    public ApiResponse<LessonDto> addLesson(@PathVariable Long id, @Valid @RequestBody CreateLessonRequest req) {
        return ApiResponse.success("Đã thêm buổi học", lessonService.create(id, req));
    }

    @PostMapping("/courses/{id}/lessons/generate")
    public ApiResponse<List<LessonDto>> generate(@PathVariable Long id, @Valid @RequestBody GenerateLessonsRequest req) {
        List<LessonDto> created = lessonService.generate(id, req);
        return ApiResponse.success("Đã tạo " + created.size() + " buổi học", created);
    }

    /** Thời khóa biểu toàn trung tâm: GET /api/admin/lessons?from=2026-11-02&to=2026-11-08 */
    @GetMapping("/lessons")
    public ApiResponse<List<LessonDto>> schedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(lessonService.adminSchedule(from, to));
    }

    @PutMapping("/lessons/{lessonId}")
    public ApiResponse<LessonDto> updateLesson(@PathVariable Long lessonId, @Valid @RequestBody UpdateLessonRequest req) {
        return ApiResponse.success("Đã cập nhật buổi học", lessonService.update(lessonId, req));
    }

    /** Hủy buổi (status = CANCELED), không xóa cứng. */
    @DeleteMapping("/lessons/{lessonId}")
    public ApiResponse<Void> cancelLesson(@PathVariable Long lessonId) {
        lessonService.cancel(lessonId);
        return ApiResponse.success("Đã hủy buổi học", null);
    }

    // ---- Xếp học viên ----
    @GetMapping("/courses/{id}/enrollments")
    public ApiResponse<List<EnrolledStudentDto>> roster(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(enrollmentService.roster(me, id));
    }

    @PostMapping("/courses/{id}/enrollments")
    public ApiResponse<Map<String, Integer>> enroll(@PathVariable Long id, @Valid @RequestBody EnrollRequest req) {
        return ApiResponse.success("Đã xếp học viên vào lớp", enrollmentService.enroll(id, req.studentIds()));
    }

    @DeleteMapping("/courses/{id}/enrollments/{studentId}")
    public ApiResponse<Void> remove(@PathVariable Long id, @PathVariable Long studentId) {
        enrollmentService.remove(id, studentId);
        return ApiResponse.success("Đã rút học viên khỏi lớp", null);
    }
}
