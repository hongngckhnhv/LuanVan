package com.example.demo.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Courses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "moodle_course_id") // Đặt tên cho cột lưu trữ ID của khóa học trên Moodle
    private Integer moodleCourseId; // ID của khóa học trên Moodle

    private String fullname;
    private String shortname;
    private Integer category;
    private String description;
    private String categoryName;

    // Getters và setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMoodleCourseId() {
        return moodleCourseId;
    }

    public void setMoodleCourseId(Integer moodleCourseId) {
        this.moodleCourseId = moodleCourseId;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
