package com.dissayakesuper.web_pos_backend.auth.controller;

import com.dissayakesuper.web_pos_backend.auth.dto.AuthResponse;
import com.dissayakesuper.web_pos_backend.auth.dto.LoginRequest;
import com.dissayakesuper.web_pos_backend.security.JwtUtils;
import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public authentication endpoint — no JWT required.
 *
 * POST /api/auth/login
 *   Body:   { "username": "admin", "password": "password" }
 *   200 OK: { "token": "<jwt>", "username": "Admin User", "role": "Owner" }
 *   401:    Invalid credentials
 *   403:    Account deactivated
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtils              jwtUtils;
    private final UserRepository        userRepository;

    public AuthController(AuthenticationManager authManager,
                          JwtUtils              jwtUtils,
                          UserRepository        userRepository) {
        this.authManager    = authManager;
        this.jwtUtils       = jwtUtils;
        this.userRepository = userRepository;
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {

        try {
            // 1. Let Spring Security verify credentials (BCrypt check via DaoAuthenticationProvider)
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has been deactivated. Please contact your manager.");
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid username or password.");
        }

        // 2. Load full User entity to read fullName and role
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "User record missing after authentication."));

        // 3. Issue JWT; return token + login username + display name + role to the frontend
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

        return new AuthResponse(token, user.getUsername(), user.getFullName(), user.getRole());
    }
}
