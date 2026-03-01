package com.dissayakesuper.web_pos_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request and, when a valid Bearer token is present,
 * populates the Spring Security context so downstream filters and
 * controllers see an authenticated principal.
 *
 * Flow:
 *   Authorization: Bearer <token>
 *     → validate token signature & expiry
 *     → load UserDetails from DB (checks account is still active)
 *     → set UsernamePasswordAuthenticationToken in SecurityContextHolder
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils           = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null && jwtUtils.validateToken(token)) {
            String      username    = jwtUtils.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Only authenticate if the account is still enabled (active=true)
            if (userDetails.isEnabled()
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
