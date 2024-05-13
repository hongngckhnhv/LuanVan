
package com.example.demo.Model;

public class QuestionDetail {
    private int questionNumber;
    private String questionText;
    private String studentResponse;
    private String correctResponse;

    private String html; // Thêm trường html vào lớp QuestionDetail
    // Thêm các trường khác tùy theo thông tin bạn muốn hiển thị

    public QuestionDetail() {
        // Constructor mặc định
    }

    public QuestionDetail(String questionText, String studentResponse, String correctResponse) {
        this.questionText = questionText;
        this.studentResponse = studentResponse;
        this.correctResponse = correctResponse;
        // Khởi tạo các trường khác tùy theo thông tin bạn muốn hiển thị
        this.html = html; // Khởi tạo trường html
    }

    // Getter và setter cho các trường

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getStudentResponse() {
        return studentResponse;
    }

    public void setStudentResponse(String studentResponse) {
        this.studentResponse = studentResponse;
    }

    public String getCorrectResponse() {
        return correctResponse;
    }

    public void setCorrectResponse(String correctResponse) {
        this.correctResponse = correctResponse;
    }

    // Thêm getter và setter cho các trường khác nếu cần

    // Getter và setter cho trường html
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }
}

