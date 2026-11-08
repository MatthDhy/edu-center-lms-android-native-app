package com.example.celehub.Model;

import java.util.List;

/** GET /api/assignments/{id}/submissions: thông tin bài, danh sách đã nộp và chưa nộp. */
public class SubmissionsOverview {
    private AssignmentItem assignment;
    private List<SubmissionItem> submitted;
    private List<StudentBrief> notSubmitted;

    public AssignmentItem getAssignment() { return assignment; }
    public List<SubmissionItem> getSubmitted() { return submitted; }
    public List<StudentBrief> getNotSubmitted() { return notSubmitted; }
}