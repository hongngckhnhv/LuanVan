package com.example.demo.Controller;

import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Categories;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Repository.CourseRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class CoursesController {
    private final CourseRepo repo;
    private final CategoriesRepo categoryRepo;

    @Autowired
    public CoursesController(CourseRepo repo, CategoriesRepo categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping("/layout_course")
    public String showCourseList(Model model) {
        List<Courses> courses = repo.findAll();
        model.addAttribute("courses", courses);

        List<Categories> categories = categoryRepo.findAll();
        model.addAttribute("categories", categories);

        return "layout_course";
    }

    @GetMapping("/create_course")
    public String showCreateCourse(Model model) {
        CoursesDto coursesDto = new CoursesDto();
        model.addAttribute("coursesDto", coursesDto);

        List<Categories> categories = categoryRepo.findAll();
        model.addAttribute("categories", categories);

        return "create_course";
    }

    @PostMapping("/create_course")
    public String showCreateCourses(@Valid @ModelAttribute CoursesDto coursesDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepo.findAll());
            return "create_course";
        }

        // Create the course on Moodle
        String moodleResponse = createMoodleCourse(coursesDto);

        if (moodleResponse != null && moodleResponse.contains("id")) {
            Integer moodleCourseId = extractMoodleCourseId(moodleResponse);

            Courses newCourse = new Courses();
            newCourse.setFullname(coursesDto.getFullname());
            newCourse.setShortname(coursesDto.getShortname());
            newCourse.setCategory(coursesDto.getCategory());
            newCourse.setDescription(coursesDto.getDescription());
            newCourse.setWebCourseId(moodleCourseId);
            repo.save(newCourse);

            return "redirect:/layout_course";
        } else {
            // If creating the course failed, return an error message
            model.addAttribute("errorMessage", "Failed to create the course on Moodle. Please try again.");
            model.addAttribute("categories", categoryRepo.findAll());
            return "create_course";
        }
    }

    private String createMoodleCourse(CoursesDto coursesDto) {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Replace with your token
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Replace with your Moodle domain

        Optional<Categories> categoryOpt = categoryRepo.findById(coursesDto.getCategory());
        if (!categoryOpt.isPresent()) {
            return null;
        }

        int moodleCategoryId = categoryOpt.get().getCategoryId();

        String functionName = "core_course_create_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courses[0][fullname]", coursesDto.getFullname());
        parameters.add("courses[0][shortname]", coursesDto.getShortname());
        parameters.add("courses[0][categoryid]", String.valueOf(moodleCategoryId));
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println(serverUrl);
        System.out.println(response);

        return response;
    }

    private Integer extractMoodleCourseId(String moodleResponse) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(moodleResponse);
            return root.path(0).path("id").asInt();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/delete_course")
    public String deleteCourse(@RequestParam("id") Integer courseId, Model model) {
        // Tìm khóa học trong cơ sở dữ liệu theo ID
        Optional<Courses> courseOpt = repo.findById(courseId);
        if (courseOpt.isPresent()) {
            Courses course = courseOpt.get();

            // Lấy ID khóa học trên Moodle
            Integer moodleCourseId = course.getWebCourseId();

            // Kiểm tra xem ID khóa học trên Moodle có tồn tại hay không
            if (moodleCourseId != null) {
                // Gọi hàm để xóa khóa học trên Moodle
                boolean moodleDeleted = deleteMoodleCourse(moodleCourseId);

                if (moodleDeleted) {
                    // Nếu xóa thành công trên Moodle, tiếp tục xóa khỏi cơ sở dữ liệu web
                    try {
                        repo.deleteById(courseId);
                        repo.flush(); // Đảm bảo rằng thay đổi được commit ngay lập tức
                        System.out.println("Course deleted from web database: " + course.getFullname());

                        // Cập nhật danh sách khóa học sau khi xóa thành công
                        List<Courses> updatedCourses = repo.findAll();
                        model.addAttribute("courses", updatedCourses);

                    } catch (Exception e) {
                        System.out.println("Failed to delete course from web database: " + course.getFullname());
                        e.printStackTrace(); // In ra chi tiết lỗi để dễ dàng debug
                        model.addAttribute("errorMessage", "Failed to delete the course from the web database. Please try again.");
                    }
                } else {
                    // Xử lý lỗi nếu việc xóa trên Moodle không thành công
                    System.out.println("Failed to delete course from Moodle: " + course.getFullname());
                    model.addAttribute("errorMessage", "Failed to delete the course on Moodle. Please try again.");
                }
            } else {
                System.out.println("Moodle Course ID is null for course: " + course.getFullname());
                model.addAttribute("errorMessage", "Moodle Course ID is missing. Cannot delete the course on Moodle.");
            }
        } else {
            System.out.println("Course not found in the database with ID: " + courseId);
            model.addAttribute("errorMessage", "Course not found in the database. Please try again.");
        }
        return "redirect:/layout_course"; // Trả về danh sách khóa học sau khi xóa
    }

    private boolean deleteMoodleCourse(Integer moodleCourseId) {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Thay thế bằng token của bạn
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Thay thế bằng domain Moodle của bạn

        String functionName = "core_course_delete_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courseids[0]", String.valueOf(moodleCourseId));
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println("Moodle Response: " + response);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Kiểm tra xem phản hồi có chứa cảnh báo không
            if (rootNode.has("warnings")) {
                JsonNode warnings = rootNode.get("warnings");
                if (warnings.size() == 0) {
                    // Không có cảnh báo nào, tức là thành công
                    return true;
                } else {
                    for (JsonNode warning : warnings) {
                        System.out.println("Warning Item: " + warning.get("item").asText());
                        System.out.println("Warning Item ID: " + warning.get("itemid").asInt());
                        System.out.println("Warning Code: " + warning.get("warningcode").asText());
                        System.out.println("Warning Message: " + warning.get("message").asText());
                    }
                    return false; // Có cảnh báo, coi như không thành công
                }
            } else {
                System.out.println("Unexpected response structure from Moodle: " + response);
                return false; // Phản hồi không như mong đợi
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @GetMapping("/edit_course")
    public String showEditCourse(@RequestParam("id") Integer courseId, Model model) {
        Optional<Courses> courseOpt = repo.findById(courseId);
        if (courseOpt.isPresent()) {
            Courses course = courseOpt.get();
            CoursesDto coursesDto = new CoursesDto();
            coursesDto.setId(course.getId());
            coursesDto.setFullname(course.getFullname());
            coursesDto.setShortname(course.getShortname());
            coursesDto.setCategory(course.getCategory()); // Đọc Integer trực tiếp
            coursesDto.setDescription(course.getDescription());

            model.addAttribute("coursesDto", coursesDto);

            List<Categories> categories = categoryRepo.findAll();
            model.addAttribute("categories", categories);

            return "edit_course";
        } else {
            model.addAttribute("errorMessage", "Course not found.");
            return "redirect:/layout_course";
        }
    }


    @PostMapping("/edit_course")
    public String editCourse(@Valid @ModelAttribute CoursesDto coursesDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepo.findAll());
            return "edit_course";
        }

        Optional<Courses> courseOpt = repo.findById(coursesDto.getId());
        if (courseOpt.isPresent()) {
            Courses course = courseOpt.get();
            course.setFullname(coursesDto.getFullname());
            course.setShortname(coursesDto.getShortname());
            course.setCategory(coursesDto.getCategory());
            course.setDescription(coursesDto.getDescription());
            repo.save(course);

            boolean moodleUpdated = updateMoodleCourse(coursesDto);
            if (moodleUpdated) {
                return "redirect:/layout_course";
            } else {
                model.addAttribute("errorMessage", "Failed to update the course on Moodle. Please try again.");
                model.addAttribute("categories", categoryRepo.findAll());
                return "edit_course";
            }
        } else {
            model.addAttribute("errorMessage", "Course not found.");
            return "redirect:/layout_course";
        }
    }

    private boolean updateMoodleCourse(CoursesDto coursesDto) {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Thay thế bằng token của bạn
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Thay thế bằng domain Moodle của bạn

        String functionName = "core_course_update_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courses[0][id]", String.valueOf(coursesDto.getWebCourseId())); // Đảm bảo ID đúng của Moodle
        parameters.add("courses[0][fullname]", coursesDto.getFullname());
        parameters.add("courses[0][shortname]", coursesDto.getShortname());
        parameters.add("courses[0][categoryid]", String.valueOf(coursesDto.getCategory()));
        parameters.add("courses[0][summary]", coursesDto.getDescription());
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println("Moodle Response: " + response);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            // Kiểm tra phản hồi từ Moodle để xác định xem việc cập nhật có thành công hay không
            if (rootNode.has("warnings")) {
                for (JsonNode warning : rootNode.get("warnings")) {
                    System.out.println("Warning: " + warning.get("message").asText());
                }
                return false;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


}


