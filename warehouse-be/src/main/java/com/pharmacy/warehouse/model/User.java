package com.pharmacy.warehouse.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String status;

    private LocalDateTime lastLogin;

    // Refresh token for issuing new access tokens
    private String refreshToken;
    private LocalDateTime refreshTokenExpiry;

    // Fields for password reset flow
    private String resetOtp;
    private LocalDateTime resetOtpExpiry;
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    // Force password change on first login
    private Boolean forceChangePassword = false;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    public void login() {
    }

    public void logout() {
    }

    public void changePassword(String newPassword) {
    }
}
