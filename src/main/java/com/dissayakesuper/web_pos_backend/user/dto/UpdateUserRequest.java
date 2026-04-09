package com.dissayakesuper.web_pos_backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @NotBlank(message = "Username is required.")
    @Size(max = 50, message = "Username must be 50 characters or fewer.")
    private String username;

    @NotBlank(message = "Member ID is required.")
    @Size(max = 30, message = "Member ID must be 30 characters or fewer.")
    private String memberId;

    @NotBlank(message = "Full name is required.")
    @Size(max = 150, message = "Full name must be 150 characters or fewer.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be 150 characters or fewer.")
    private String email;

    @NotBlank(message = "Role is required.")
    @Size(max = 30, message = "Role must be 30 characters or fewer.")
    private String role;

    public UpdateUserRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
