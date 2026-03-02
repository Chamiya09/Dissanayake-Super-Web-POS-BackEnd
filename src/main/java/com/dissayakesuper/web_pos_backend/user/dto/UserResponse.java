package com.dissayakesuper.web_pos_backend.user.dto;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private boolean active;

    public UserResponse() {}

    public UserResponse(Long id, String username, String fullName, String email, String role, boolean active) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
