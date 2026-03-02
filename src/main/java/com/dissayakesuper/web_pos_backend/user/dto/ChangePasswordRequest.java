package com.dissayakesuper.web_pos_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Username is required.")
    private String username;

    @NotBlank(message = "Current password is required.")
    private String currentPassword;

    @NotBlank(message = "New password is required.")
    @Size(min = 6, message = "New password must be at least 6 characters.")
    private String newPassword;
}
