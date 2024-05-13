package com.example.demo.Model;

import java.util.List;

public class AttemptInfo {
    private Integer attemptId;
    private Double score;
    private List<QuestionDetail> questionDetails;

    public AttemptInfo() {
        // Constructor mặc định
    }

    public AttemptInfo(Integer attemptId, Double score) {
        this.attemptId = attemptId;
        this.score = score;
    }

    // Getter và setter cho các trường

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public List<QuestionDetail> getQuestionDetails() {
        return questionDetails;
    }

    public void setQuestionDetails(List<QuestionDetail> questionDetails) {
        this.questionDetails = questionDetails;
    }
}
