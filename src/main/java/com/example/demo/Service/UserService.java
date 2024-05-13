package com.example.demo.Service;

import com.example.demo.Dto.UserDto;
import com.example.demo.Model.User;

public interface UserService {
    User findByUsername(String username);
    User save(UserDto userDto);
}
