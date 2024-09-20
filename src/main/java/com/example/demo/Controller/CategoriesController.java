package com.example.demo.Controller;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Model.Categories;
import com.example.demo.Repository.CategoriesRepo;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class CategoriesController {
    private final CategoriesRepo repo;

    @Autowired
    public CategoriesController(CategoriesRepo repo) {
        this.repo = repo;
    }

    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

    @GetMapping("/categories")
    public String showCreateCategories(Model model) {
        CategoryDto categoryDto = new CategoryDto();
        List<Categories> categories = repo.findAll(Sort.by("name"));

        // Log số lượng danh mục
        System.out.println("Number of categories in DB: " + categories.size());

        // Luôn đồng bộ và cập nhật với Moodle
        System.out.println("Syncing and updating categories from Moodle...");
        updateCategoriesFromMoodle();

        // Sau khi đồng bộ, lấy lại danh sách danh mục từ CSDL
        categories = repo.findAll(Sort.by("name"));

        model.addAttribute("categoryDto", categoryDto);
        model.addAttribute("categories", categories);
        return "categories_form";
    }

    private void updateCategoriesFromMoodle() {
        // Gọi API Moodle để lấy danh mục
        List<Categories> moodleCategories = fetchCategoriesFromMoodle();
        System.out.println("Fetched " + moodleCategories.size() + " categories from Moodle.");

        for (Categories moodleCategory : moodleCategories) {
            Optional<Categories> existingCategoryOpt = repo.findByCategoryId(moodleCategory.getCategoryId());

            if (existingCategoryOpt.isPresent()) {
                // Kiểm tra nếu cần cập nhật
                Categories existingCategory = existingCategoryOpt.get();
                if (!existingCategory.getName().equals(moodleCategory.getName()) ||
                        !Objects.equals(existingCategory.getParentCategoryId(), moodleCategory.getParentCategoryId())) {
                    // Nếu có thay đổi, cập nhật danh mục
                    existingCategory.setName(moodleCategory.getName());
                    existingCategory.setParentCategoryId(moodleCategory.getParentCategoryId());
                    repo.save(existingCategory);
                    System.out.println("Updated category: " + existingCategory.getName());
                } else {
                    System.out.println("No changes detected for category: " + existingCategory.getName());
                }
            } else {
                // Nếu không tồn tại trong DB, thêm mới danh mục
                repo.save(moodleCategory);
                System.out.println("Added new category: " + moodleCategory.getName());
            }
        }
    }

    private Categories fetchCategoryFromMoodle(Integer categoryId) {
        String functionName = "core_course_get_categories";

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject categoryObject = new JSONObject();
        categoryObject.put("ids", new JSONArray().put(categoryId));
        jsonArray.put(categoryObject);
        jsonObject.put("categories", jsonArray);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(jsonObject.toString(), headers);

        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName + "&moodlewsrestformat=json";

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(serverUrl, HttpMethod.POST, request, String.class);

        String response = responseEntity.getBody();
        System.out.println("Response from Moodle: " + response);

        System.out.println(serverUrl);
        if (response != null) {
            try {
                if (response.startsWith("[")) {
                    JSONArray jsonArrayResponse = new JSONArray(response);
                    if (jsonArrayResponse.length() > 0) {
                        JSONObject categoryObjectResponse = jsonArrayResponse.getJSONObject(0);
                        Categories category = new Categories();
                        category.setCategoryId(categoryObjectResponse.getInt("id"));
                        category.setName(categoryObjectResponse.getString("name"));
                        if (categoryObjectResponse.has("parent")) {
                            category.setParentCategoryId(categoryObjectResponse.getInt("parent"));
                        }
                        return category;
                    }
                } else if (response.startsWith("{")) {
                    JSONObject errorResponse = new JSONObject(response);
                    System.out.println("Error Details: " + errorResponse.getString("message"));
                } else {
                    System.out.println("Unexpected response format from Moodle.");
                }
            } catch (JSONException e) {
                System.err.println("JSON Parsing Error: " + e.getMessage());
            }
        }

        return null;
    }

//    private boolean isCategoryExistsInDb(Integer categoryId) {
//        return repo.findByCategoryId(categoryId).isPresent();
//    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute CategoryDto categoryDto, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "categories_form";
        }

        Integer parentCategoryId = categoryDto.getParentCategoryId();
        if (parentCategoryId != null && parentCategoryId > 0) {
            Categories parentCategory = fetchCategoryFromMoodle(parentCategoryId);
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

        String moodleCategoryId = createMoodleCategory(categoryDto);

        if (moodleCategoryId != null) {
            savedCategory.setCategoryId(Integer.parseInt(moodleCategoryId));
            repo.save(savedCategory);
            return "redirect:/layout_course";
        } else {
            model.addAttribute("error", "Không thể tạo danh mục trên Moodle.");
            return "categories_form";
        }
    }

    private String createMoodleCategory(CategoryDto categoryDto) {
        String functionName = "core_course_create_categories";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("categories[0][name]", categoryDto.getName());

        if (categoryDto.getParentCategoryId() != null && categoryDto.getParentCategoryId() > 0) {
            parameters.add("categories[0][parent]", String.valueOf(categoryDto.getParentCategoryId()));
        }

        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = null;

        try {
            response = restTemplate.postForObject(serverUrl, request, String.class);
            System.out.println("Response from Moodle2: " + response);

            if (response != null && response.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(response);
                if (jsonArray.length() > 0) {
                    JSONObject categoryObject = jsonArray.getJSONObject(0);
                    int categoryId = categoryObject.getInt("id");
                    System.out.println("Category ID from Moodle: " + categoryId);
                    return String.valueOf(categoryId);
                }
            } else {
                System.out.println("Unexpected response format from Moodle.");
            }
        } catch (Exception e) {
            System.err.println("Error creating category on Moodle: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private List<Categories> fetchCategoriesFromMoodle() {
        String functionName = "core_course_get_categories";

        // Chuẩn bị các tham số và request
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        String response = null;

        List<Categories> categoriesList = new ArrayList<>();

        try {
            // Gửi request và nhận phản hồi từ Moodle
            response = restTemplate.postForObject(serverUrl, request, String.class);
            System.out.println("Response from Moodle: " + response);

            if (response != null && response.startsWith("[")) {
                // Phản hồi là mảng JSON, xử lý từng đối tượng
                JSONArray jsonArray = new JSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject categoryObject = jsonArray.getJSONObject(i);
                    Categories category = new Categories();

                    // Lấy và gán ID và tên cho danh mục
                    category.setCategoryId(categoryObject.getInt("id"));
                    category.setName(categoryObject.getString("name"));

                    // Xử lý trường "parent" nếu tồn tại
                    if (categoryObject.has("parent") && !categoryObject.isNull("parent")) {
                        category.setParentCategoryId(categoryObject.getInt("parent"));
                    }

                    // Thêm vào danh sách kết quả
                    categoriesList.add(category);
                }
            } else {
                // Phản hồi không có định dạng như mong đợi
                System.out.println("Unexpected response format from Moodle: " + response);
            }
        } catch (Exception e) {
            // Bắt và log bất kỳ lỗi nào xảy ra
            System.err.println("Error fetching categories from Moodle: " + e.getMessage());
            e.printStackTrace();
        }

        // Trả về danh sách danh mục
        return categoriesList;
    }
}
