package com.celehub.backend.quiz;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private String content;

    private String explanation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() {}

    public Question(Quiz quiz, String content, String explanation, int orderIndex) {
        this.quiz = quiz;
        this.content = content;
        this.explanation = explanation;
        this.orderIndex = orderIndex;
    }

    public void addOption(QuestionOption o) { options.add(o); }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getContent() { return content; }
    public String getExplanation() { return explanation; }
    public int getOrderIndex() { return orderIndex; }
    public List<QuestionOption> getOptions() { return options; }
}
