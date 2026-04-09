package com.dissayakesuper.web_pos_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/auth/login.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Login ID is required.")
    private String loginId;

    private String username;

    @NotBlank(message = "Password is required.")
    private String password;

    public String getLoginIdOrUsername() {
        String value = loginId != null ? loginId.trim() : "";
        if (!value.isBlank()) return value;
        return username != null ? username.trim() : "";
    }
}
