package com.dissayakesuper.web_pos_backend.security;

import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges the application's {@link User} entity with Spring Security's
 * {@link UserDetails} contract.
 *
 * Role mapping:
 *   User.role "Owner"   → GrantedAuthority "ROLE_OWNER"
 *   User.role "Manager" → GrantedAuthority "ROLE_MANAGER"
 *   User.role "Staff"   → GrantedAuthority "ROLE_STAFF"
 *
 * The {@link User#isActive()} flag is surfaced as {@link UserDetails#isEnabled()},
 * which causes Spring Security to throw {@link org.springframework.security.authentication.DisabledException}
 * for deactivated accounts during authentication.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())))
                .disabled(!user.isActive())        // active=false → disabled → DisabledException
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .build();
    }
}
