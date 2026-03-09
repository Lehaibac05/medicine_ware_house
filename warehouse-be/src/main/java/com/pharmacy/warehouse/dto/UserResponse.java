package com.pharmacy.warehouse.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String status;
    private LocalDateTime lastLogin;
    private Long roleId;
    private String roleName;
}
