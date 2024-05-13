package com.example.demo.Controller;

import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CourseRepo;
//import com.example.demo.Service.CoursesService;
import com.example.demo.Service.UserService;
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

import java.util.List;
import java.util.Optional;

@Controller
public class CoursesController {
    private final CourseRepo repo;

    @Autowired
    public CoursesController(CourseRepo repo) {
        this.repo = repo;
    }

    @GetMapping("/layout_course")
    public String showCourseList(Model model) {
        List<Courses> courses = repo.findAll();
        model.addAttribute("courses", courses);
        return "/layout_course";
    }

    @GetMapping("/create_course")
    public String showCreateCourse(Model model) {
        CoursesDto coursesDto = new CoursesDto();
        model.addAttribute("coursesDto", coursesDto);
        return "create_course"; // Trả về trang tạo mới, không phải "/layout_course"
    }

    @PostMapping("/create_course")
    public String showCreateCourses(@Valid @ModelAttribute CoursesDto coursesDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "create_course"; // Trả về lại trang tạo mới nếu có lỗi
        }

        // Lưu dữ liệu vào CSDL
        Courses newCourse = new Courses();
        newCourse.setFullname(coursesDto.getFullname());
        newCourse.setShortname(coursesDto.getShortname());
        newCourse.setCategory(coursesDto.getCategory());
        newCourse.setDescription(coursesDto.getDescription());
        repo.save(newCourse);

        // Tạo khóa học trên Moodle
        String moodleResponse = createMoodleCourse(coursesDto);

