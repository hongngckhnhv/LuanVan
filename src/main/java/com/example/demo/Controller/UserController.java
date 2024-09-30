package com.example.demo.Controller;

import com.example.demo.Model.User;
import com.example.demo.Dto.UserDto;
import com.example.demo.Service.UserService;
//import com.example.demo.Service.UserSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

@Controller
public class UserController {

    @Autowired
    private UserDetailsService userDetailsService;

    private UserService userService;

    @Value("${moodle.token}")
    private String moodleToken;

    @Value("${moodle.domain}")
    private String moodleDomain;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/home")
    public String home(Model model, Principal principal) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getName());
        model.addAttribute("userdetail", userDetails);
        return "userHome";
    }

    @GetMapping("/login")
    public String login(Model model, UserDto userDto) {
        model.addAttribute("user", userDto);
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model, UserDto userDto) {
        model.addAttribute("user", userDto);
        return "register";
    }

//    @PostMapping("/register")
//    public String registerSave(@ModelAttribute("user") UserDto userDto, Model model) {
//        // Kiểm tra xem người dùng đã tồn tại trong cơ sở dữ liệu của ứng dụng web chưa
//        User existingUser = userService.findByUsername(userDto.getUsername());
//        if (existingUser != null) {
//            model.addAttribute("userexist", "Username đã tồn tại.");
//            return "register";
//        }
//
//        // Kiểm tra mật khẩu rỗng
//        if (userDto.getPassword() == null || userDto.getPassword().isEmpty()) {
//            model.addAttribute("passwordError", "Password không được để trống.");
//            return "register";
//        }
//
//        // Kiểm tra mật khẩu có chứa ký tự đặc biệt
//        if (!userDto.getPassword().matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
//            model.addAttribute("passwordError", "Password phải chứa ít nhất một ký tự đặc biệt.");
//            return "register";
//        }
//
//        // Kiểm tra mật khẩu có chứa ít nhất một chữ cái viết hoa
//        if (!userDto.getPassword().matches(".*[A-Z].*")) {
//            model.addAttribute("passwordError", "Password phải chứa ít nhất một chữ cái viết hoa.");
//            return "register";
//        }
//
//        // Kiểm tra mật khẩu có chứa ít nhất một chữ cái viết thường
//        if (!userDto.getPassword().matches(".*[a-z].*")) {
//            model.addAttribute("passwordError", "Password phải chứa ít nhất một chữ cái viết thường.");
//            return "register";
//        }
//
//        // Lưu thông tin người dùng vào cơ sở dữ liệu của ứng dụng web
//        User savedUser = userService.save(userDto);
//
//        // Tạo người dùng trên Moodle và lấy Moodle user ID
//        String moodleUserId = createMoodleUser(userDto);
//
//        // Kiểm tra phản hồi từ Moodle
//        if (moodleUserId != null) {
//            // Cập nhật userId cho người dùng đã lưu trong cơ sở dữ liệu
//            savedUser.setUserId(Integer.parseInt(moodleUserId)); // Chuyển đổi moodleUserId từ String sang int
//            userService.update(savedUser); // Giả sử bạn có phương thức update trong UserService
//
//            model.addAttribute("successMessage", "Đăng ký thành công.");
//            return "register";
//        } else {
//            // Xóa người dùng khỏi cơ sở dữ liệu nếu không thể tạo trên Moodle
//            userService.delete(savedUser.getId());
//            model.addAttribute("errorMessage", "Đăng ký không thành công. Vui lòng thử lại.");
//            return "register";
//        }
//    }
//
//    private String createMoodleUser(UserDto userDto) {
//        String functionName = "core_user_create_users";
//
//        // Xây dựng các tham số cho API của Moodle
//        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
//        parameters.add("users[0][username]", userDto.getUsername());
//        parameters.add("users[0][password]", userDto.getPassword());
//        parameters.add("users[0][firstname]", userDto.getFirstname());
//        parameters.add("users[0][lastname]", userDto.getLastname());
//        parameters.add("users[0][email]", userDto.getEmail());
//        parameters.add("moodlewsrestformat", "json"); // Đảm bảo định dạng JSON
//
//        // Tạo header cho Content-Type
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        // Đóng gói request với các tham số và headers
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
//
//        // Xây dựng URL của API Moodle
//        String serverUrl = moodleDomain + "/webservice/rest/server.php?wstoken=" + moodleToken + "&wsfunction=" + functionName;
//
//        // Gửi request đến API Moodle
//        RestTemplate restTemplate = new RestTemplate();
//        String response = restTemplate.postForObject(serverUrl, request, String.class);
//
//        // In phản hồi để kiểm tra
//        System.out.println(response);
//        System.out.println(serverUrl);
//        System.out.println(parameters);
//
//        // Phân tích JSON để lấy user ID từ Moodle
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode root = objectMapper.readTree(response);
//
//            // Kiểm tra nếu phản hồi là mảng JSON
//            if (root.isArray() && root.size() > 0) {
//                JsonNode firstUser = root.get(0); // Lấy phần tử đầu tiên của mảng
//                String moodleUserId = firstUser.get("id").asText(); // Lấy user ID
//                return moodleUserId;
//            }
//            // Kiểm tra nếu phản hồi là object JSON (không phải mảng)
//            else if (root.isObject()) {
//                String moodleUserId = root.get("id").asText(); // Lấy user ID từ object
//                return moodleUserId;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null; // Trả về null nếu không thể lấy user ID
//    }

    @PostMapping("/register")
    public String registerSave(@ModelAttribute("user") UserDto userDto, Model model) {
        User user = userService.findByUsername(userDto.getUsername());
        if (user != null) {
            model.addAttribute("userexist", "Username đã tồn tại.");
            return "register";
        }

        // Kiểm tra mật khẩu rỗng
        if (userDto.getPassword() == null || userDto.getPassword().isEmpty()) {
            model.addAttribute("passwordError", "Password không được để trống.");
            return "register";
        }

        // Kiểm tra mật khẩu có chứa ký tự đặc biệt
        if (!userDto.getPassword().matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            model.addAttribute("passwordError", "Password phải chứa ít nhất một ký tự đặc biệt.");
            return "register";
        }

        // Kiểm tra mật khẩu có chứa ít nhất một chữ cái viết hoa
        if (!userDto.getPassword().matches(".*[A-Z].*")) {
            model.addAttribute("passwordError", "Password phải chứa ít nhất một chữ cái viết hoa.");
            return "register";
        }

        // Kiểm tra mật khẩu có chứa ít nhất một chữ cái viết thường
        if (!userDto.getPassword().matches(".*[a-z].*")) {
            model.addAttribute("passwordError", "Password phải chứa ít nhất một chữ cái viết thường.");
            return "register";
        }

        userService.save(userDto);
        String moodleResponse = createMoodleUser(userDto);

        // Kiểm tra phản hồi từ Moodle (giả sử nó là JSON)
        if (moodleResponse != null && moodleResponse.contains("\"id\":")) {
            model.addAttribute("successMessage", "Đăng ký thành công.");
            return "register";
        } else {
            model.addAttribute("errorMessage", "Đăng ký không thành công. Vui lòng thử lại.");
            return "register";
        }
    }

    private String createMoodleUser(UserDto userDto) {
        // Thay thế bằng domain của Moodle của bạn
        String functionName = "core_user_create_users";

        // Xây dựng các tham số cho cuộc gọi API của Moodle
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("users[0][username]", userDto.getUsername());
        parameters.add("users[0][password]", userDto.getPassword());
        parameters.add("users[0][firstname]", userDto.getFirstname());
        parameters.add("users[0][lastname]", userDto.getLastname());
        parameters.add("users[0][email]", userDto.getEmail());
        parameters.add("moodlewsrestformat", "json"); // Đảm bảo định dạng dữ liệu trả về là JSON

        // Tạo header để chỉ định Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Tạo đối tượng HttpEntity để đóng gói các tham số và header
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);

        // Xây dựng URL cho cuộc gọi API của Moodle
        String serverUrl = moodleDomain + "/webservice/rest/server.php?wstoken=" + moodleToken + "&wsfunction=" + functionName;

        // Gọi API của Moodle để tạo tài khoản người dùng
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(serverUrl, request, String.class);

        System.out.println(response);
        System.out.println(serverUrl);
        System.out.println(parameters);
        // Trả về phản hồi từ Moodle
        return response;

    }

}