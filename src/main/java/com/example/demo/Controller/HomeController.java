package com.example.demo.Controller;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Repository.CourseRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {
    private final CourseRepo repo;
    private final CategoriesRepo categoryRepo;

    @Autowired
    public HomeController(CourseRepo repo, CategoriesRepo categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping("/home")
    public String showHomePage(Model model) {
        // Lấy danh sách các khóa học từ Moodle
        List<CoursesDto> moodleCourses = fetchCoursesFromMoodle();
        model.addAttribute("moodleCourses", moodleCourses);

        // Lấy danh sách các khóa học từ cơ sở dữ liệu
        List<Courses> courses = repo.findAll();
        model.addAttribute("courses", courses);

        // Lấy danh mục từ Moodle và thêm vào mô hình
        List<CategoryDto> moodleCategories = fetchCategoriesFromMoodle();
        model.addAttribute("moodleCategories", moodleCategories);

        return "home"; // Tên của trang HTML
    }

    private List<CoursesDto> fetchCoursesFromMoodle() {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Thay thế bằng token của bạn
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Thay thế bằng domain Moodle của bạn
        String functionName = "core_course_get_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<CoursesDto> courseList = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(response);
            if (root.isArray()) {
                for (JsonNode courseNode : root) {
                    int courseId = courseNode.path("id").asInt();
                    if (courseId != 1) { // Loại bỏ khóa học với ID = 1
                        CoursesDto course = new CoursesDto();
                        course.setId(courseId);
                        course.setFullname(courseNode.path("fullname").asText());
                        course.setShortname(courseNode.path("shortname").asText());
                        course.setDescription(courseNode.path("summary").asText());
                        int categoryId = courseNode.path("categoryid").asInt();
                        course.setCategory(categoryId);

                        // Lấy tên danh mục từ Moodle
                        String categoryName = fetchCategoryNameFromMoodle(categoryId);
                        course.setCategoryName(categoryName);

                        courseList.add(course);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return courseList;
    }

    private String fetchCategoryNameFromMoodle(int categoryId) {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Thay thế bằng token của bạn
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Thay thế bằng domain Moodle của bạn
        String functionName = "core_course_get_categories";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response);
            if (root.isArray()) {
                for (JsonNode categoryNode : root) {
                    int id = categoryNode.path("id").asInt();
                    if (id == categoryId) {
                        return categoryNode.path("name").asText();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Unknown Category";
    }

    private List<CategoryDto> fetchCategoriesFromMoodle() {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Thay thế bằng token của bạn
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Thay thế bằng domain Moodle của bạn
        String functionName = "core_course_get_categories";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<CategoryDto> categoryList = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(response);
            if (root.isArray()) {
                for (JsonNode categoryNode : root) {
                    CategoryDto category = new CategoryDto();
                    category.setId(categoryNode.path("id").asInt());
                    category.setName(categoryNode.path("name").asText());
                    category.setCategoryId(categoryNode.path("parent").asInt()); // Nếu bạn có sử dụng field này
                    categoryList.add(category);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return categoryList;
    }
}
