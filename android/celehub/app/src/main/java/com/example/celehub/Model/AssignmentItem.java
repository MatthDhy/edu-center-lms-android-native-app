package com.example.celehub.Model;

/**
 * Bài tập (khớp AssignmentDto của backend).
 * Học viên: status = PENDING | OVERDUE | SUBMITTED | GRADED, mySubmission có giá trị nếu đã nộp.
 * Giảng viên: status = null, ba số đếm có giá trị.
 */
public class AssignmentItem {
    private Long id;
    private String type;
    private Long courseId;
    private String courseName;
    private String title;
    private String description;
    private String attachmentUrl;
    private String deadline;      // ISO
    private Double maxScore;
    private String createdAt;
    private Long minutesLeft;     // âm nghĩa là đã quá hạn
    private String status;
    private SubmissionItem mySubmission;
    private Long submittedCount;
    private Long gradedCount;
    private Long totalStudents;

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getDeadline() { return deadline; }
    public String getStatus() { return status; }
    public SubmissionItem getMySubmission() { return mySubmission; }
    public Long getSubmittedCount() { return submittedCount; }
    public Long getGradedCount() { return gradedCount; }
    public Long getTotalStudents() { return totalStudents; }

    /** Điểm tối đa, mặc định 10 nếu bài không đặt. */
    public double getMaxScoreOrDefault() { return maxScore != null ? maxScore : 10.0; }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isOverdue() { return "OVERDUE".equals(status); }
    public boolean isSubmitted() { return "SUBMITTED".equals(status); }
    public boolean isGraded() { return "GRADED".equals(status); }
    public Long getMinutesLeft() { return minutesLeft; }

    /** Đã hết hạn nộp (backend trả minutesLeft âm). */
    public boolean isClosed() { return minutesLeft != null && minutesLeft < 0; }
    private String teacherName;
    public String getTeacherName() { return teacherName; }
}