package com.solana.dex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DEX配置实体类
 * 存储各个DEX的程序ID和配置参数
 */
@Entity
@Table(name = "dex_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DexConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * DEX名称
     */
    @Column(name = "dex_name", unique = true, nullable = false, length = 50)
    private String dexName;

    /**
     * DEX程序ID
     */
    @Column(name = "program_id", nullable = false, length = 44)
    private String programId;

    /**
     * 配置数据（JSON格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configData;

    /**
     * 是否激活
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 获取费率配置
     * @return 费率（小数形式，如0.0025表示0.25%）
     */
    public Double getFeeRate() {
        Object feeRate = configData.get("fee_rate");
        if (feeRate instanceof Number) {
            return ((Number) feeRate).doubleValue();
        }
        return 0.0025; // 默认费率0.25%
    }

    /**
     * 获取最小交易数量
     * @return 最小交易数量
     */
    public Long getMinAmount() {
        Object minAmount = configData.get("min_amount");
        if (minAmount instanceof Number) {
            return ((Number) minAmount).longValue();
        }
        return 1000L; // 默认最小数量
    }

    /**
     * 获取最大滑点
     * @return 最大滑点（百分比形式，如5.0表示5%）
     */
    public Double getMaxSlippage() {
        Object maxSlippage = configData.get("max_slippage");
        if (maxSlippage instanceof Number) {
            return ((Number) maxSlippage).doubleValue();
        }
        return 10.0; // 默认最大滑点10%
    }

    /**
     * 获取RPC超时时间
     * @return 超时时间（毫秒）
     */
    public Integer getRpcTimeout() {
        Object timeout = configData.get("rpc_timeout");
        if (timeout instanceof Number) {
            return ((Number) timeout).intValue();
        }
        return 30000; // 默认30秒
    }

    /**
     * 获取重试次数
     * @return 重试次数
     */
    public Integer getMaxRetries() {
        Object maxRetries = configData.get("max_retries");
        if (maxRetries instanceof Number) {
            return ((Number) maxRetries).intValue();
        }
        return 3; // 默认重试3次
    }

    /**
     * 检查DEX是否支持指定操作
     * @param operation 操作类型
     * @return true if supported, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean supportsOperation(String operation) {
        Object supportedOps = configData.get("supported_operations");
        if (supportedOps instanceof java.util.List) {
            return ((java.util.List<String>) supportedOps).contains(operation);
        }
        return true; // 默认支持所有操作
    }

    /**
     * 获取特定配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        Object value = configData.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
}