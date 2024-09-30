package com.example.demo.Service;

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
import java.util.List;
import java.util.Optional;

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


    public Courses createCourse(Courses course) {
        Courses savedCourse = repo.save(course);
        System.out.println("Course saved with ID: " + savedCourse.getId());
        return savedCourse;
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
