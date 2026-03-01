package com.dissayakesuper.web_pos_backend.config;

import com.dissayakesuper.web_pos_backend.security.JwtAuthFilter;
import com.dissayakesuper.web_pos_backend.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Production-ready Spring Security configuration.
 *
 * Strategy:
 *   • Stateless JWT authentication (no HTTP sessions).
 *   • /api/auth/** is fully public (login endpoint).
 *   • All other /api/** routes require a valid Bearer token.
 *   • CORS is configured to allow requests from the React dev server.
 */
@Configuration
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter          jwtAuthFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthFilter          jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter      = jwtAuthFilter;
    }

    // ── Beans ─────────────────────────────────────────────────────────────────

    /** Single shared BCrypt encoder — injected everywhere (UserService, etc.). */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Wires our UserDetailsService + BCrypt encoder into Spring's auth pipeline. */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Exposes the AuthenticationManager so AuthController can call authenticate(). */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Security filter chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Global CORS — must be in Security layer so preflight OPTIONS passes
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // No CSRF needed for a stateless REST API
            .csrf(csrf -> csrf.disable())

            // No server-side HTTP sessions — every request is authenticated via JWT
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Use our DAO provider (BCrypt + UserDetailsService)
            .authenticationProvider(authenticationProvider())

            // JWT filter runs before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                    // Public: login
                    .requestMatchers("/api/auth/**").permitAll()
                    // Public: CORS pre-flight
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Everything else requires authentication
                    .anyRequest().authenticated()
            );

        return http.build();
    }

    // ── CORS ──────────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000"    // CRA / alternative port
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
