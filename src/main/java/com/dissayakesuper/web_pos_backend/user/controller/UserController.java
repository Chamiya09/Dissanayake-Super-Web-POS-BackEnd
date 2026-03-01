package com.dissayakesuper.web_pos_backend.user.controller;

import com.dissayakesuper.web_pos_backend.user.dto.ChangePasswordRequest;
import com.dissayakesuper.web_pos_backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // ── PUT /api/users/change-password ────────────────────────────────────────
    /**
     * Changes the password for an authenticated user.
     * Body: { username, currentPassword, newPassword }
     * Returns 200 OK on success, 401 if currentPassword is wrong.
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    // ── PUT /api/users/deactivate/{username} ──────────────────────────────────
    /**
     * Marks a user account as inactive.
     * The user will be redirected to /login after this is called.
     */
    @PutMapping("/deactivate/{username}")
    public ResponseEntity<Map<String, String>> deactivate(@PathVariable String username) {
        service.deactivate(username);
        return ResponseEntity.ok(Map.of("message", "Account deactivated."));
    }
}
