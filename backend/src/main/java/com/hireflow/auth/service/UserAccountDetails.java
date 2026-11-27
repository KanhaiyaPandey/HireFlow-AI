package com.hireflow.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.hireflow.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Adapts our {@link User} to what Spring Security's login check expects. Used only during login. */
public class UserAccountDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean canSignIn;

    UserAccountDetails(User user, boolean canSignIn) {
        this.userId = user.getId();
        this.email = user.getEmail();
        // An invited user without a password gets an empty hash, which never matches.
        this.passwordHash = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        this.role = user.getRole().name();
        this.canSignIn = canSignIn;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isEnabled() {
        return canSignIn;
    }
}
