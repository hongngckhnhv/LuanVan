package com.example.demo.Controller;

import com.example.demo.Model.Categories;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Repository.CourseRepo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MyCoursesStudentController {
    private final CourseRepo repo;
    private final CategoriesRepo categoryRepo;

    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    public MyCoursesStudentController(CourseRepo repo, CategoriesRepo categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping("/my-courses-student")
    public String showCourseSearchForm(Model model) {
        // Lấy thông tin người dùng hiện tại từ Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;

        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                username = userDetails.getUsername(); // Lấy username của người dùng đã đăng nhập
            } else {
                username = authentication.getPrincipal().toString();
            }
        }
        // Đảm bảo rằng username không null
        if (username == null) {
            model.addAttribute("error", "Không thể xác định được người dùng hiện tại.");
            return "my-courses"; // Trả về view form chứa thông báo lỗi
        }

        // Đặt username vào model
        model.addAttribute("username", username);

        // Gọi phương thức để lấy khóa học của người dùng
        List<String> userCourses = getUserCourses(username); // Lấy danh sách khóa học
        model.addAttribute("userCourses", userCourses);

      //  System.out.println("Username: " + username);
        System.out.println("User Courses: " + userCourses);

        return "my-courses"; // Trả về view chứa danh sách khóa học
    }

    @PostMapping("/my-courses-student")
    public String processCourseSearch(@RequestParam("username") String username,
                                      @RequestParam(value = "searchQuery", required = false) String searchQuery,
                                      @RequestParam(value = "sort", required = false) String sort,
                                      Model model) {
        if (username == null) {
            model.addAttribute("error", "Không thể xác định được người dùng hiện tại.");
            return "my-courses";
        }

        // Lấy danh sách khóa học cho người dùng
        List<String> userCourses = getUserCourses(username);

        // Nếu có yêu cầu tìm kiếm, lọc danh sách theo tên khóa học
        if (searchQuery != null && !searchQuery.isEmpty()) {
            userCourses = filterCoursesByName(userCourses, searchQuery);
        }

        // Nếu có yêu cầu sắp xếp, sắp xếp danh sách theo tên khóa học
        if ("name".equals(sort)) {
            userCourses.sort(String::compareToIgnoreCase);
        }

        // Cập nhật model
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("userCourses", userCourses);

        System.out.println("All User Courses: " + userCourses);
        System.out.println("Search Query: " + searchQuery);
        System.out.println("Filtered Courses: " + userCourses);

        return "my-courses";
    }


    private List<String> getUserCourses(String username) {
        List<String> userCourses = new ArrayList<>(); // Danh sách lưu trữ các khóa học của người dùng
        String getStudentsFunction = "core_user_get_users";
        String getUserCoursesFunction = "core_enrol_get_users_courses";

        System.out.println("Username: " + username);

        // Gửi yêu cầu API để lấy thông tin sinh viên từ Moodle dựa trên username
        String getStudentsUrl = domainName + "/webservice/rest/server.php" +
                "?wstoken=" + token +
                "&wsfunction=" + getStudentsFunction +
                "&moodlewsrestformat=json" +
                "&criteria[0][key]=username" +
                "&criteria[0][value]=" + username;

        // Gửi yêu cầu GET đến API Moodle
        String studentsResponse = restTemplate.getForObject(getStudentsUrl, String.class);

        // In ra phản hồi từ Moodle API để kiểm tra
        System.out.println("Response from Moodle API: " + studentsResponse);

        int userId = 0;

        // Trích xuất thông tin ID của người dùng từ kết quả
        try {
            JSONObject jsonObject = new JSONObject(studentsResponse);

            // Kiểm tra xem JSON có chứa trường 'users' hay không
            if (jsonObject.has("users")) {
                JSONArray users = jsonObject.getJSONArray("users");
                if (users.length() > 0) {
                    JSONObject user = users.getJSONObject(0); // Lấy thông tin user đầu tiên tìm thấy
                    userId = user.getInt("id");
                } else {
                    System.out.println("Không tìm thấy người dùng với username: " + username);
                }
            } else {
                System.out.println("Không tìm thấy trường 'users' trong phản hồi.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Đảm bảo rằng userId hợp lệ
        if (userId == 0) {
            return userCourses; // Trả về danh sách rỗng nếu không xác định được userId
        }

        // Gửi yêu cầu API để lấy danh sách khóa học của người dùng
        String getUserCoursesUrl = domainName + "/webservice/rest/server.php" +
                "?wstoken=" + token +
                "&wsfunction=" + getUserCoursesFunction +
                "&moodlewsrestformat=json" +
                "&userid=" + userId;

        System.out.println("UserID: " + userId);
        String coursesResponse = restTemplate.getForObject(getUserCoursesUrl, String.class);

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

        return userCourses; // Trả về danh sách khóa học
    }


    // Phương thức lọc khóa học theo tên
    private List<String> filterCoursesByName(List<String> userCourses, String searchQuery) {
        List<String> filteredCourses = new ArrayList<>();
        for (String course : userCourses) {
            System.out.println("Checking course: " + course); // Log từng khóa học
            if (course.toLowerCase().contains(searchQuery.toLowerCase())) {
                filteredCourses.add(course);
            }
        }
        return filteredCourses;
    }


}
