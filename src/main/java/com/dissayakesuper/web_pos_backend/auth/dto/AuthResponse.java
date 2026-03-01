package com.dissayakesuper.web_pos_backend.auth.dto;

/**
 * Response payload returned by POST /api/auth/login on success.
 *
 * @param token    Signed JWT — include as  Authorization: Bearer <token>  on every subsequent request.
 * @param username The authenticated user's login username (used for API calls like change-password).
 * @param name     The user's full display name (shown in the UI).
 * @param role     The user's role: Owner | Manager | Staff.
 */
public record AuthResponse(String token, String username, String name, String role) {}
