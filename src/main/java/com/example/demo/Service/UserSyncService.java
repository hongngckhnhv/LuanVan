//package com.example.demo.Service;
//
//import com.example.demo.Dto.UserDto;
//import com.example.demo.Model.User;
//import com.example.demo.Repository.UserRepo;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.client.HttpClientErrorException;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class UserSyncService {
//
//    @Autowired
//    private UserRepo userRepo;
//
//    @Value("${moodle.token}")
//    private String moodleToken;
//
//    @Value("${moodle.domain}")
//    private String moodleDomain;
//
//    @Scheduled(fixedRate = 300000)
//    public void syncUsers() {
//        System.out.println("Đang thực hiện đồng bộ...");
//        List<UserDto> moodleUsers = fetchUsersFromMoodle(); // Lấy danh sách người dùng từ Moodle
//
//        if (moodleUsers == null || moodleUsers.isEmpty()) {
//            System.out.println("Không có người dùng nào được tìm thấy từ Moodle hoặc có lỗi xảy ra.");
//            return; // Không tiếp tục nếu danh sách người dùng rỗng hoặc null
//        }
//
//        for (UserDto moodleUser : moodleUsers) {
//            // Tìm kiếm người dùng trong hệ thống hiện tại
//            User existingUser = userRepo.findByUsername(moodleUser.getUsername());
//
//            if (existingUser == null) {
//                // Nếu người dùng chưa tồn tại, thêm mới
//                User newUser = new User(moodleUser.getUserId(),
//                        moodleUser.getUsername(),
//                        null, // Có thể thêm mật khẩu mặc định
//                        moodleUser.getEmail(),
//                        moodleUser.getFirstname(),
//                        moodleUser.getLastname());
//                userRepo.save(newUser);
//                System.out.println("Người dùng mới được tạo: " + newUser.getUsername());
//            } else {
//                // Cập nhật thông tin nếu cần
//                updateExistingUser(existingUser, moodleUser);
//            }
//        }
//        System.out.println("Đồng bộ hoàn tất với tổng số người dùng: " + moodleUsers.size());
//    }
//
//    @PostConstruct
//    public void init() {
//        syncUsers(); // Gọi phương thức đồng bộ khi ứng dụng khởi động
//    }
//
//    // Phương thức lấy danh sách người dùng từ Moodle
//    private List<UserDto> fetchUsersFromMoodle() {
//        String functionName = "core_user_get_users";
//        String serverUrl = moodleDomain + "/webservice/rest/server.php?wstoken=" + moodleToken +
//                "&wsfunction=" + functionName + "&moodlewsrestformat=json";
//
//        RestTemplate restTemplate = new RestTemplate();
//        String response;
//        try {
//            response = restTemplate.getForObject(serverUrl, String.class);
//        } catch (HttpClientErrorException e) {
//            System.out.println("Lỗi HTTP khi kết nối tới Moodle: " + e.getMessage());
//            return null; // Trả về null nếu có lỗi kết nối HTTP
//        } catch (Exception e) {
//            System.out.println("Lỗi khi kết nối tới Moodle: " + e.getMessage());
//            return null; // Trả về null nếu gặp lỗi chung
//        }
//
//        List<UserDto> moodleUsers = new ArrayList<>();
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode root = objectMapper.readTree(response);
//
//            // Kiểm tra xem JSON trả về có lỗi không
//            if (root.has("exception")) {
//                System.out.println("Lỗi từ Moodle: " + root.get("exception").asText());
//                return null;
//            }
//
//            if (root.isArray()) {
//                for (JsonNode userNode : root) {
//                    UserDto userDto = new UserDto();
//                    userDto.setUserId(userNode.get("id").asInt());
//                    userDto.setUsername(userNode.get("username").asText());
//                    userDto.setEmail(userNode.get("email").asText());
//                    userDto.setFirstname(userNode.get("firstname").asText());
//                    userDto.setLastname(userNode.get("lastname").asText());
//                    moodleUsers.add(userDto);
//                }
//            }
//        } catch (Exception e) {
//            System.out.println("Lỗi khi phân tích danh sách người dùng từ Moodle: " + e.getMessage());
//        }
//        return moodleUsers;
//    }
//
//    // Phương thức để cập nhật thông tin người dùng đã tồn tại
//    private void updateExistingUser(User existingUser, UserDto moodleUser) {
//        boolean needsUpdate = false;
//
//        if (!moodleUser.getEmail().equals(existingUser.getEmail())) {
//            existingUser.setEmail(moodleUser.getEmail());
//            needsUpdate = true;
//        }
//
//        if (!moodleUser.getFirstname().equals(existingUser.getFirstname())) {
//            existingUser.setFirstname(moodleUser.getFirstname());
//            needsUpdate = true;
//        }
//
//        if (!moodleUser.getLastname().equals(existingUser.getLastname())) {
//            existingUser.setLastname(moodleUser.getLastname());
//            needsUpdate = true;
//        }
//
//        if (needsUpdate) {
//            userRepo.save(existingUser);
//            System.out.println("Người dùng đã được cập nhật: " + existingUser.getUsername());
//        }
//    }
//
//    // Phương thức lấy thông tin người dùng từ Moodle theo username
//    private UserDto fetchUserFromMoodleByUsername(String username) {
//        String functionName = "core_user_get_users";
//        String serverUrl = moodleDomain + "/webservice/rest/server.php?wstoken=" + moodleToken
//                + "&wsfunction=" + functionName + "&moodlewsrestformat=json"
//                + "&criteria[0][key]=username&criteria[0][value]=" + username;
//
//        RestTemplate restTemplate = new RestTemplate();
//        String response;
//        try {
//            response = restTemplate.getForObject(serverUrl, String.class);
//        } catch (Exception e) {
//            System.out.println("Lỗi khi kết nối tới Moodle: " + e.getMessage());
//            return null;
//        }
//
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode root = objectMapper.readTree(response);
//
//            // Kiểm tra xem JSON trả về có lỗi không
//            if (root.has("exception")) {
//                System.out.println("Lỗi từ Moodle: " + root.get("exception").asText());
//                return null;
//            }
//
//            if (root.isArray() && root.size() > 0) {
//                JsonNode userNode = root.get(0); // Lấy người dùng đầu tiên nếu tồn tại
//                UserDto userDto = new UserDto();
//                userDto.setUserId(userNode.get("id").asInt());
//                userDto.setUsername(userNode.get("username").asText());
//                userDto.setEmail(userNode.get("email").asText());
//                userDto.setFirstname(userNode.get("firstname").asText());
//                userDto.setLastname(userNode.get("lastname").asText());
//                return userDto;
//            }
//        } catch (Exception e) {
//            System.out.println("Lỗi khi lấy thông tin người dùng theo username: " + e.getMessage());
//        }
//        return null; // Trả về null nếu không tìm thấy người dùng
//    }
//
//    // Phương thức đồng bộ người dùng theo username
//    public void syncUserByUsername(String username) {
//        UserDto moodleUser = fetchUserFromMoodleByUsername(username); // Lấy người dùng từ Moodle theo username
//
//        if (moodleUser != null) {
//            User existingUser = userRepo.findByUsername(moodleUser.getUsername());
//
//            if (existingUser == null) {
//                // Nếu người dùng chưa tồn tại, thêm mới
//                User newUser = new User(moodleUser.getUserId(),
//                        moodleUser.getUsername(),
//                        null, // Có thể thêm mật khẩu mặc định
//                        moodleUser.getEmail(),
//                        moodleUser.getFirstname(),
//                        moodleUser.getLastname());
//                userRepo.save(newUser);
//                System.out.println("Người dùng mới được tạo: " + newUser.getUsername());
//            } else {
//                // Cập nhật thông tin nếu cần
//                updateExistingUser(existingUser, moodleUser);
//            }
//        } else {
//            System.out.println("Không tìm thấy người dùng với username: " + username);
//        }
//    }
//}
