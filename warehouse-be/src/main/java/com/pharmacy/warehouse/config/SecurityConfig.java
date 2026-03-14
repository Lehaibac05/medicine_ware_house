package com.pharmacy.warehouse.config;

import lombok.RequiredArgsConstructor;

import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/alerts/**").permitAll()  // Temporary for testing
                .requestMatchers("/error").permitAll()

                // Supplier Management - ADMIN and WAREHOUSE_MANAGER
                .requestMatchers("/suppliers/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                // Purchase Order Management
                .requestMatchers("/purchase-orders/*/confirm").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                .requestMatchers("/purchase-orders/*/cancel").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                .requestMatchers("/purchase-orders/*/status").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                .requestMatchers("/purchase-orders").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF")

                // Goods Receipt Management
                .requestMatchers("/goods-receipts/*/approve").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                .requestMatchers("/goods-receipts").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF")

                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/warehouse/manager/**").hasRole("WAREHOUSE_MANAGER")
                .requestMatchers("/warehouse/staff/**").hasRole("WAREHOUSE_STAFF")
                .requestMatchers("/accountant/**").hasRole("ACCOUNTANT")
                .requestMatchers("/supplier/**").hasRole("SUPPLIER")

                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
