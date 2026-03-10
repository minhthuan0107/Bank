package com.example.bank.common.config.security;

import com.example.bank.entity.user.User;
import com.example.bank.enums.user.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailsImpl implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    @JsonIgnore
    private String passwordHash;

    private Integer passwordVersion;

    private AccountStatus status; // ACTIVE / LOCKED / SUSPENDED

    private LocalDateTime deletedAt;

    private Collection<? extends GrantedAuthority> authorities;

    // =========================
    // Mapping từ Entity
    // =========================
    public static UserDetailsImpl from(User user) {

        List<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())
        );

        return UserDetailsImpl.builder()
                .id(user.getId())
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .passwordVersion(user.getPasswordVersion())
                .status(user.getStatus())
                .deletedAt(user.getDeletedAt())
                .authorities(auths)
                .build();
    }

    // =========================
    // Spring Security methods
    // =========================
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.LOCKED;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Optional helper
    @JsonIgnore
    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

