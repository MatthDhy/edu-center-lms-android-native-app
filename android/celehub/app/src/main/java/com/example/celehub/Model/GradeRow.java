package com.example.celehub.Model;

/** Một bài nộp kèm thông tin bài tập, dùng cho danh sách chấm bài. */
public class GradeRow {
    private final SubmissionItem submission;
    private final String assignmentTitle;
    private final String courseName;
    private final double maxScore;

    public GradeRow(SubmissionItem submission, String assignmentTitle, String courseName, double maxScore) {
        this.submission = submission;
        this.assignmentTitle = assignmentTitle;
        this.courseName = courseName;
        this.maxScore = maxScore;
    }

    public SubmissionItem getSubmission() { return submission; }
    public String getAssignmentTitle() { return assignmentTitle; }
    public String getCourseName() { return courseName; }
    public double getMaxScore() { return maxScore; }

    public boolean isGraded() { return submission.getScore() != null; }
}