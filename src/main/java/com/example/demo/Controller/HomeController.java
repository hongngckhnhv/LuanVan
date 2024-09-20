package com.example.demo.Controller;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Repository.CourseRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final CourseRepo repo;
    private final CategoriesRepo categoryRepo;

    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

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
        String functionName = "core_course_get_courses";
        String response = sendMoodleRequest(functionName, new LinkedMultiValueMap<>());

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
                        String categoryName = getCategoryNameFromMoodle(categoryId);
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

    private String getCategoryNameFromMoodle(int categoryId) {
        List<CategoryDto> categories = fetchCategoriesFromMoodle();
        return categories.stream()
                .filter(category -> category.getId() == categoryId)
                .findFirst()
                .map(CategoryDto::getName)
                .orElse("Unknown Category");
    }

    private List<CategoryDto> fetchCategoriesFromMoodle() {
        String functionName = "core_course_get_categories";
        String response = sendMoodleRequest(functionName, new LinkedMultiValueMap<>());

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

    private String sendMoodleRequest(String functionName, MultiValueMap<String, String> parameters) {
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(serverUrl, request, String.class);
    }
}
