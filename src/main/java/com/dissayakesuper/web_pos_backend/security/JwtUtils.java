package com.dissayakesuper.web_pos_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility for generating and validating JWT tokens.
 *
 * Token payload:
 *   sub  → username
 *   role → user's role string (Owner / Manager / Staff)
 *   iat  → issued-at timestamp
 *   exp  → expiry (default 24 h)
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Key ───────────────────────────────────────────────────────────────────

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Builds a signed HS256 JWT for the supplied user.
     *
     * @param username the user's unique username (stored as JWT subject)
     * @param role     the user's role string (Owner / Manager / Staff)
     * @return compact JWT string — safe to return to the client
     */
    public String generateToken(String username, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    // ── Token parsing ─────────────────────────────────────────────────────────

    /** Extracts the username (subject) from a valid, non-expired token. */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extracts the role claim from a valid, non-expired token. */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Returns {@code true} if the token has a valid signature and has not expired.
     * Does NOT check whether the user still exists or is still active —
     * that is handled by {@link JwtAuthFilter}.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
