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
 *   Body:   { "loginId": "MGR001", "password": "password" }
 *   200 OK: { "token": "<jwt>", "username": "manager1", "loginId": "MGR001", "name": "Manager User", "role": "Manager" }
 *   401:    Invalid credentials
 *   403:    Account deactivated
 */
@RestController
@RequestMapping("/api/auth")
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
        String loginId = req.getLoginIdOrUsername();
        if (loginId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Login ID is required.");
        }

        // 1. Look up by member ID first, then by username for backward compatibility (admin portal)
        User user = userRepository.findByMemberIdIgnoreCase(loginId)
            .or(() -> userRepository.findByUsernameIgnoreCase(loginId))
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid login ID or password."));

        // 2. Check if account is active
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has been deactivated. Please contact your manager.");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid login ID or password.");
        }

        // 4. Issue JWT; return token + login ID + display name + role + isSenior
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.isSenior());
        String responseLoginId = user.getMemberId() != null ? user.getMemberId() : user.getUsername();
        return new AuthResponse(token, user.getUsername(), responseLoginId, user.getFullName(), user.getRole(), user.isSenior());
    }
}
