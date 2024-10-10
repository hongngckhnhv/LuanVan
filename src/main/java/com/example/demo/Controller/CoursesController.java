package com.example.demo.Controller;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Dto.CoursesDto;
import com.example.demo.Model.Categories;
import com.example.demo.Model.Courses;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Repository.CourseRepo;
import com.example.demo.Service.CategoryService;
import com.example.demo.Service.CoursesService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CoursesController {
    private final CourseRepo repo;
    private final CategoriesRepo categoryRepo;
    private final CoursesService coursesService; // Tiêm service qua constructor
    private final CategoryService categoryService;

    @Autowired
    public CoursesController(CourseRepo repo, CategoriesRepo categoryRepo, CoursesService coursesService, CategoryService categoryService) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.coursesService = coursesService;
        this.categoryService = categoryService;
    }


    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

    @GetMapping("/layout_course")
    public String showCourseList(Model model) {
        // Fetch courses from Moodle
        List<CoursesDto> moodleCourses = coursesService.fetchCoursesFromMoodle();
        // Synchronize courses between Moodle and the database
        coursesService.synchronizeCourses(moodleCourses);
        // Fetch courses from the database
        List<Courses> courses = repo.findAll();

        // Luôn đồng bộ và cập nhật với Moodle
        System.out.println("Syncing and updating categories from Moodle...");
        categoryService.updateCategoriesFromMoodle();

        // Sau khi đồng bộ, lấy lại danh sách danh mục từ CSDL
        List<Categories> categories = categoryService.getAllCategories();

        model.addAttribute("courses", courses);
        model.addAttribute("moodleCourses", moodleCourses);
        model.addAttribute("categories", categories);
        // Fetch categories from Moodle
        List<CategoryDto> moodleCategories = coursesService.fetchCategoriesFromMoodle();
        model.addAttribute("moodleCategories", moodleCategories);

        return "layout_course";
    }

    @GetMapping("/create_course")
    public String showCreateCourse(Model model) {
        CoursesDto coursesDto = new CoursesDto();
        model.addAttribute("coursesDto", coursesDto);

        // Lấy danh sách category từ Moodle qua MoodleService
        List<CategoryDto> categoriesFromMoodle = coursesService.fetchCategoriesFromMoodle();
        model.addAttribute("categories", categoriesFromMoodle);

        return "create_course";
    }

    @PostMapping("/create_course")
    public String showCreateCourses(@Valid @ModelAttribute CoursesDto coursesDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepo.findAll());
            return "create_course";
        }

        // Create course using the service
        Optional<Integer> moodleResponse = coursesService.createCourse(coursesDto);
        if (moodleResponse.isPresent()) {
            Integer moodleCourseId = moodleResponse.get();

            // Lấy categoryName từ cơ sở dữ liệu
            Optional<Categories> categoryOpt = categoryRepo.findById(coursesDto.getCategory());
            String categoryName = categoryOpt.isPresent() ? categoryOpt.get().getName() : "";

            Courses newCourse = new Courses();
            newCourse.setFullname(coursesDto.getFullname());
            newCourse.setShortname(coursesDto.getShortname());
            newCourse.setCategory(coursesDto.getCategory());
            newCourse.setDescription(coursesDto.getDescription());
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

    // xóa course
    @PostMapping("/delete_moodle_course")
    public String deleteMoodleCourse(@RequestParam("id") int courseId, RedirectAttributes redirectAttributes) {
        System.out.println("Course ID: " + courseId); // Log the courseId for debugging

        boolean success = coursesService.deleteCourse(courseId);
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Course deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete the course from Moodle or the database.");
        }

        return "redirect:/layout_course";
    }


    @GetMapping("/edit_course")
    public String showEditCourse(@RequestParam("moodle_course_id") Integer moodleCourseId, Model model) {
        System.out.println("Moodle Course ID: " + moodleCourseId);
        Optional<Courses> courseOpt = coursesService.getCourseByMoodleId(moodleCourseId);

        if (courseOpt.isPresent()) {
            Courses course = courseOpt.get();
            CoursesDto coursesDto = new CoursesDto();

            // Gán các thông tin từ entity vào DTO
            coursesDto.setId(course.getId());
            coursesDto.setFullname(course.getFullname());
            coursesDto.setShortname(course.getShortname());
            coursesDto.setCategory(course.getCategory());
            coursesDto.setDescription(course.getDescription());
            coursesDto.setMoodleCourseId(course.getMoodleCourseId()); // Thêm dòng này để set Moodle Course ID

            // In dữ liệu ra console để kiểm tra
            System.out.println("DTO Course ID: " + coursesDto.getId());
            System.out.println("DTO Full Name: " + coursesDto.getFullname());
            System.out.println("DTO Short Name: " + coursesDto.getShortname());
            System.out.println("DTO Category: " + coursesDto.getCategory());
            System.out.println("DTO Description: " + coursesDto.getDescription());
            System.out.println("DTO Moodle Course ID: " + coursesDto.getMoodleCourseId()); // In ra để kiểm tra

            // Đưa đối tượng coursesDto vào model để Thymeleaf sử dụng
            model.addAttribute("coursesDto", coursesDto);

            // Thêm danh sách categories vào model để hiển thị trong dropdown
            model.addAttribute("categories", categoryRepo.findAll());

            return "edit_course"; // Trả về trang edit_course
        } else {
            // Nếu không tìm thấy khóa học, trả về trang layout_course với thông báo lỗi
            model.addAttribute("errorMessage", "Course not found");
            return "redirect:/layout_course";
        }
    }

    @PostMapping("/edit_course")
    public String updateCourse(@ModelAttribute CoursesDto coursesDto, RedirectAttributes redirectAttributes, Model model) {
        // Kiểm tra giá trị của Moodle Course ID
        System.out.println("Moodle Course DTO ID1: " + coursesDto.getMoodleCourseId());
        System.out.println("Category ID from form: " + coursesDto.getCategory());

        String result = coursesService.updateCourse(coursesDto);

        if ("success".equals(result)) {
            // Thêm successMessage vào RedirectAttributes khi cập nhật thành công
            redirectAttributes.addFlashAttribute("successMessage", "Course updated successfully!");
            return "redirect:/layout_course";  // Sau đó chuyển hướng đến trang layout_course
        } else {
            model.addAttribute("errorMessage", result);
            model.addAttribute("coursesDto", coursesDto);
            model.addAttribute("categories", categoryRepo.findAll());
            return "edit_course";  // Nếu có lỗi, ở lại trang edit_course
        }
    }

}


