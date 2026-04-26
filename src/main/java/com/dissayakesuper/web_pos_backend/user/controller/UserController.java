package com.dissayakesuper.web_pos_backend.user.controller;

import com.dissayakesuper.web_pos_backend.user.dto.ChangePasswordRequest;
import com.dissayakesuper.web_pos_backend.user.dto.CreateUserRequest;
import com.dissayakesuper.web_pos_backend.user.dto.ProfileUpdateRequest;
import com.dissayakesuper.web_pos_backend.user.dto.UpdateUserRequest;
import com.dissayakesuper.web_pos_backend.user.dto.UserResponse;
import com.dissayakesuper.web_pos_backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // ── GET /api/users ────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.findAll());
    }

    // ── GET /api/users/profile ───────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(service.getProfile(authentication.getName()));
    }

    // ── PUT /api/users/profile ───────────────────────────────────────────────
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(service.updateProfile(authentication.getName(), request));
    }

    // ── POST /api/users ───────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    // ── PUT /api/users/{id} ───────────────────────────────────────────────────
    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────
    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    // ── PUT /api/users/change-password ────────────────────────────────────────
    /**
     * Changes the password for an authenticated user.
     * Body: { username, currentPassword, newPassword }
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    // ── PUT /api/users/deactivate/{username} ──────────────────────────────────
    @PutMapping("/deactivate/{username}")
    public ResponseEntity<Map<String, String>> deactivate(@PathVariable String username) {
        service.deactivate(username);
        return ResponseEntity.ok(Map.of("message", "Account deactivated."));
    }
}

