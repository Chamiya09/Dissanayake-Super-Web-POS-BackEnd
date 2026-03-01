package com.dissayakesuper.web_pos_backend.auth.dto;

/**
 * Response payload returned by POST /api/auth/login on success.
 *
 * @param token    Signed JWT — include as  Authorization: Bearer <token>  on every subsequent request.
 * @param username The authenticated user's display name (fullName from the DB).
 * @param role     The user's role: Owner | Manager | Staff.
 */
public record AuthResponse(String token, String username, String role) {}
