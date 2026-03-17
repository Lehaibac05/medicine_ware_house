package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.info("Loading user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        log.info("User found: {}, status: {}, role: {}", 
                username, 
                user.getStatus(), 
                user.getRole() != null ? user.getRole().getRoleName() : "null");
        
        log.debug("Password hash from DB: {}", user.getPasswordHash());

        boolean enabled = user.getStatus() != null && user.getStatus().equalsIgnoreCase("ACTIVE");
        
        if (!enabled) {
            log.warn("User {} is not active. Status: {}", username, user.getStatus());
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                true,
                true,
                true,
                user.getRole() == null
                        ? java.util.Collections.emptySet()
                        : java.util.Collections.singleton(
                                                                new SimpleGrantedAuthority(normalizeAuthority(user.getRole().getRoleName()))
                        )
        );
    }

        private String normalizeAuthority(String roleName) {
                if (roleName == null || roleName.isBlank()) {
                        return "ROLE_USER";
                }

                String trimmed = roleName.trim();
                return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
        }
}
