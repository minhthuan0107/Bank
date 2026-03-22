package com.example.bank.entity.auth;

import com.example.bank.entity.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_sessions")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
/**
 * Bảng quản lý phiên đăng nhập (refresh token) của người dùng.
 * Mỗi user có thể có nhiều phiên (theo thiết bị / browser).
 * Dùng để kiểm soát token hợp lệ, thu hồi, và bảo mật.
 */
public class AuthSession {
    /** Khóa chính tự tăng */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người dùng sở hữu phiên (FK → users.id) */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Thông tin user-agent rút gọn (browser / app) */
    @Size(max = 255)
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** Địa chỉ IP đăng nhập (IPv4 / IPv6) */
    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Hash (SHA-256) của refresh token (duy nhất trong DB) */
    @Column(name = "refresh_token_hash", length = 64, unique = true)
    private String refreshTokenHash;

    /** Thời điểm cấp refresh token */
    @Column(name = "issued_at")
    private Instant issuedAt;

    /** Thời điểm hết hạn token */
    @Column(name = "expires_at")
    private  Instant expiresAt;

    /** Trạng thái thu hồi (false = active, true = revoked) */
    @Column(name = "is_revoked")
    private Boolean isRevoked = false ;

    /** Thời điểm bị thu hồi*/
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Lần cuối dùng để refresh token */
    @Column(name = "last_used_at")
    private  Instant lastUsedAt;

    /** Gán giá trị mặc định khi tạo mới phiên */
    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null)
            this.issuedAt =  Instant.now();
    }

    /** Thu hồi token */
    public void revoke() {
        this.isRevoked = true;
        this.revokedAt =  Instant.now();
    }

    /** Cập nhật lần cuối sử dụng */
    public void updateLastUsed() {
        this.lastUsedAt =  Instant.now();
    }

}