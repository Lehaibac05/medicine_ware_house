package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.dto.CreateUserRequest;
import com.pharmacy.warehouse.dto.UpdateUserRequest;
import com.pharmacy.warehouse.dto.UserResponse;
import com.pharmacy.warehouse.exception.ResourceNotFoundException;
import com.pharmacy.warehouse.model.Role;
import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.RoleRepository;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_PASSWORD = "12345@";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserResponse> getUsers(int page,
                                       int size,
                                       String search,
                                       String sortBy,
                                       String sortDir) {

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0 ? 10 : Math.min(size, 100);

        String sortableField = switch (sortBy) {
            case "email" -> "email";
            case "fullName" -> "fullName";
            case "status" -> "status";
            default -> "username";
        };

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(direction, sortableField)
        );

        if (StringUtils.hasText(search)) {
            Page<User> userPage = userRepository
                    .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                            search.trim(),
                            search.trim(),
                            pageable
                    );
            return userPage.map(this::toResponse);
        }

        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());

        // Luôn luôn dùng mật khẩu mặc định khi tạo user mới
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));
            user.setRole(role);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));
            user.setRole(role);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        User byUsername = userRepository.findByUsername(usernameOrEmail).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }
        return userRepository.findByEmail(usernameOrEmail).orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Import nhiều user từ file CSV với các cột:
     * email,username,full_name,status,role_id (header có thể đổi thứ tự, nhưng tên cột phải khớp).
     */
    public List<UserResponse> importUsersFromCsv(MultipartFile file) {
        List<UserResponse> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> indexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String name = headers[i].trim().replace("\"", "").toLowerCase();
                indexMap.put(name, i);
            }

            // Validate các cột bắt buộc
            String[] requiredCols = {"email", "username", "full_name", "status", "role_id"};
            for (String col : requiredCols) {
                if (!indexMap.containsKey(col)) {
                    throw new RuntimeException("CSV is missing required column: " + col);
                }
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                String[] cols = line.split(",");

                String email = getCol(cols, indexMap, "email");
                String username = getCol(cols, indexMap, "username");
                String fullName = getCol(cols, indexMap, "full_name");
                String status = getCol(cols, indexMap, "status");
                String roleIdStr = getCol(cols, indexMap, "role_id");

                CreateUserRequest req = new CreateUserRequest();
                req.setEmail(email);
                req.setUsername(username);
                req.setFullName(fullName);
                req.setStatus(status);
                req.setPassword(null); // dùng mật khẩu mặc định

                if (StringUtils.hasText(roleIdStr)) {
                    try {
                        req.setRoleId(Long.parseLong(roleIdStr));
                    } catch (NumberFormatException ex) {
                        throw new RuntimeException("Invalid role_id value: " + roleIdStr);
                    }
                } else {
                    throw new RuntimeException("role_id is required for each row");
                }

                result.add(createUser(req));
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to import users from CSV", e);
        }
    }

    private String getCol(String[] cols, Map<String, Integer> indexMap, String key) {
        Integer idx = indexMap.get(key);
        if (idx == null || idx < 0 || idx >= cols.length) {
            return null;
        }
        return cols[idx].trim().replace("\"", "");
    }

    /**
     * Ghi nhận lần đăng nhập gần nhất, lưu refresh token và trả về thông tin user.
     */
    public UserResponse recordLoginSuccess(String username, String refreshToken, LocalDateTime refreshExpiry) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        user.setLastLogin(LocalDateTime.now());
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(refreshExpiry);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        Long roleId = user.getRole() != null ? user.getRole().getRoleId() : null;
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getStatus(),
                user.getLastLogin(),
                roleId,
                roleName
        );
    }
}

