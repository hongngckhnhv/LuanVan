package com.example.demo.Controller;
import com.example.demo.Dto.CategoryDto;
import com.example.demo.Model.Categories;
import com.example.demo.Repository.CategoriesRepo;
import com.example.demo.Service.UserService;
import jakarta.validation.Valid;
import org.json.JSONArray;
import org.json.JSONObject;
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
public class CategoriesController {
    private final CategoriesRepo repo;

    @Autowired
    public CategoriesController(CategoriesRepo repo){
        this.repo = repo;
    }

    @GetMapping("/categories")
    public String showCreateCategories(Model model){
        CategoryDto categoryDto = new CategoryDto();
        model.addAttribute("categoryDto", categoryDto);
        return "categories_form";
    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute CategoryDto categoryDto, BindingResult bindingResult, Model model) {
        // Kiểm tra lỗi form trước
        if (bindingResult.hasErrors()) {
            return "categories_form";
        }

        // Tạo category mới và lưu vào CSDL tạm thời (chưa có categoryId từ Moodle)
        Categories newCategory = new Categories();
        newCategory.setName(categoryDto.getName());
        repo.save(newCategory); // Lưu tạm thời để có id của entity (nếu cần)

        // Tạo category trên Moodle
        String moodleCategoryId = createMoodleCategory(categoryDto);

        if (moodleCategoryId != null) {
            // Cập nhật categoryId từ Moodle và lưu lại vào CSDL
            newCategory.setCategoryId(Integer.parseInt(moodleCategoryId));
            repo.save(newCategory); // Lưu lại đối tượng với categoryId từ Moodle
            return "redirect:/layout_course";
        } else {
            return "redirect:/layout_course";
        }
    }

    private String createMoodleCategory(CategoryDto categoryDto) {
        String token = "2f8b6d0d241565fd8731dcabcf342e3e";
        String domainName = "http://localhost/demo.hoangngockhanh.vn";
        String functionName = "core_course_create_categories";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("categories[0][name]", categoryDto.getName());
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();

        // Gọi API của Moodle để tạo danh mục và lưu phản hồi vào biến response
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        // Trả về phản hồi từ Moodle
        System.out.println(serverUrl);
        System.out.println(response);

        // Parse JSON response to extract the ID
        try {
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() > 0) {
                JSONObject categoryObject = jsonArray.getJSONObject(0);
                int categoryId = categoryObject.getInt("id");
                System.out.println("Category ID from Moodle: " + categoryId); // Debugging line
                return String.valueOf(categoryId);  // Trả về ID của danh mục dưới dạng String
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Failed to extract ID from Moodle response.");
        return null;
    }


}
