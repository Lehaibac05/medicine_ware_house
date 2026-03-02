package com.pharmacy.warehouse.config;

<<<<<<< HEAD
=======
import com.pharmacy.warehouse.config.JwtFilter;
>>>>>>> 1cd68f6a43bf2c1f3c2c1a8b62f0d2a30f3990a5
import lombok.RequiredArgsConstructor;
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
<<<<<<< HEAD
import org.springframework.web.cors.CorsConfigurationSource;
=======
>>>>>>> 1cd68f6a43bf2c1f3c2c1a8b62f0d2a30f3990a5

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
<<<<<<< HEAD
    private final CorsConfigurationSource corsConfigurationSource;
=======
>>>>>>> 1cd68f6a43bf2c1f3c2c1a8b62f0d2a30f3990a5

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
                .requestMatchers("/alerts/scan").permitAll()  // Temporary for testing
                .requestMatchers("/error").permitAll()

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
