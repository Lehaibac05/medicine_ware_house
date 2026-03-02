package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @GetMapping("/hash")
    public Map<String, String> generateHash(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        return result;
    }

    @GetMapping("/verify")
    public Map<String, Object> verifyPassword(
            @RequestParam String password,
            @RequestParam String hash) {
        boolean matches = passwordEncoder.matches(password, hash);
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", matches);
        return result;
    }
    
    @GetMapping("/user/{username}")
    public Map<String, Object> getUserInfo(@PathVariable String username) {
        Map<String, Object> result = new HashMap<>();
        
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            result.put("found", false);
            result.put("message", "User not found");
            return result;
        }
        
        result.put("found", true);
        result.put("id", user.getUserId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("fullName", user.getFullName());
        result.put("status", user.getStatus());
        result.put("passwordHash", user.getPasswordHash());
        result.put("roleId", user.getRole() != null ? user.getRole().getRoleId() : null);
        result.put("roleName", user.getRole() != null ? user.getRole().getRoleName() : null);
        
        return result;
    }
    
    @GetMapping("/login-test")
    public Map<String, Object> testLogin(
            @RequestParam String username,
            @RequestParam String password) {
        
        Map<String, Object> result = new HashMap<>();
        
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user == null) {
            result.put("success", false);
            result.put("error", "User not found");
            return result;
        }
        
        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        boolean isActive = "ACTIVE".equalsIgnoreCase(user.getStatus());
        
        result.put("success", passwordMatches && isActive);
        result.put("userFound", true);
        result.put("passwordMatches", passwordMatches);
        result.put("isActive", isActive);
        result.put("status", user.getStatus());
        result.put("hasRole", user.getRole() != null);
        result.put("roleName", user.getRole() != null ? user.getRole().getRoleName() : null);
        
        return result;
    }
}
