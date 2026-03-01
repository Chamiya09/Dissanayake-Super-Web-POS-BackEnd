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

    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Password is required.")
    private String password;
}
