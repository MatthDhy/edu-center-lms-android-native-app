package com.example.celehub.Model;

/** PATCH /api/submissions/{id}/grade */
public class GradeBody {
    private final double score;
    private final String feedback;

    public GradeBody(double score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }
}