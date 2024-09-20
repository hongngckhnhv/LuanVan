package com.example.demo.Controller;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Categories;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Repository.CourseRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CoursesController {
    private final CourseRepo repo;
    private final CategoriesRepo categoryRepo;

    @Autowired
    public CoursesController(CourseRepo repo, CategoriesRepo categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

    @GetMapping("/layout_course")
    public String showCourseList(Model model) {
        // Lấy dữ liệu khóa học từ Moodle
        List<CoursesDto> moodleCourses = fetchCoursesFromMoodle();

        // Đồng bộ khóa học từ Moodle
        synchronizeCourses(moodleCourses);

        // Lấy dữ liệu khóa học từ cơ sở dữ liệu
        List<Courses> courses = repo.findAll();
        model.addAttribute("courses", courses);
        model.addAttribute("moodleCourses", moodleCourses);

        // Lấy danh mục từ Moodle và thêm vào mô hình
        List<CategoryDto> moodleCategories = fetchCategoriesFromMoodle();
        model.addAttribute("moodleCategories", moodleCategories);

        return "layout_course";
    }

    private void synchronizeCourses(List<CoursesDto> moodleCourses) {
        System.out.println("Starting course synchronization...");

        // Lấy danh sách khóa học hiện có từ cơ sở dữ liệu
        List<Courses> existingCourses = repo.findAll();
        Set<Integer> existingCourseIds = existingCourses.stream()
                .map(Courses::getWebCourseId)
                .collect(Collectors.toSet());

        // Lưu trữ ID khóa học từ Moodle
        Set<Integer> moodleCourseIds = moodleCourses.stream()
                .map(CoursesDto::getId)
                .collect(Collectors.toSet());

        for (CoursesDto moodleCourse : moodleCourses) {
            System.out.println("Processing Moodle course: " + moodleCourse.getFullname());

            if (!existingCourseIds.contains(moodleCourse.getId())) {
                // Nếu khóa học không tồn tại, thêm vào cơ sở dữ liệu
                Courses newCourse = new Courses();
                newCourse.setFullname(moodleCourse.getFullname());
                newCourse.setShortname(moodleCourse.getShortname());
                newCourse.setDescription(moodleCourse.getDescription());
                newCourse.setCategory(moodleCourse.getCategory());
                newCourse.setWebCourseId(moodleCourse.getId());
                newCourse.setCategoryName(moodleCourse.getCategoryName());

                repo.save(newCourse);
                System.out.println("Added new course: " + newCourse.getFullname());
            } else {
                // Nếu khóa học đã tồn tại, kiểm tra và cập nhật thông tin nếu cần
                Courses existingCourse = existingCourses.stream()
                        .filter(course -> course.getWebCourseId().equals(moodleCourse.getId()))
                        .findFirst()
                        .orElse(null);

                if (existingCourse != null) {
                    // Cập nhật thông tin khóa học nếu có thay đổi
                    existingCourse.setFullname(moodleCourse.getFullname());
                    existingCourse.setShortname(moodleCourse.getShortname());
                    existingCourse.setDescription(moodleCourse.getDescription());
                    existingCourse.setCategory(moodleCourse.getCategory());
                    existingCourse.setCategoryName(moodleCourse.getCategoryName());

                    repo.save(existingCourse);
                    System.out.println("Updated course: " + existingCourse.getFullname());
                }
            }
        }

        // Xóa các khóa học không còn tồn tại trên Moodle
        for (Courses existingCourse : existingCourses) {
            if (!moodleCourseIds.contains(existingCourse.getWebCourseId())) {
                repo.delete(existingCourse);
                System.out.println("Deleted course: " + existingCourse.getFullname());
            }
        }

        System.out.println("Course synchronization completed.");

        // Kiểm tra các khóa học hiện có trong cơ sở dữ liệu sau khi đồng bộ
        List<Courses> updatedCourses = repo.findAll();
        System.out.println("Courses in database after synchronization: " + updatedCourses);
    }

    private List<CoursesDto> fetchCoursesFromMoodle() {
        String functionName = "core_course_get_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println(serverUrl);
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

    @GetMapping("/create_course")
    public String showCreateCourse(Model model) {
        CoursesDto coursesDto = new CoursesDto();
        model.addAttribute("coursesDto", coursesDto);

        // Lấy danh sách category từ Moodle
        List<CategoryDto> categoriesFromMoodle = fetchCategoriesFromMoodle();
        model.addAttribute("categories", categoriesFromMoodle);

        return "create_course";
    }


    @PostMapping("/create_course")
    public String showCreateCourses(@Valid @ModelAttribute CoursesDto coursesDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepo.findAll());
            return "create_course";
        }

        // Tạo khóa học trên Moodle
        String moodleResponse = createMoodleCourse(coursesDto);

        if (moodleResponse != null && moodleResponse.contains("id")) {
            Integer moodleCourseId = extractMoodleCourseId(moodleResponse);

            // Lấy categoryName từ cơ sở dữ liệu
            Optional<Categories> categoryOpt = categoryRepo.findById(coursesDto.getCategory());
            String categoryName = categoryOpt.isPresent() ? categoryOpt.get().getName() : "";

            Courses newCourse = new Courses();
            newCourse.setFullname(coursesDto.getFullname());
            newCourse.setShortname(coursesDto.getShortname());
            newCourse.setCategory(coursesDto.getCategory());
            newCourse.setDescription(coursesDto.getDescription());
            newCourse.setWebCourseId(moodleCourseId);
            newCourse.setCategoryName(categoryName); // Thiết lập categoryName

            repo.save(newCourse);

            return "redirect:/layout_course";
        } else {
            // Nếu tạo khóa học thất bại, trả về thông báo lỗi
            model.addAttribute("errorMessage", "Failed to create the course on Moodle. Please try again.");
            model.addAttribute("categories", categoryRepo.findAll());
            return "create_course";
        }
    }


    private String createMoodleCourse(CoursesDto coursesDto) {
        // Kiểm tra nếu categoryId được chọn từ Moodle thì sử dụng trực tiếp categoryId đó
        Integer moodleCategoryId = coursesDto.getCategory(); // Category lấy từ Moodle có thể truyền trực tiếp

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
            course.setCategory(coursesDto.getCategory()); // Sử dụng Integer trực tiếp
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

        String functionName = "core_course_update_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courses[0][id]", String.valueOf(coursesDto.getId())); // Chuyển đổi Integer thành String
        parameters.add("courses[0][fullname]", coursesDto.getFullname());
        parameters.add("courses[0][shortname]", coursesDto.getShortname());
        parameters.add("courses[0][categoryid]", String.valueOf(coursesDto.getCategory())); // Chuyển đổi Integer thành String nếu cần
        parameters.add("courses[0][summary]", coursesDto.getDescription());
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println("Moodle Response: " + response);
        System.out.println(serverUrl);

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

    @PostMapping("/delete_moodle_course")
    public String deleteMoodleCourse(@RequestParam("id") int courseId, RedirectAttributes redirectAttributes) {
        System.out.println("Course ID: " + courseId); // Kiểm tra courseId

        boolean moodleSuccess = deleteCourseFromMoodle(courseId);
        if (moodleSuccess) {
            boolean dbSuccess = deleteCourseFromDatabase(courseId);
            if (dbSuccess) {
                redirectAttributes.addFlashAttribute("message", "Course deleted successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to delete course from the database.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete course from Moodle.");
        }
        return "redirect:/layout_course";
    }


    private boolean deleteCourseFromDatabase(int courseId) {
        try {
            if (repo.existsById(courseId)) {
                repo.deleteById(courseId);
                return true;
            } else {
                System.out.println("Course with ID " + courseId + " does not exist in the database.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteCourseFromMoodle(int courseId) {

        String functionName = "core_course_delete_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");
        parameters.add("courseids[0]", String.valueOf(courseId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        try {
            String response = restTemplate.postForObject(serverUrl, request, String.class);
            System.out.println("Moodle Response: " + response); // Kiểm tra phản hồi từ Moodle

            return response.contains("success");  // Hoặc điều chỉnh kiểm tra phản hồi cho phù hợp
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}


