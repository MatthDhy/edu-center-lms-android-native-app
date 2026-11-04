package com.celehub.backend.quiz;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.quiz.QuizDtos.*;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // ---------------- Giảng viên ----------------

    @GetMapping("/quizzes/template")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quiz_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(quizService.template());
    }

    /** Tạo tay (JSON). Xuất bản sau bằng POST /quizzes/{id}/publish. */
    @PostMapping("/courses/{courseId}/quizzes")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<QuizDto> create(@AuthenticationPrincipal AuthUser me, @PathVariable Long courseId,
                                       @Valid @RequestBody CreateQuizRequest req) {
        return ApiResponse.success("Đã lưu quiz (bản nháp)", quizService.create(me, courseId, req));
    }

    /** multipart: title, description?, durationMinutes, startTime?, endTime?, file (.xlsx) */
    @PostMapping(value = "/courses/{courseId}/quizzes/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<QuizDto> importExcel(
            @AuthenticationPrincipal AuthUser me, @PathVariable Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Đã nhập quiz từ file (bản nháp)",
                quizService.importExcel(me, courseId, title, description, durationMinutes, startTime, endTime, file));
    }

    @GetMapping("/courses/{courseId}/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<QuizDto>> listByCourse(@AuthenticationPrincipal AuthUser me, @PathVariable Long courseId) {
        return ApiResponse.success(quizService.listByCourse(me, courseId));
    }

    /** Xem trước (có đáp án đúng, render LaTeX phía app). */
    @GetMapping("/quizzes/{id}/preview")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<QuizDetailDto> preview(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(quizService.preview(me, id));
    }

    @PutMapping("/quizzes/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<QuizDto> update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                       @Valid @RequestBody UpdateQuizRequest req) {
        return ApiResponse.success("Đã cập nhật quiz", quizService.update(me, id, req));
    }

    @PutMapping("/quizzes/{id}/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<QuizDetailDto> replaceQuestions(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                       @Valid @RequestBody ReplaceQuestionsRequest req) {
        return ApiResponse.success("Đã cập nhật câu hỏi", quizService.replaceQuestions(me, id, req.questions()));
    }

    @PostMapping("/quizzes/{id}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<QuizDto> publish(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success("Đã xuất bản quiz", quizService.publish(me, id));
    }

    @DeleteMapping("/quizzes/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        quizService.delete(me, id);
        return ApiResponse.success("Đã xóa quiz", null);
    }

    /** Màn "Kết quả quiz" của giảng viên: tóm tắt, phổ điểm, từng học viên, thống kê từng câu. */
    @GetMapping("/quizzes/{id}/results")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<QuizResultsDto> results(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(quizService.results(me, id));
    }

    // ---------------- Học viên ----------------

    @GetMapping("/quizzes/{id}/info")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<QuizDto> info(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(quizService.info(me, id));
    }

    @PostMapping("/quizzes/{id}/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<QuizSessionDto> start(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(quizService.start(me, id));
    }

    @PutMapping("/quiz-attempts/{id}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> saveAnswer(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                        @Valid @RequestBody AnswerRequest req) {
        quizService.saveAnswer(me, id, req);
        return ApiResponse.success("Đã lưu", null);
    }

    @PostMapping("/quiz-attempts/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<QuizResultDto> submit(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success("Đã nộp bài", quizService.submit(me, id));
    }

    @GetMapping("/quiz-attempts/{id}/review")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<QuizReviewDto> review(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(quizService.review(me, id));
    }
}
