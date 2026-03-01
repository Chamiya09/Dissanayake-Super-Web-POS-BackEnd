package com.dissayakesuper.web_pos_backend.auth.controller;

import com.dissayakesuper.web_pos_backend.auth.dto.AuthResponse;
import com.dissayakesuper.web_pos_backend.auth.dto.LoginRequest;
import com.dissayakesuper.web_pos_backend.security.JwtUtils;
import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public authentication endpoint — no JWT required.
 *
 * POST /api/auth/login
 *   Body:   { "username": "admin", "password": "password" }
 *   200 OK: { "token": "<jwt>", "username": "admin", "name": "Admin User", "role": "Owner" }
 *   401:    Invalid credentials
 *   403:    Account deactivated
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    private final UserRepository        userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils              jwtUtils;

    public AuthController(UserRepository        userRepository,
                          BCryptPasswordEncoder  passwordEncoder,
                          JwtUtils              jwtUtils) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils        = jwtUtils;
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {

        // 1. Look up the user
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password."));

        // 2. Check if account is active
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has been deactivated. Please contact your manager.");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid username or password.");
        }

        // 4. Issue JWT; return token + login username + display name + role
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getFullName(), user.getRole());
    }
}
