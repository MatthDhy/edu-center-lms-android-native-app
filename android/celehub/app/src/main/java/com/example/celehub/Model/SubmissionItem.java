package com.example.celehub.Model;

/** Bài nộp (khớp SubmissionDto của backend). */
public class SubmissionItem {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatarUrl;
    private String fileUrl;
    private String submittedAt;   // ISO
    private boolean late;
    private Double score;         // null nếu chưa chấm
    private String feedback;
    private String gradedAt;
    private String status;        // SUBMITTED | GRADED

    public Long getId() { return id; }
    public Long getAssignmentId() { return assignmentId; }
    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getFileUrl() { return fileUrl; }
    public String getSubmittedAt() { return submittedAt; }
    public boolean isLate() { return late; }
    public Double getScore() { return score; }
    public String getFeedback() { return feedback; }
    public String getGradedAt() { return gradedAt; }
    public String getStatus() { return status; }
}