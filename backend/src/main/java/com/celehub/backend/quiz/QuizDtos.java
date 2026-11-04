package com.celehub.backend.quiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Toàn bộ request/response của Quiz. JSON camelCase. */
public final class QuizDtos {
    private QuizDtos() {}

    // ---------- request (giảng viên) ----------
    public record QuizOptionInput(String content, boolean correct) {}

    public record QuizQuestionInput(
            @NotBlank(message = "Nội dung câu hỏi không được để trống") String content,
            String explanation,
            @NotNull(message = "Câu hỏi phải có đáp án") List<@Valid QuizOptionInput> options) {}

    public record CreateQuizRequest(
            @NotBlank(message = "Tên đề không được để trống") @Size(max = 200, message = "Tên đề tối đa 200 ký tự") String title,
            @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
            @NotNull(message = "Thời gian làm bài không được để trống")
            @Min(value = 1, message = "Thời gian làm bài tối thiểu 1 phút")
            @Max(value = 600, message = "Thời gian làm bài tối đa 600 phút") Integer durationMinutes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<@Valid QuizQuestionInput> questions) {}

    /** Trường nào null thì giữ nguyên. */
    public record UpdateQuizRequest(
            @Size(max = 200, message = "Tên đề tối đa 200 ký tự") String title,
            @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
            @Min(value = 1, message = "Thời gian làm bài tối thiểu 1 phút")
            @Max(value = 600, message = "Thời gian làm bài tối đa 600 phút") Integer durationMinutes,
            LocalDateTime startTime,
            LocalDateTime endTime) {}

    public record ReplaceQuestionsRequest(@NotNull(message = "Thiếu danh sách câu hỏi") List<@Valid QuizQuestionInput> questions) {}

    // ---------- request (học viên) ----------
    /** optionId = null nghĩa là bỏ chọn. */
    public record AnswerRequest(@NotNull(message = "Thiếu questionId") Long questionId, Long optionId) {}

    // ---------- response chung ----------
    public record QuizDto(Long id, Long courseId, String courseName, String title, String description,
                          Integer durationMinutes, LocalDateTime startTime, LocalDateTime endTime,
                          boolean published, long questionCount, LocalDateTime createdAt) {}

    // ---------- giảng viên: xem trước (có đáp án đúng) ----------
    public record OptionDto(Long id, String content, boolean correct) {}

    public record QuestionDto(Long id, int orderIndex, String content, String explanation, List<OptionDto> options) {}

    public record QuizDetailDto(QuizDto quiz, List<QuestionDto> questions) {}

    // ---------- giảng viên: kết quả ----------
    /** status: NOT_STARTED | IN_PROGRESS | SUBMITTED */
    public record StudentResultDto(Long studentId, String fullName, String email, String status, BigDecimal score,
                                   int exitCount, boolean autoSubmitted, LocalDateTime startedAt,
                                   LocalDateTime submittedAt, Long durationSeconds) {}

    /** correctRate: % học viên đã nộp trả lời đúng câu này (bỏ trống tính là sai); null nếu chưa ai nộp. */
    public record QuestionStatDto(Long questionId, int orderIndex, String content, long answeredCount,
                                  long correctCount, Double correctRate) {}

    /** histogram: 10 phần tử, [0] = điểm [0,1), ..., [9] = điểm [9,10]. */
    public record QuizResultsDto(QuizDto quiz, int totalStudents, int submittedCount, int inProgressCount,
                                 BigDecimal avgScore, BigDecimal maxScore, BigDecimal minScore,
                                 List<Integer> histogram, List<StudentResultDto> students,
                                 List<QuestionStatDto> questionStats) {}

    // ---------- học viên: làm bài ----------
    public record PublicOptionDto(Long id, String content) {}

    public record StudentQuestionDto(Long id, int orderIndex, String content, List<PublicOptionDto> options) {}

    /**
     * Trả về từ POST /api/quizzes/{id}/start.
     * finished = false: đang làm → có questions, selected (questionId → optionId đã chọn), remainingSeconds, exitCount.
     * finished = true : bài đã được tự nộp (hết giờ / quá số lần thoát) → có score, correctCount, totalQuestions.
     */
    public record QuizSessionDto(boolean finished, boolean autoSubmitted, Long attemptId, Long quizId, String title,
                                 Integer durationMinutes, long remainingSeconds, int exitCount, int maxExits,
                                 LocalDateTime endsAt, List<StudentQuestionDto> questions, Map<Long, Long> selected,
                                 BigDecimal score, Integer correctCount, Integer totalQuestions) {}

    public record QuizResultDto(Long attemptId, Long quizId, BigDecimal score, int correctCount, int totalQuestions,
                                boolean autoSubmitted, LocalDateTime submittedAt) {}

    /** correct: true/false, null nếu bỏ trống. */
    public record ReviewQuestionDto(Long id, int orderIndex, String content, String explanation,
                                    List<OptionDto> options, Long selectedOptionId, Boolean correct) {}

    public record QuizReviewDto(QuizResultDto summary, String title, int exitCount, List<ReviewQuestionDto> questions) {}
}
