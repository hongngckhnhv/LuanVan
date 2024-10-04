package com.example.demo.Controller;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Model.Categories;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class CategoriesController {
    private final CategoriesRepo repo;

    @Autowired
    public CategoriesController(CategoriesRepo repo) {
        this.repo = repo;
    }

    // Phương thức xóa category
    @Autowired
    private CategoryService categoryService;

    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

    @GetMapping("/categories")
    public String showCreateCategories(Model model) {
        CategoryDto categoryDto = new CategoryDto();

        // Luôn đồng bộ và cập nhật với Moodle
        System.out.println("Syncing and updating categories from Moodle...");
        categoryService.updateCategoriesFromMoodle();

        // Sau khi đồng bộ, lấy lại danh sách danh mục từ CSDL
        List<Categories> categories = categoryService.getAllCategories();

        model.addAttribute("categoryDto", categoryDto);
        model.addAttribute("categories", categories);
        return "categories_form";
    }


    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute CategoryDto categoryDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "categories_form";
        }

        Integer parentCategoryId = categoryDto.getParentCategoryId();
        if (parentCategoryId != null && parentCategoryId > 0) {
            Categories parentCategory = categoryService.fetchCategoryFromMoodle(parentCategoryId); // Gọi từ service
            if (parentCategory != null) {
                categoryDto.setParentCategoryId(parentCategoryId);
            } else {
                model.addAttribute("error", "Danh mục cha không tồn tại trên Moodle, không sử dụng danh mục cha.");
                categoryDto.setParentCategoryId(null);
            }
        }

        Categories newCategory = new Categories();
        newCategory.setName(categoryDto.getName());
        newCategory.setParentCategoryId(categoryDto.getParentCategoryId());
        Categories savedCategory = repo.save(newCategory);

        String moodleCategoryId = categoryService.createMoodleCategory(categoryDto); // Gọi từ service

        if (moodleCategoryId != null) {
            savedCategory.setCategoryId(Integer.parseInt(moodleCategoryId));
            repo.save(savedCategory);
            return "redirect:/layout_course";
        } else {
            model.addAttribute("error", "Không thể tạo danh mục trên Moodle.");
            return "categories_form";
        }
    }

    @PostMapping("/delete_category")
    public String deleteCategory(@RequestParam("id") int categoryId, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Deleting category with ID: " + categoryId);

            String newCategoryName = categoryService.deleteCategoryWithCourses(categoryId);

            if (newCategoryName != null) {
                redirectAttributes.addFlashAttribute("message", "Courses moved to new category: " + newCategoryName);
                redirectAttributes.addFlashAttribute("messageType", "success");
            } else {
                redirectAttributes.addFlashAttribute("message", "Category deleted successfully.");
                redirectAttributes.addFlashAttribute("messageType", "success");
            }

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred.");
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/layout_course";
    }


}
