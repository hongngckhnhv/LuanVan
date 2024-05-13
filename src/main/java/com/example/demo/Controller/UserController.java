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

    @GetMapping("/home")
    public String home(Model model, Principal principal) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getName());
        model.addAttribute("userdetail", userDetails);
        return "home";
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
    public String registerSave(@ModelAttribute("user")UserDto userDto, Model model) {
        User user = userService.findByUsername(userDto.getUsername()); //kiem tra trung username
        if(user != null) {
            model.addAttribute("userexist", user);
            return "username da ton tai";
        }
        userService.save(userDto);
        String moodleResponse = createMoodleUser(userDto);
        // Xử lý phản hồi từ Moodle
        if (moodleResponse != null && moodleResponse.contains("success")) {
            // Trả về trang thông báo tạo tài khoản thành công
            return "redirect:/register?success";
        } else {
            // Trả về trang thông báo lỗi khi tạo tài khoản trong Moodle
            return "redirect:/register?success";
        }
    }

//    @PostMapping("/register")
//    public String registerSave(@ModelAttribute("user") UserDto userDto, Model model) {
//        User user = userService.findByUsername(userDto.getUsername()); // Kiểm tra trùng username
//        if (user != null) {
//            model.addAttribute("userexist", user);
//            return "username da ton tai";
//        }
//
//        userService.save(userDto);
//        String moodleResponse = createMoodleUser(userDto);
//
//        // Kiểm tra mã trạng thái HTTP của phản hồi từ Moodle
//        if (moodleResponse != null && moodleResponse.equals("200")) {
//            // Phản hồi thành công từ Moodle, xử lý phản hồi chi tiết để xác định tạo tài khoản thành công hay không
//            if (moodleResponse.contains("success")) {
//                // Trả về trang thông báo tạo tài khoản thành công
//                return "redirect:/register?success=true";
//            } else {
//                // Trả về trang thông báo lỗi khi tạo tài khoản trong Moodle
//                return "redirect:/register?error=true";
//            }
//        } else {
//            // Lỗi khi gửi yêu cầu tạo tài khoản đến Moodle, có thể xử lý theo ý của bạn, ví dụ hiển thị thông báo lỗi cho người dùng
//            return "redirect:/register?error=true";
//        }
//    }


    private String createMoodleUser(UserDto userDto) {
        String token = "388cdd1555fc753635b67bbba524ef87"; // Thay thế bằng token của Moodle của bạn
        String domainName = "http://localhost/demo.ngockhanh.vn"; // Thay thế bằng domain của Moodle của bạn
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
        // Trả về phản hồi từ Moodle
        return response;

    }

//    @GetMapping("/courses_list")
//    public String showSource() {
//        return "course_list";
//    }

}
