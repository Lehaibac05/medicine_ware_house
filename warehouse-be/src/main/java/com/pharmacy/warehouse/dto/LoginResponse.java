package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * Access token (JWT) cho các request bảo vệ.
     */
    private String token;

    /**
     * Refresh token để xin access token mới.
     */
    private String refreshToken;

    private UserResponse user;
}


