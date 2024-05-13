package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Controller
public class StudentCourseController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/students-and-courses")
    public String showSearchForm() {
        return "students-and-courses"; // Trả về trang HTML hiển thị form tìm kiếm
    }

    @PostMapping("/students-and-courses")
    public String searchStudent(@RequestParam String firstname, Model model) {
        // Thay thế bằng token của Moodle của bạn
        String token = "3cc2d13c0b1ee13c59cb3757239b30e5";

        // Thay thế bằng domain của Moodle của bạn
        String domainName = "http://localhost/demo.ngockhanh.vn";

        // Tên của hàm API trong Moodle để lấy danh sách sinh viên
        String getStudentsFunction = "core_user_get_users";

        // Tên của hàm API trong Moodle để lấy danh sách khóa học của sinh viên
        String getUserCoursesFunction = "core_enrol_get_users_courses";

        // Gửi yêu cầu API để lấy danh sách sinh viên từ Moodle dựa trên firstname
        String getStudentsUrl = domainName + "/webservice/rest/server.php" +
                "?wstoken=" + token +
                "&wsfunction=" + getStudentsFunction +
                "&moodlewsrestformat=json" +
                "&criteria[0][key]=firstname" +
                "&criteria[0][value]=" + firstname;

        // Gửi yêu cầu GET đến API Moodle
        String studentsResponse = restTemplate.getForObject(getStudentsUrl, String.class);

        // Trích xuất thông tin fullname và email từ kết quả và đặt vào model
        List<Integer> userIds = new ArrayList<>(); // Danh sách lưu trữ ID của sinh viên
        List<String> fullNames = new ArrayList<>(); // Danh sách lưu trữ full name
        List<String> emails = new ArrayList<>(); // Danh sách lưu trữ email

        // Parse kết quả JSON và trích xuất thông tin full name và email
        try {
            JSONObject jsonObject = new JSONObject(studentsResponse);
            JSONArray users = jsonObject.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                userIds.add(user.getInt("id"));
                fullNames.add(user.getString("fullname"));
                emails.add(user.getString("email"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Lấy danh sách các khóa học của từng sinh viên và đặt vào model
        List<List<String>> courseIds = new ArrayList<>(); // Danh sách lưu trữ các khóa học của từng sinh viên

        for (Integer userId : userIds) {
            String getUserCoursesUrl = domainName + "/webservice/rest/server.php" +
                    "?wstoken=" + token +
                    "&wsfunction=" + getUserCoursesFunction +
                    "&moodlewsrestformat=json" +
                    "&userid=" + userId;

            String coursesResponse = restTemplate.getForObject(getUserCoursesUrl, String.class);

            List<String> userCourses = new ArrayList<>(); // Danh sách lưu trữ các khóa học của một sinh viên

            try {
                JSONArray coursesArray = new JSONArray(coursesResponse);
                for (int i = 0; i < coursesArray.length(); i++) {
                    JSONObject course = coursesArray.getJSONObject(i);
                    int courseId = course.getInt("id");
                    String courseName = course.getString("fullname");
                    String courseInfo = courseId + " - " + courseName; // Kết hợp ID và tên của khóa học
                    userCourses.add(courseInfo);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            courseIds.add(userCourses);
        }

        // Đặt thông tin vào model để hiển thị trên trang HTML
        model.addAttribute("userIds", userIds);
        model.addAttribute("fullNames", fullNames);
        model.addAttribute("emails", emails);
        model.addAttribute("courseIds", courseIds);

        System.out.println(courseIds);

        // Trả về tên view để hiển thị kết quả
        return "students-and-courses";

    }



}