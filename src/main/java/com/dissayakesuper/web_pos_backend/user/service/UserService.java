package com.dissayakesuper.web_pos_backend.user.service;

import com.dissayakesuper.web_pos_backend.user.dto.ChangePasswordRequest;
import com.dissayakesuper.web_pos_backend.user.dto.CreateUserRequest;
import com.dissayakesuper.web_pos_backend.user.dto.UpdateUserRequest;
import com.dissayakesuper.web_pos_backend.user.dto.UserResponse;
import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository        repo;
    private final BCryptPasswordEncoder encoder;

    /** Encoder bean is provided by {@link com.dissayakesuper.web_pos_backend.config.SecurityConfig}. */
    public UserService(UserRepository repo, BCryptPasswordEncoder encoder) {
        this.repo    = repo;
        this.encoder = encoder;
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse(u.getId(), u.getUsername(), u.getFullName(),
                                          u.getEmail(), u.getRole(), u.isActive());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }

    // ── List all users ────────────────────────────────────────────────────────
    public List<UserResponse> findAll() {
        return repo.findAll().stream()
                   .map(this::toResponse)
                   .collect(Collectors.toList());
    }

    // ── Create user ───────────────────────────────────────────────────────────
    public UserResponse create(CreateUserRequest req) {
        if (repo.findByUsername(req.getUsername().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username '" + req.getUsername() + "' is already taken.");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().trim());
        user.setRole(req.getRole());
        user.setPasswordHash(encoder.encode(req.getPassword()));
        user.setActive(true);

        return toResponse(repo.save(user));
    }

    // ── Update user ───────────────────────────────────────────────────────────
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found."));

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "The admin account cannot be modified.");
        }

        // If username is being changed, check for conflicts
        String newUsername = req.getUsername() != null ? req.getUsername().trim() : user.getUsername();
        if (!newUsername.equals(user.getUsername())) {
            repo.findByUsername(newUsername).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Username '" + newUsername + "' is already taken.");
            });
            user.setUsername(newUsername);
        }

        if (req.getFullName() != null) user.setFullName(req.getFullName().trim());
        if (req.getEmail()    != null) user.setEmail(req.getEmail().trim());
        if (req.getRole()     != null) user.setRole(req.getRole());

        return toResponse(repo.save(user));
    }

    // ── Delete user ───────────────────────────────────────────────────────────
    public void delete(Long id) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found."));

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "The admin account cannot be deleted.");
        }

        repo.deleteById(id);
    }

    // ── Change password ───────────────────────────────────────────────────────
    public void changePassword(ChangePasswordRequest req) {
        User user = repo.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated.");
        }

        if (!encoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }

        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must differ from the current password.");
        }

        user.setPasswordHash(encoder.encode(req.getNewPassword()));
        repo.save(user);
    }

    // ── Deactivate account ────────────────────────────────────────────────────
    public void deactivate(String username) {
        User user = repo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found."));

        user.setActive(false);
        repo.save(user);
    }
}
