package com.pharmacy.warehouse.controller;

import com.pharmacy.warehouse.dto.*;
import com.pharmacy.warehouse.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var roles = auth.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(
                request.getUsername(),
                roles
        );

        return new LoginResponse(token);
    }
}
