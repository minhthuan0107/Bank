package com.example.bank.common.security;

import com.example.angelproject.modules.user.domain.entity.User;
import com.example.angelproject.modules.user.domain.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder(toBuilder = true)
/*
    Lớp này triển khai UserDetails của Spring Security,
    chứa thông tin người dùng cần thiết cho xác thực và ủy quyền.
 */
public class UserDetailsImpl implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String publicId;
    private String displayName;
    private String email;
    private String phone;
    @JsonIgnore
    @ToString.Exclude
    private String loginPrincipal;
    @JsonIgnore
    @ToString.Exclude
    private String passwordHash;
    private Integer passwordVersion;
    private AccountStatus accountStatus;  // ACTIVE / LOCKED / SUSPENDED / DISABLED...
    private String avatarUrl;
    private LocalDate dob;
    private String address;
    private Boolean deleted;    // soft delete flag
    private LocalDateTime deletedAt;
    private Collection<? extends GrantedAuthority> authorities;


    public UserDetailsImpl(Long id, String publicId, String displayName, String email, String phone,
                           String loginPrincipal, String passwordHash, Integer passwordVersion, AccountStatus accountStatus, String avatarUrl,
                           LocalDate dob, String address, Boolean deleted, LocalDateTime deletedAt, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.publicId = publicId;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.loginPrincipal = loginPrincipal;
        this.passwordHash = passwordHash;
        this.passwordVersion = passwordVersion;
        this.accountStatus = accountStatus;
        this.avatarUrl = avatarUrl;
        this.dob = dob;
        this.address = address;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.authorities = authorities;
    }

    public static UserDetailsImpl from(User user) {
        // Ưu tiên email làm username; nếu trống thì dùng phone
        String principal = (user.getEmail() != null && !user.getEmail().isBlank())
                ? user.getEmail()
                : user.getPhone();

        if (principal == null || principal.isBlank()) {
            throw new IllegalStateException("User has neither email nor phone as login principal (id=" + user.getId() + ")");
        }
        // Nếu có nhiều role, map stream. Ở đây là 1 role:
        List<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode())
        );
        return UserDetailsImpl.builder()
                .id(user.getId())
                .publicId(user.getPublicId())
                .loginPrincipal(principal)        // đăng nhập: email/sđt
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .passwordHash(user.getPasswordHash())
                .passwordVersion(user.getPasswordVersion())
                .accountStatus(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .dob(user.getDob())
                .address(user.getAddress())
                .deletedAt(user.getDeletedAt())
                .authorities(auths)
                .build();
    }
    // Dùng khi xác thực JWT (không cần principal)
    public static UserDetailsImpl fromForJwt(User user) {
        List<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode())
        );
        return UserDetailsImpl.builder()
                .id(user.getId())
                .publicId(user.getPublicId())
                // JWT không cần principal (username), có thể set null
                .loginPrincipal(null)
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .passwordHash(user.getPasswordHash())
                .passwordVersion(user.getPasswordVersion())
                .accountStatus(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .dob(user.getDob())
                .address(user.getAddress())
                .deletedAt(user.getDeletedAt())
                .authorities(auths)
                .build();
    }

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
        return loginPrincipal;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    /** Khoá nếu status là LOCKED/SUSPENDED hoặc bị đánh dấu deleted */
    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.LOCKED;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus != AccountStatus.DISABLED;
    }

    @JsonIgnore
    public boolean isAdmin() {
        return authorities != null &&
                authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @JsonIgnore
    public boolean isUser() {
        return authorities != null &&
                authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }
}
