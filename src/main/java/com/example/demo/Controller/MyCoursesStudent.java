package com.example.demo.Controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MyCoursesStudent {
    // Hiển thị danh sách khóa học
    @GetMapping("/my-courses-student")
    public String showMyCoursesStudent(Model model) {
        // Thêm danh sách các khóa học vào model để hiển thị trên frontend
        // model.addAttribute("courses", courseService.getAllCourses()); // Sau này kết nối với database

        return "my-courses"; // Hiển thị trang my-courses.html
    }

    // Hiển thị chi tiết khóa học dựa vào courseId
    @GetMapping("/course-details/{courseId}")
    public String courseOverview(@PathVariable("courseId") Long courseId, Model model) {
        // Lấy thông tin chi tiết của khóa học từ courseId (sau này kết nối với database)
        // Course course = courseService.getCourseById(courseId);
        // model.addAttribute("course", course);

        // Hiện tại giả lập dữ liệu khóa học cho frontend
        model.addAttribute("courseId", courseId);
        model.addAttribute("courseName", "Khóa học " + courseId);
        model.addAttribute("teacher", "GV A");
        model.addAttribute("semester", "HK1");
        model.addAttribute("year", "2024");

        return "course-details"; // Trả về trang course-details.html
    }
}
