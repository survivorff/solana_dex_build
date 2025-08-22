package com.solana.dex.repository;

import com.solana.dex.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API密钥数据访问层
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * 根据API密钥查找
     * @param apiKey API密钥
     * @return ApiKey实例
     */
    Optional<ApiKey> findByApiKey(String apiKey);

    /**
     * 根据用户名查找
     * @param userName 用户名
     * @return ApiKey列表
     */
    List<ApiKey> findByUserName(String userName);

    /**
     * 查找激活状态的API密钥
     * @param isActive 是否激活
     * @return ApiKey列表
     */
    List<ApiKey> findByIsActive(Boolean isActive);

    /**
     * 根据用户名和激活状态查找
     * @param userName 用户名
     * @param isActive 是否激活
     * @return ApiKey列表
     */
    List<ApiKey> findByUserNameAndIsActive(String userName, Boolean isActive);

    /**
     * 查找即将过期的API密钥
     * @param expirationTime 过期时间阈值
     * @return ApiKey列表
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND ak.expiresAt IS NOT NULL AND ak.expiresAt <= :expirationTime")
    List<ApiKey> findExpiringSoon(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * 查找已过期的API密钥
     * @param currentTime 当前时间
     * @return ApiKey列表
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND ak.expiresAt IS NOT NULL AND ak.expiresAt < :currentTime")
    List<ApiKey> findExpired(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 统计用户的API密钥数量
     * @param userName 用户名
     * @return 数量
     */
    long countByUserName(String userName);

    /**
     * 统计激活的API密钥数量
     * @return 数量
     */
    long countByIsActive(Boolean isActive);

    /**
     * 检查API密钥是否存在
     * @param apiKey API密钥
     * @return true if exists, false otherwise
     */
    boolean existsByApiKey(String apiKey);

    /**
     * 根据速率限制范围查找
     * @param minRateLimit 最小速率限制
     * @param maxRateLimit 最大速率限制
     * @return ApiKey列表
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND ak.rateLimit BETWEEN :minRateLimit AND :maxRateLimit")
    List<ApiKey> findByRateLimitBetween(@Param("minRateLimit") Integer minRateLimit, 
                                       @Param("maxRateLimit") Integer maxRateLimit);

    /**
     * 查找高级用户（高速率限制）
     * @param premiumThreshold 高级用户阈值
     * @return ApiKey列表
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND ak.rateLimit >= :premiumThreshold")
    List<ApiKey> findPremiumUsers(@Param("premiumThreshold") Integer premiumThreshold);

    /**
     * 批量更新过期的API密钥状态
     * @param currentTime 当前时间
     * @return 更新的记录数
     */
    @Query("UPDATE ApiKey ak SET ak.isActive = false WHERE ak.isActive = true AND ak.expiresAt IS NOT NULL AND ak.expiresAt < :currentTime")
    int deactivateExpiredKeys(@Param("currentTime") LocalDateTime currentTime);
}