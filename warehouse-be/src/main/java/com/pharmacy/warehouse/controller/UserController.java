package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.ChangePasswordRequest;
import com.pharmacy.warehouse.dto.BulkUserImportResponse;
import com.pharmacy.warehouse.dto.CreateUserRequest;
import com.pharmacy.warehouse.dto.ForceChangePasswordRequest;
import com.pharmacy.warehouse.dto.UpdateUserRequest;
import com.pharmacy.warehouse.dto.UserResponse;
import com.pharmacy.warehouse.service.UserActivityLogService;
import com.pharmacy.warehouse.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserActivityLogService userActivityLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return userService.getUsers(page, size, search, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@RequestBody CreateUserRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        UserResponse created = userService.createUser(request);
        userActivityLogService.log(
                authentication != null ? authentication.getName() : "system",
                "CREATE_USER",
                "users/" + created.getUserId(),
                httpRequest.getRemoteAddr());
        return created;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public BulkUserImportResponse importUsersPreview(@RequestParam("file") MultipartFile file) {
        return userService.previewBulkUsersFromCsv(file);
    }

    @PostMapping("/import/commit")
    @PreAuthorize("hasRole('ADMIN')")
    public BulkUserImportResponse importUsersCommit(@RequestParam("file") MultipartFile file, Authentication authentication, HttpServletRequest httpRequest) {
        BulkUserImportResponse result = userService.executeBulkUsersFromCsv(file);
        userActivityLogService.log(
                authentication != null ? authentication.getName() : "system",
                "IMPORT_USERS_COMMIT",
                "users/import/created=" + result.getCreatedRows(),
                httpRequest.getRemoteAddr());
        return result;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public UserResponse updateUser(@PathVariable Long id,
                                   @RequestBody UpdateUserRequest request,
                                   HttpServletRequest httpRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        UserResponse targetUser = userService.getUser(id);

        if (!isAdmin && !targetUser.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Bạn chỉ được phép sửa thông tin của chính mình");
        }

        UserResponse updated = userService.updateUser(id, request);
        userActivityLogService.log(
            currentUsername,
            "UPDATE_USER",
            "users/" + id,
            httpRequest.getRemoteAddr());
        return updated;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        userService.changePassword(username, request);
        
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @PostMapping("/force-change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> forceChangePassword(@RequestBody ForceChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        userService.forceChangePassword(username, request);
        
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}
