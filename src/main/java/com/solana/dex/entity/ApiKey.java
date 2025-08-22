package com.solana.dex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API密钥实体类
 * 用于管理API认证和权限控制
 */
@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * API密钥
     */
    @Column(name = "api_key", unique = true, nullable = false, length = 64)
    private String apiKey;

    /**
     * 用户名
     */
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    /**
     * 是否激活
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 速率限制（每小时请求次数）
     */
    @Column(name = "rate_limit", nullable = false)
    @Builder.Default
    private Integer rateLimit = 1000;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 检查API密钥是否有效
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (!isActive) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否即将过期（7天内）
     * @return true if expiring soon, false otherwise
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.isBefore(LocalDateTime.now().plusDays(7));
    }
}