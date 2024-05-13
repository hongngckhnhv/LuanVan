package com.example.demo.Service;

import com.example.demo.Model.User;
import com.example.demo.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;

//kiem tra trung email
@Service
public class CustomerUserDetailsServices implements UserDetailsService {
    @Autowired
    private UserRepo userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username);
        if(user == null) {
            throw new UsernameNotFoundException("Username or Password not found");

        }
        return new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                authorities(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname()
        );
    }
    public Collection<? extends GrantedAuthority> authorities(){
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }
}
