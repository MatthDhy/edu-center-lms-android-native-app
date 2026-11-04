package com.example.celehub.Model;

import java.util.List;

/** GET /api/assignments/{id}/submissions. Danh sách học viên chưa nộp (notSubmitted) chưa dùng nên bỏ qua. */
public class SubmissionsOverview {
    private AssignmentItem assignment;
    private List<SubmissionItem> submitted;

    public AssignmentItem getAssignment() { return assignment; }
    public List<SubmissionItem> getSubmitted() { return submitted; }
}