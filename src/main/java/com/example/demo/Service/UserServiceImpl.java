package com.example.demo.Service;

import com.example.demo.Dto.UserDto;
import com.example.demo.Model.User;
import com.example.demo.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    private UserRepo userRepo;

    public UserServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    public User save(UserDto userDto) {
        User user = new User(userDto.getUserId(), userDto.getUsername(), passwordEncoder.encode(userDto.getPassword()), userDto.getEmail(), userDto.getFirstname(), userDto.getLastname());
        return userRepo.save(user);
    }

//    @Override
//    public void delete(int id) {
//        userRepo.deleteById(id); // Xóa người dùng theo ID
//    }
//
//    @Override
//    public User update(User user) {
//        // Có thể mã hóa lại mật khẩu nếu cần thiết
//        if (user.getPassword() != null) {
//            user.setPassword(passwordEncoder.encode(user.getPassword()));
//        }
//        return userRepo.save(user); // Cập nhật thông tin người dùng
//    }
}
