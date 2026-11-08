package com.example.celehub.Model;

/** Body của PUT /api/assignments/{id}. Trường null sẽ không được gửi và backend giữ nguyên. */
public class UpdateAssignmentBody {
    private final String title;
    private final String description;
    private final String deadline;     // yyyy-MM-dd'T'HH:mm:ss, null nếu không đổi hạn
    private final Double maxScore;

    public UpdateAssignmentBody(String title, String description, String deadline, Double maxScore) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.maxScore = maxScore;
    }
}