package com.pharmacy.warehouse.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ===== PUBLIC =====
                        .requestMatchers("/auth/**", "/test/**", "/error").permitAll()

                        // ===== SHARED ENDPOINTS =====
                        // Medicines - ADMIN, WAREHOUSE_MANAGER, WAREHOUSE_STAFF, ACCOUNTANT (view)
                        .requestMatchers(HttpMethod.GET, "/medicines/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/medicines/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/medicines/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/medicines/**").hasRole("ADMIN")

                        // Warehouses - ADMIN, WAREHOUSE_MANAGER, WAREHOUSE_STAFF (view)
                        .requestMatchers(HttpMethod.GET, "/warehouses/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/warehouses/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/warehouses/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/warehouses/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Suppliers - ADMIN, WAREHOUSE_MANAGER (view and update)
                        .requestMatchers(HttpMethod.GET, "/suppliers/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/suppliers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/suppliers/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/suppliers/**").hasRole("ADMIN")

                        // Batches - ADMIN, WAREHOUSE_MANAGER, WAREHOUSE_STAFF, ACCOUNTANT (view), WAREHOUSE_STAFF (create)
                        .requestMatchers(HttpMethod.GET, "/batches/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/medicines/*/batches").hasAnyRole("ADMIN", "WAREHOUSE_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/medicines/*/batches/*").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/batches/**").hasRole("ADMIN")

                        // Purchase Orders - ADMIN, WAREHOUSE_MANAGER, WAREHOUSE_STAFF (create/view)
                        .requestMatchers(HttpMethod.GET, "/purchase-orders/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/purchase-orders").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/purchase-orders/*").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/purchase-orders/*/confirm").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/purchase-orders/*/cancel").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/purchase-orders/*/status").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/purchase-orders/*/pdf").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/purchase-orders/*/send-email").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Goods Receipts - ADMIN, WAREHOUSE_MANAGER (approve), WAREHOUSE_STAFF (create/view)
                        .requestMatchers(HttpMethod.GET, "/goods-receipts/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/goods-receipts").hasAnyRole("ADMIN", "WAREHOUSE_STAFF")
                        .requestMatchers("/goods-receipts/*/approve").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Medicine Requests - ADMIN, WAREHOUSE_MANAGER (approve), WAREHOUSE_STAFF (create/view)
                        .requestMatchers(HttpMethod.GET, "/medicine-requests/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/medicine-requests").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "REQUESTER")
                        .requestMatchers("/medicine-requests/*/approve").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/medicine-requests/*/reject").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Issue Requests - ADMIN, WAREHOUSE_MANAGER (approve/reject), WAREHOUSE_STAFF (create), ACCOUNTANT (view)
                        .requestMatchers(HttpMethod.GET, "/issue-requests/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")
                        .requestMatchers(HttpMethod.POST, "/issue-requests").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")
                        .requestMatchers("/issue-requests/*/approve").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/issue-requests/*/reject").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Issues - ADMIN (execute), WAREHOUSE_STAFF (execute), others (view history/insight)
                        .requestMatchers(HttpMethod.POST, "/issues/execute/*").hasAnyRole("ADMIN", "WAREHOUSE_STAFF")
                        .requestMatchers(HttpMethod.GET, "/issues/history").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")
                        .requestMatchers(HttpMethod.GET, "/issues/stock-insight").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")

                        // Supplier Invoices - ACCOUNTANT (full), ADMIN (full), WAREHOUSE_MANAGER/ACCOUNTANT (view)
                        .requestMatchers(HttpMethod.GET, "/supplier-invoices/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/supplier-invoices").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers("/supplier-invoices/*/verify").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers("/supplier-invoices/*/reject").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers("/supplier-invoices/*/pay").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers("/supplier-invoices/*/pay/confirm").hasAnyRole("ADMIN", "ACCOUNTANT")

                        // Inventory - ADMIN, WAREHOUSE_MANAGER, WAREHOUSE_STAFF, ACCOUNTANT
                        .requestMatchers("/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT", "REQUESTER")

                        // Reports - ADMIN (all), WAREHOUSE_MANAGER (inventory/issue/dashboard/financial view), WAREHOUSE_STAFF (inventory/issue), ACCOUNTANT (financial/inventory/issue/dashboard view)
                        .requestMatchers("/reports/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT")
                        .requestMatchers("/reports/issue/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT")
                        .requestMatchers("/reports/dashboard/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.GET, "/reports/financial/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/reports/**").hasRole("ADMIN")

                        // Alerts - ADMIN (full), WAREHOUSE_MANAGER (view/resolve/update status/scan), other operational staff/accountant (view)
                        .requestMatchers(HttpMethod.GET, "/alerts/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "WAREHOUSE_STAFF", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/alerts/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/alerts/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // System Configurations - ADMIN only
                        .requestMatchers("/system-configurations/**").hasRole("ADMIN")

                        // Users - ADMIN can manage all, users can update their own info
                        .requestMatchers(HttpMethod.GET, "/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/change-password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/force-change-password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
