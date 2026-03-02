package com.pharmacy.warehouse.service;

import com.pharmacy.warehouse.model.User;
import com.pharmacy.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean enabled = user.getStatus() != null && user.getStatus().equalsIgnoreCase("ACTIVE");

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
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName())
                        )
        );
    }
}
