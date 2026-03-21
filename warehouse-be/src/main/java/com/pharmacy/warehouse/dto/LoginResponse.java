package com.pharmacy.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {


    private String token;

    private String refreshToken;

    private UserResponse user;

    private boolean forceChangePassword;
}


