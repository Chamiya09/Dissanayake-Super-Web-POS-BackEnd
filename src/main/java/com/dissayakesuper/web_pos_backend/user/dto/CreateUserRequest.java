package com.dissayakesuper.web_pos_backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

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

    @Size(max = 30, message = "Phone number must be 30 characters or fewer.")
    private String phoneNumber;

    @Size(max = 255, message = "Address must be 255 characters or fewer.")
    private String address;

    @NotBlank(message = "Role is required.")
    @Size(max = 30, message = "Role must be 30 characters or fewer.")
    private String role;

    @NotBlank(message = "Password is required.")
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters.")
    private String password;

    @JsonProperty("isSenior")
    private boolean isSenior;

    public CreateUserRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isSenior() { return isSenior; }
    public void setSenior(boolean senior) { isSenior = senior; }
}
