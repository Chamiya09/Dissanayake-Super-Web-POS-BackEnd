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
import java.util.Locale;
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
    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeRole(String role) {
        String normalized = normalize(role);
        if (normalized.equalsIgnoreCase("Owner")) return "Owner";
        if (normalized.equalsIgnoreCase("Manager")) return "Manager";
        if (normalized.equalsIgnoreCase("Staff")) return "Staff";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Role must be Owner, Manager, or Staff.");
    }

    private static boolean requiresMemberId(String role) {
        return "Manager".equals(role) || "Staff".equals(role);
    }

    private static String normalizeMemberId(String memberId) {
        return normalize(memberId).toUpperCase(Locale.ROOT);
    }

    private static void validateMemberIdFormat(String role, String memberId) {
        if ("Manager".equals(role) && !memberId.matches("^MGR[0-9]{3,}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manager ID must follow format MGR### (e.g., MGR001).");
        }
        if ("Staff".equals(role) && !memberId.matches("^STF[0-9]{3,}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Staff ID must follow format STF### (e.g., STF001).");
        }
    }

    private static String defaultUsernameFromMemberId(String memberId) {
        return memberId.toLowerCase(Locale.ROOT);
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        repo.findByUsername(username).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Username '" + username + "' is already taken.");
            }
        });
    }

    private void ensureMemberIdUnique(String memberId, Long excludeId) {
        repo.findByMemberId(memberId).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Member ID '" + memberId + "' is already assigned.");
            }
        });
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse(u.getId(), u.getUsername(), u.getMemberId(), u.getFullName(),
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
        String fullName = normalize(req.getFullName());
        String email = normalize(req.getEmail());
        String role = normalizeRole(req.getRole());
        String password = normalize(req.getPassword());
        String username = normalize(req.getUsername());
        String memberId = normalizeMemberId(req.getMemberId());

        if (fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required.");
        }
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required.");
        }
        if (password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required.");
        }

        if (requiresMemberId(role)) {
            if (memberId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Member ID is required for Manager and Staff accounts.");
            }
            validateMemberIdFormat(role, memberId);
            ensureMemberIdUnique(memberId, null);
        } else {
            memberId = "";
        }

        if (username.isBlank()) {
            if (memberId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Username is required for this role.");
            }
            username = defaultUsernameFromMemberId(memberId);
        }
        ensureUsernameUnique(username, null);

        User user = new User();
        user.setUsername(username);
        user.setMemberId(memberId.isBlank() ? null : memberId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(encoder.encode(password));
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

        String newRole = req.getRole() != null ? normalizeRole(req.getRole()) : user.getRole();
        String requestedMemberId = req.getMemberId() != null
                ? normalizeMemberId(req.getMemberId())
                : (user.getMemberId() == null ? "" : user.getMemberId());

        if (requiresMemberId(newRole)) {
            if (requestedMemberId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Member ID is required for Manager and Staff accounts.");
            }
            validateMemberIdFormat(newRole, requestedMemberId);
            ensureMemberIdUnique(requestedMemberId, user.getId());
            user.setMemberId(requestedMemberId);
        } else {
            user.setMemberId(null);
        }

        // If username is being changed, check for conflicts
        String newUsername = req.getUsername() != null ? normalize(req.getUsername()) : user.getUsername();
        if (newUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty.");
        }
        if (!newUsername.equals(user.getUsername())) {
            ensureUsernameUnique(newUsername, user.getId());
            user.setUsername(newUsername);
        }

        if (req.getFullName() != null) user.setFullName(normalize(req.getFullName()));
        if (req.getEmail()    != null) user.setEmail(normalize(req.getEmail()));
        user.setRole(newRole);

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
