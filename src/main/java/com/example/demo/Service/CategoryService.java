package com.example.demo.Service;

import com.example.demo.Dto.CategoryDto;
import com.example.demo.Model.Categories;
import com.example.demo.Repository.CategoriesRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoriesRepo repo;

    @Value("${moodle.token}")
    private String token;

    @Value("${moodle.domain}")
    private String domainName;

    private static final int ROOT_CATEGORY_ID = 0; // Giả sử ID của danh mục gốc là 0

    // hiển thị category
    @Transactional
    public void updateCategoriesFromMoodle() {
        List<Categories> moodleCategories = fetchCategoriesFromMoodle();
        System.out.println("Fetched " + moodleCategories.size() + " categories from Moodle.");

        for (Categories moodleCategory : moodleCategories) {
            Optional<Categories> existingCategoryOpt = repo.findByCategoryId(moodleCategory.getCategoryId());

            if (existingCategoryOpt.isPresent()) {
                Categories existingCategory = existingCategoryOpt.get();
                if (!existingCategory.getName().equals(moodleCategory.getName()) ||
                        !Objects.equals(existingCategory.getParentCategoryId(), moodleCategory.getParentCategoryId())) {
                    existingCategory.setName(moodleCategory.getName());
                    existingCategory.setParentCategoryId(moodleCategory.getParentCategoryId());
                    repo.save(existingCategory);
                    System.out.println("Updated category: " + existingCategory.getName());
                } else {
                    System.out.println("No changes detected for category: " + existingCategory.getName());
                }
            } else {
                repo.save(moodleCategory);
                System.out.println("Added new category: " + moodleCategory.getName());
            }
        }
    }

    // Thêm phương thức này để lấy tất cả danh mục
    public List<Categories> getAllCategories() {
        return repo.findAll(Sort.by("name"));
    }


    private List<Categories> fetchCategoriesFromMoodle() {
        // Logic lấy danh mục từ Moodle
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

    public Categories fetchCategoryFromMoodle(Integer categoryId) {
        // Logic lấy một danh mục cụ thể từ Moodle

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

    public String createMoodleCategory(CategoryDto categoryDto) {
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
    // xóa category
    @Transactional
    public void deleteCategoryById(int categoryId) {
        if (repo.findByCategoryId(categoryId).isPresent()) {
            repo.deleteByCategoryId(categoryId);
        } else {
            System.out.println("Category with ID " + categoryId + " does not exist in the database.");
            throw new EntityNotFoundException("Category not found");
        }
    }

    @Transactional
    public String deleteCategoryWithCourses(int categoryId) {
        List<Integer> courseIds = getCoursesInCategory(categoryId);
        if (courseIds.isEmpty()) {
            boolean moodleSuccess = deleteCategoryOnMoodle(categoryId);
            if (moodleSuccess) {
                deleteCategoryById(categoryId);
            } else {
                throw new IllegalStateException("Failed to delete category on Moodle.");
            }
        } else {
            int newCategoryId = getNewCategoryIdExcept(categoryId);
            String newCategoryName = getCategoryNameById(newCategoryId);

            if (!moveCoursesToNewCategory(courseIds, newCategoryId)) {
                throw new IllegalStateException("Failed to move some courses.");
            }

            notifyUserAboutMovedCourses(newCategoryId, newCategoryName);

            List<Integer> remainingCourses = getCoursesInCategory(categoryId);
            if (!remainingCourses.isEmpty()) {
                throw new IllegalStateException("Failed to move all courses.");
            }

            boolean moodleSuccess = deleteCategoryOnMoodle(categoryId);
            if (moodleSuccess) {
                deleteCategoryById(categoryId);
            } else {
                throw new IllegalStateException("Failed to delete category on Moodle.");
            }

            // Trả về tên danh mục mới sau khi chuyển
            return newCategoryName;
        }
        return null;
    }



    // Hàm thông báo cho người dùng về danh mục đã di chuyển
    private void notifyUserAboutMovedCourses(int newCategoryId, String newCategoryName) {
        // In ra console thông báo (có thể giữ lại hoặc xóa)
        System.out.println("Courses have been moved to category ID: " + newCategoryId + " (" + newCategoryName + ")");
    }


    @Transactional
    public String getCategoryNameById(int categoryId) {
        // Tìm danh mục theo ID
        Categories category = repo.findByCategoryId(categoryId).orElseThrow(() ->
                new EntityNotFoundException("Category not found for ID: " + categoryId));

        // Trả về tên danh mục
        return category.getName(); // Giả sử có phương thức getName() trong lớp Categories
    }



    // Trả về danh sách ID của các category, ngoại trừ category đang xóa
    public List<Integer> getAllCategoriesExcluding(int excludedCategoryId) {
        return repo.findAll()
                .stream()
                .filter(category -> category.getCategoryId() != excludedCategoryId)
                .map(category -> category.getCategoryId())
                .collect(Collectors.toList());
    }

    // Hàm lấy danh mục mới không trùng với danh mục đang xóa
    public int getNewCategoryIdExcept(int currentCategoryId) {
        List<Integer> availableCategories = getAllCategoriesExcluding(currentCategoryId);
        List<Integer> childCategories = availableCategories.stream()
                .filter(catId -> !isRootCategory(catId)) // Lọc danh mục không phải là root
                .collect(Collectors.toList());

        if (childCategories.isEmpty()) {
            throw new IllegalStateException("No available child categories to move courses to.");
        }

        Random random = new Random();
        return childCategories.get(random.nextInt(childCategories.size()));
    }

    // Hàm kiểm tra danh mục có phải là danh mục gốc hay không
    public boolean isRootCategory(int categoryId) {
        // Giả sử bạn có một cách để lấy categoryId của danh mục
        Categories category = repo.findByCategoryId(categoryId).orElse(null);
        return category != null && category.getParentCategoryId() == ROOT_CATEGORY_ID;
    }


    // Hàm xóa category trên Moodle
    public boolean deleteCategoryOnMoodle(int categoryId) {
        String functionName = "core_course_delete_categories";
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");
        parameters.add("categories[0][id]", String.valueOf(categoryId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String serverUrl = domainName + "/webservice/rest/server.php?wstoken=" + token + "&wsfunction=" + functionName;
        RestTemplate restTemplate = new RestTemplate();

        System.out.println("delete category: " + serverUrl);
        try {
            String response = restTemplate.postForObject(serverUrl, new HttpEntity<>(parameters, headers), String.class);
            if (response.contains("exception") || response.contains("error")) {
                JSONObject errorDetails = new JSONObject(response);
                String errorMessage = errorDetails.optString("message", "Unknown error");
                System.out.println("Error deleting category on Moodle: " + errorMessage);
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Hàm di chuyển các khóa học sang danh mục mới
    public boolean moveCoursesToNewCategory(List<Integer> courseIds, int newCategoryId) {
        String functionName = "core_course_update_courses";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            parameters.add("courses[" + i + "][id]", String.valueOf(courseIds.get(i)));
            parameters.add("courses[" + i + "][categoryid]", String.valueOf(newCategoryId));
        }
        parameters.add("moodlewsrestformat", "json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        String serverUrl = domainName + "/webservice/rest/server.php?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        try {
            String response = restTemplate.postForObject(serverUrl, request, String.class);
            System.out.println("Response from Moodle (move courses): " + response);

            if (response.contains("exception") || response.contains("error")) {
                JSONObject errorDetails = new JSONObject(response);
                String errorMessage = errorDetails.optString("message", "Unknown error");
                System.out.println("Error moving courses: " + errorMessage);
                return false;
            }

            // Kiểm tra lại trạng thái của từng khóa học sau khi di chuyển
            for (Integer courseId : courseIds) {
                if (!isCourseInCategory(courseId, newCategoryId)) {
                    System.out.println("Course ID " + courseId + " was not moved correctly.");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Hàm kiểm tra khóa học đã được di chuyển vào danh mục mới chưa
    public boolean isCourseInCategory(int courseId, int categoryId) {
        String functionName = "core_course_get_courses_by_field";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");
        parameters.add("field", "id");
        parameters.add("value", String.valueOf(courseId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
        String serverUrl = domainName + "/webservice/rest/server.php?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        try {
            String response = restTemplate.postForObject(serverUrl, request, String.class);
            System.out.println("Response from Moodle (get course details): " + response);

            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray coursesArray = jsonResponse.getJSONArray("courses");

                if (coursesArray.length() > 0) {
                    JSONObject courseObject = coursesArray.getJSONObject(0);
                    int actualCategoryId = courseObject.getInt("categoryid");

                    // Kiểm tra nếu khóa học đã thực sự được di chuyển vào danh mục mới
                    return actualCategoryId == categoryId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Hàm lấy danh sách các khóa học trong danh mục (nếu chưa có)
    public List<Integer> getCoursesInCategory(int categoryId) {
        String functionName = "core_course_get_courses_by_field";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("moodlewsrestformat", "json");
        parameters.add("field", "category");
        parameters.add("value", String.valueOf(categoryId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
        String serverUrl = domainName + "/webservice/rest/server.php?wstoken=" + token + "&wsfunction=" + functionName;

        RestTemplate restTemplate = new RestTemplate();
        List<Integer> courseIds = new ArrayList<>();

        try {
            String response = restTemplate.postForObject(serverUrl, request, String.class);
            System.out.println("Response from Moodle (get courses in category): " + response);

            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray coursesArray = jsonResponse.getJSONArray("courses");

                for (int i = 0; i < coursesArray.length(); i++) {
                    JSONObject courseObject = coursesArray.getJSONObject(i);
                    int courseId = courseObject.getInt("id");
                    courseIds.add(courseId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return courseIds;
    }
}
