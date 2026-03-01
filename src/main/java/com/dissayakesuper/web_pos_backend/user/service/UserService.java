package com.dissayakesuper.web_pos_backend.user.service;

import com.dissayakesuper.web_pos_backend.user.dto.ChangePasswordRequest;
import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserRepository repo) {
        this.repo    = repo;
        this.encoder = new BCryptPasswordEncoder();
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
