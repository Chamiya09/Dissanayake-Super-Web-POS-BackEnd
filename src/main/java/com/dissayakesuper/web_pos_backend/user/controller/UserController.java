package com.dissayakesuper.web_pos_backend.user.controller;

import com.dissayakesuper.web_pos_backend.user.dto.ChangePasswordRequest;
import com.dissayakesuper.web_pos_backend.user.dto.CreateUserRequest;
import com.dissayakesuper.web_pos_backend.user.dto.UpdateUserRequest;
import com.dissayakesuper.web_pos_backend.user.dto.UserResponse;
import com.dissayakesuper.web_pos_backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // ── POST /api/users ───────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    // ── PUT /api/users/{id} ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────
    @DeleteMapping("/{id}")
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

