package com.example.demo.Service;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CourseRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CoursesService {
    @Autowired
    private CourseRepo repo;


    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;


    public List<Courses> getAllCourses() {
        List<Courses> courses = repo.findAll();
        System.out.println("Courses fetched: " + courses.size());
        return courses;
    }

    public Optional<Courses> getCourseByMoodleId(Integer moodleCourseId) {
        return repo.findByMoodleCourseId(moodleCourseId);
    }

   // cap nhat
   public List<CoursesDto> fetchCoursesFromMoodle() {
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
                   if (courseId != 1) { // Skip course with ID = 1
                       CoursesDto course = new CoursesDto();
                       course.setId(courseId);
                       course.setFullname(courseNode.path("fullname").asText());
                       course.setShortname(courseNode.path("shortname").asText());
                       course.setDescription(courseNode.path("summary").asText());
                       int categoryId = courseNode.path("categoryid").asInt();
                       course.setCategory(categoryId);

                       // Fetch category name from Moodle
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

    public String fetchCategoryNameFromMoodle(int categoryId) {
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

    public List<CategoryDto> fetchCategoriesFromMoodle() {
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
                    category.setCategoryId(categoryNode.path("id").asInt()); // Sửa đây để lấy id làm categoryId
                    category.setParentCategoryId(categoryNode.path("parent").asInt()); // Lấy parent cho parentCategoryId
                    categoryList.add(category);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return categoryList;
    }

    public void synchronizeCourses(List<CoursesDto> moodleCourses) {
        System.out.println("Starting course synchronization...");

        // Fetch the current courses from the database
        List<Courses> existingCourses = repo.findAll();
        Set<Integer> existingCourseIds = existingCourses.stream()
                .map(Courses::getMoodleCourseId)
                .collect(Collectors.toSet());

        // Store the IDs of courses fetched from Moodle
        Set<Integer> moodleCourseIds = moodleCourses.stream()
                .map(CoursesDto::getId)
                .collect(Collectors.toSet());

        for (CoursesDto moodleCourse : moodleCourses) {
            System.out.println("Processing Moodle course: " + moodleCourse.getFullname());

            if (!existingCourseIds.contains(moodleCourse.getId())) {
                // Add new course to the database
                Courses newCourse = new Courses();
                newCourse.setFullname(moodleCourse.getFullname());
                newCourse.setShortname(moodleCourse.getShortname());
                newCourse.setDescription(moodleCourse.getDescription());
                newCourse.setCategory(moodleCourse.getCategory());
                newCourse.setMoodleCourseId(moodleCourse.getId());
                newCourse.setCategoryName(moodleCourse.getCategoryName());

                repo.save(newCourse);
                System.out.println("Added new course: " + newCourse.getFullname());
            } else {
                // Update existing course if necessary
                Courses existingCourse = existingCourses.stream()
                        .filter(course -> course.getMoodleCourseId().equals(moodleCourse.getId()))
                        .findFirst()
                        .orElse(null);

                if (existingCourse != null) {
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

        // Remove courses that no longer exist on Moodle
        for (Courses existingCourse : existingCourses) {
            if (!moodleCourseIds.contains(existingCourse.getMoodleCourseId())) {
                repo.delete(existingCourse);
                System.out.println("Deleted course: " + existingCourse.getFullname());
            }
        }

        System.out.println("Course synchronization completed.");
    }


    // Tạo khóa học
    public String createMoodleCourse(CoursesDto coursesDto) {
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

    public Integer extractMoodleCourseId(String moodleResponse) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(moodleResponse);
            return root.path(0).path("id").asInt();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Optional<Integer> createCourse(CoursesDto coursesDto) {
        String moodleResponse = createMoodleCourse(coursesDto);
        if (moodleResponse != null && moodleResponse.contains("id")) {
            Integer moodleCourseId = extractMoodleCourseId(moodleResponse);
            return Optional.of(moodleCourseId);
        }
        return Optional.empty();
    }


    // Xóa khóa học

    public boolean deleteCourse(int courseId) {
        // Delete from Moodle first
        boolean moodleSuccess = deleteCourseFromMoodle(courseId);
        if (!moodleSuccess) {
            return false;  // If Moodle deletion fails, return false
        }

        // If Moodle deletion succeeds, delete from the database
        return deleteCourseFromDatabase(courseId);
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
            System.out.println("Moodle Response: " + response);

            return response.contains("success");  // Adjust based on actual Moodle API response
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    // Xử lý logic cập nhật khóa học
    public String updateCourse(CoursesDto coursesDto) {
        System.out.println("Moodle Course DTO ID: " + coursesDto.getMoodleCourseId()); // In ra Moodle ID để kiểm tra
        System.out.println("Category ID: " + coursesDto.getCategory());

        Optional<Courses> courseOpt = repo.findByMoodleCourseId(coursesDto.getMoodleCourseId()); // Tìm theo moodle_course_id
        if (courseOpt.isPresent()) {
            Courses course = courseOpt.get();
            course.setFullname(coursesDto.getFullname());
            course.setShortname(coursesDto.getShortname());
            course.setCategory(coursesDto.getCategory());
            course.setDescription(coursesDto.getDescription());
            course.setMoodleCourseId(coursesDto.getMoodleCourseId());

            System.out.println("Saving course with Moodle Course ID: " + coursesDto.getMoodleCourseId());

            repo.save(course);

            if (updateMoodleCourse(coursesDto)) {
                return "success";
            } else {
                return "Failed to update the course on Moodle. Please try again.";
            }
        } else {
            return "Course not found.";
        }
    }


    // Phương thức cập nhật khóa học lên Moodle
    public boolean updateMoodleCourse(CoursesDto coursesDto) {
        String functionName = "core_course_update_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courses[0][id]", String.valueOf(coursesDto.getMoodleCourseId()));
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
        System.out.println(serverUrl);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Nếu có cảnh báo, nhưng là mảng trống thì bỏ qua
            if (rootNode.has("warnings")) {
                JsonNode warningsNode = rootNode.get("warnings");
                if (warningsNode.isArray() && warningsNode.size() > 0) {
                    for (JsonNode warning : warningsNode) {
                        System.out.println("Warning: " + warning.get("message").asText());
                    }
                    return false; // Trả về false nếu có cảnh báo thực sự
                }
            }
            // Nếu không có cảnh báo, nghĩa là cập nhật thành công
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


}
