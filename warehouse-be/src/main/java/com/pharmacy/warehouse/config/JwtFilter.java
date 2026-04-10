package com.pharmacy.warehouse.config;

import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") ||
               path.startsWith("/test/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {

                String username = jwtUtil.getUsername(token);
                User user = userRepository.findByUsername(username).orElse(null);

                if (user != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            jwtUtil.getRoles(token).stream()
                                .map(this::normalizeAuthority)
                                .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList())
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        while (normalized.startsWith("ROLE_ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        return normalized;
    }
}
