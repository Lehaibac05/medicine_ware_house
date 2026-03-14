package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.CreateUserRequest;
import com.pharmacy.warehouse.dto.UpdateUserRequest;
import com.pharmacy.warehouse.dto.UserResponse;
import com.pharmacy.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> importUsers(@RequestParam("file") MultipartFile file) {
        return userService.importUsersFromCsv(file);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateUser(@PathVariable Long id,
                                   @RequestBody UpdateUserRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        UserResponse targetUser = userService.getUser(id);

        if (!isAdmin && !targetUser.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("Bạn chỉ được phép sửa thông tin của chính mình");
        }

        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