        // Xử lý phản hồi từ Moodle
        if (moodleResponse != null && moodleResponse.contains("success")) {
            // Nếu tạo khóa học thành công trên Moodle, chuyển hướng đến trang hiển thị danh sách khóa học
            return "redirect:/layout_course";
        } else {
            // Nếu có lỗi khi tạo khóa học trên Moodle, có thể xử lý theo ý của bạn, ví dụ hiển thị thông báo lỗi cho người dùng
            return "redirect:/layout_course";
        }
    }


    private String createMoodleCourse(CoursesDto coursesDto) {
        // Thay thế bằng token của Moodle của bạn
        String token = "388cdd1555fc753635b67bbba524ef87";

        // Thay thế bằng domain của Moodle của bạn
        String domainName = "http://localhost/demo.ngockhanh.vn";

        // Tên của hàm API trong Moodle để tạo khóa học
        String functionName = "core_course_create_courses";

        // Xây dựng các tham số cho cuộc gọi API của Moodle
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courses[0][fullname]", coursesDto.getFullname());
        parameters.add("courses[0][shortname]", coursesDto.getShortname());
        parameters.add("courses[0][categoryid]", coursesDto.getCategory());
        parameters.add("moodlewsrestformat", "json"); // Đảm bảo định dạng dữ liệu trả về là JSON

        // Tạo header để chỉ định Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Tạo đối tượng HttpEntity để đóng gói các tham số và header
        HttpEntity
                <MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        // Xây dựng URL cho cuộc gọi API của Moodle
        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        // Gọi API của Moodle để tạo khóa học
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        // Trả về phản hồi từ Moodle
        return response;
    }



    @GetMapping("/edit_course")
    public String showEditPage(Model model, @RequestParam int id) {
        try {
            // Kiểm tra xem khóa học có tồn tại không trước khi hiển thị trang chỉnh sửa
            Optional<Courses> optionalCourse = repo.findById(id);
            if (optionalCourse.isPresent()) {
                Courses courses = optionalCourse.get();
                model.addAttribute("courses", courses);

                // Ánh xạ dữ liệu từ entity sang DTO
                CoursesDto coursesDto = mapCoursesToDto(courses);
                model.addAttribute("coursesDto", coursesDto);

                return "/edit_course";
            } else {
                // Xử lý khi không tìm thấy khóa học trong cơ sở dữ liệu
                return "redirect:/layout_course";
            }
        } catch (Exception e) {
            // Xử lý lỗi và chuyển hướng đến trang danh sách khóa học
            System.out.println("Exception: " + e.getMessage());
            return "redirect:/layout_course";
        }
    }

    // Ánh xạ dữ liệu từ entity sang DTO
    private CoursesDto mapCoursesToDto(Courses courses) {
        CoursesDto coursesDto = new CoursesDto();
        coursesDto.setId(courses.getId()); // Đặt ID cho DTO
        coursesDto.setFullname(courses.getFullname());
        coursesDto.setShortname(courses.getShortname());
        coursesDto.setCategory(courses.getCategory());
        coursesDto.setDescription(courses.getDescription());
        return coursesDto;
    }


    @PostMapping("/edit_course")
    public String updateCourse(Model model, @ModelAttribute @Valid CoursesDto coursesDto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                // Nếu có lỗi trong dữ liệu nhập vào, trả về trang chỉnh sửa với thông tin khóa học hiện tại
                return "/edit_course";
            } else {
                // Không có lỗi, tiến hành cập nhật thông tin khóa học trong cơ sở dữ liệu và Moodle
                int courseId = coursesDto.getId(); // Lấy ID từ DTO
                Optional<Courses> optionalCourse = repo.findById(courseId);
                if (optionalCourse.isPresent()) {
                    Courses updatedCourse = optionalCourse.get();
                    updatedCourse.setFullname(coursesDto.getFullname());
                    updatedCourse.setShortname(coursesDto.getShortname());
                    updatedCourse.setCategory(coursesDto.getCategory());
                    updatedCourse.setDescription(coursesDto.getDescription());
                    repo.save(updatedCourse);

                    // Gọi phương thức để cập nhật thông tin khóa học trong Moodle
                    String moodleResponse = updateMoodleCourse(coursesDto, courseId);
                    if (moodleResponse != null && moodleResponse.contains("success")) {
                        // Xử lý khi cập nhật thành công trên Moodle (nếu cần)
                    } else {
                        // Xử lý khi gặp lỗi khi cập nhật trên Moodle (nếu cần)
                    }
                    // Sau khi cập nhật, chuyển hướng về trang hiển thị danh sách khóa học
                    return "redirect:/layout_course";
                } else {
                    // Xử lý khi không tìm thấy khóa học trong cơ sở dữ liệu
                    return "redirect:/layout_course";
                }
            }
        } catch (Exception e) {
            // Xử lý lỗi và chuyển hướng đến trang danh sách khóa học
            System.out.println("Exception: " + e.getMessage());
            return "redirect:/layout_course";
        }
    }

    private String updateMoodleCourse(CoursesDto coursesDto, int courseId) {
        // Thay thế bằng token của Moodle của bạn
        String token = "388cdd1555fc753635b67bbba524ef87";

        // Thay thế bằng domain của Moodle của bạn
        String domainName = "http://localhost/demo.ngockhanh.vn";

        // Tên của hàm API trong Moodle để cập nhật khóa học
        String functionName = "core_course_update_courses";

        // Xây dựng các tham số cho cuộc gọi API của Moodle
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courses[0][id]", String.valueOf(courseId)); // ID của khóa học cần cập nhật
        parameters.add("courses[0][fullname]", coursesDto.getFullname());
        parameters.add("courses[0][shortname]", coursesDto.getShortname());
        parameters.add("courses[0][categoryid]", coursesDto.getCategory());
        parameters.add("moodlewsrestformat", "json"); // Đảm bảo định dạng dữ liệu trả về là JSON

        // Tạo header để chỉ định Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Tạo đối tượng HttpEntity để đóng gói các tham số và header
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        // Xây dựng URL cho cuộc gọi API của Moodle
        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        // Gọi API của Moodle để cập nhật thông tin khóa học
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println(response);
        // Trả về phản hồi từ Moodle
        return response;
    }

    @GetMapping("/delete_course")
    public String deleteCourse(@RequestParam int id) {
        try {
            // Lấy thông tin khóa học từ cơ sở dữ liệu
            Optional<Courses> optionalCourse = repo.findById(id);
            if (optionalCourse.isPresent()) {
                Courses course = optionalCourse.get();

                // Gọi phương thức xóa khóa học trên Moodle
                deleteMoodleCourse(course.getId());

                // Xóa khóa học từ cơ sở dữ liệu của ứng dụng web
                repo.delete(course);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return "redirect:/layout_course";
    }


    private void deleteMoodleCourse(int courseId) {
        // Thay thế bằng token của Moodle của bạn
        String token = "388cdd1555fc753635b67bbba524ef87";

        // Thay thế bằng domain của Moodle của bạn
        String domainName = "http://localhost/demo.ngockhanh.vn";

        // Tên của hàm API trong Moodle để xóa khóa học
        String functionName = "core_course_delete_courses";

        // Xây dựng các tham số cho cuộc gọi API của Moodle
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("courseids[0]", String.valueOf(courseId)); // ID của khóa học cần xóa

        // Tạo header để chỉ định Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Tạo đối tượng HttpEntity để đóng gói các tham số và header
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        // Xây dựng URL cho cuộc gọi API của Moodle
        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        // Gọi API của Moodle để xóa khóa học
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(serverUrl, request, String.class);
    }
}
