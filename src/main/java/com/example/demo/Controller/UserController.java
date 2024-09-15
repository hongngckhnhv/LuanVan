package com.example.demo.Controller;

import com.example.demo.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.ui.Model;
import com.example.demo.Dto.UserDto;
import com.example.demo.Service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

@Controller
public class UserController {
    @Autowired
    private UserDetailsService userDetailsService;
    private UserService userService;
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
        String token = "2f8b6d0d241565fd8731dcabcf342e3e"; // Thay thế bằng token của Moodle của bạn
        String domainName = "http://localhost/demo.hoangngockhanh.vn"; // Thay thế bằng domain của Moodle của bạn
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
        String serverUrl = domainName + "/webservice/rest/server.php" + "?wstoken=" + token + "&wsfunction=" + functionName;

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
