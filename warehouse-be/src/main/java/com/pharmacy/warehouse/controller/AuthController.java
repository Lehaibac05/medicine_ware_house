package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.config.JwtUtil;
import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.service.PasswordResetService;
import com.pharmacy.warehouse.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for user: {}", request.getUsername());

            var roles = auth.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toList());

            String accessToken = jwtUtil.generateAccessToken(
                    request.getUsername(),
                    roles
            );

            String refreshToken = jwtUtil.generateRefreshToken(request.getUsername());
            LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(7);

            // Cập nhật lastLogin, lưu refresh token và trả về thông tin user
            UserResponse user = userService.recordLoginSuccess(request.getUsername(), refreshToken, refreshExpiry);

            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, user));

        } catch (BadCredentialsException e) {
            log.error("Bad credentials for user: {}", request.getUsername());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(error);

        } catch (DisabledException e) {
            log.error("Account disabled for user: {}", request.getUsername());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Account is disabled");
            return ResponseEntity.status(401).body(error);

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {} - {}", request.getUsername(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        String username = jwtUtil.extractUsernameOrEmail(refreshToken);
        User user = userService.findByUsernameOrEmail(username);

        if (user == null || user.getRefreshToken() == null ||
                !user.getRefreshToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token mismatch"));
        }

        if (user.getRefreshTokenExpiry() == null ||
                user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token expired"));
        }

        var roles = user.getRole() == null
                ? java.util.List.<String>of()
                : java.util.List.of("ROLE_" + user.getRole().getRoleName());

        String newAccessToken = jwtUtil.generateAccessToken(username, roles);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsernameOrEmail(token);
                User user = userService.findByUsernameOrEmail(username);
                if (user != null) {
                    user.setRefreshToken(null);
                    user.setRefreshTokenExpiry(null);
                    userService.save(user);
                }
            }
            return ResponseEntity.ok("Logout successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đăng xuất: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> requestBody,
                                                 HttpServletRequest request) {
        String email = requestBody.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email không được để trống");
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email không tồn tại trong hệ thống");
        }

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        passwordResetService.requestReset(email, ip, userAgent);
        return ResponseEntity.ok("Nếu có tài khoản, chúng tôi đã gửi hướng dẫn đến email.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String otp = requestBody.get("otp");
        try {
            String resetToken = passwordResetService.verifyOtp(email, otp);
            return ResponseEntity.ok(Map.of("reset_token", resetToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String resetToken = requestBody.get("reset_token");
        String newPassword = requestBody.get("newPassword");
        try {
            passwordResetService.resetPassword(email, resetToken, newPassword);
            return ResponseEntity.ok("Mật khẩu đã được thay đổi.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

