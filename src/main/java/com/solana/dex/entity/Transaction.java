package com.solana.dex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 交易记录实体类
 * 存储所有DEX交易的详细信息
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * 交易ID（业务唯一标识）
     */
    @Column(name = "transaction_id", unique = true, nullable = false, length = 64)
    private String transactionId;

    /**
     * 关联的API密钥
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    /**
     * 用户钱包地址
     */
    @Column(name = "wallet_address", nullable = false, length = 44)
    private String walletAddress;

    /**
     * DEX类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dex_type", nullable = false, length = 20)
    private DexType dexType;

    /**
     * 操作类型
     */
    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;

    /**
     * 代币合约地址
     */
    @Column(name = "token_mint", length = 44)
    private String tokenMint;

    /**
     * 交易数量
     */
    @Column(name = "amount", precision = 20, scale = 8)
    private BigDecimal amount;

    /**
     * 滑点容忍度
     */
    @Column(name = "slippage", precision = 5, scale = 2)
    private BigDecimal slippage;

    /**
     * 交易状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * 编码后的交易数据
     */
    @Column(name = "transaction_data", columnDefinition = "TEXT")
    private String transactionData;

    /**
     * 链上交易签名
     */
    @Column(name = "signature", length = 88)
    private String signature;

    /**
     * 预估交易费用（SOL）
     */
    @Column(name = "estimated_fee", precision = 10, scale = 8)
    private BigDecimal estimatedFee;

    /**
     * 实际交易费用（SOL）
     */
    @Column(name = "actual_fee", precision = 10, scale = 8)
    private BigDecimal actualFee;

    /**
     * 区块确认时间
     */
    @Column(name = "block_time")
    private LocalDateTime blockTime;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * DEX类型枚举
     */
    public enum DexType {
        PUMPFUN("pumpfun"),
        PUMPSWAP("pumpswap"),
        RAYDIUM("raydium"),
        BATCH("batch");

        private final String value;

        DexType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DexType fromValue(String value) {
            for (DexType type : DexType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown DEX type: " + value);
        }
    }

    /**
     * 交易状态枚举
     */
    public enum TransactionStatus {
        PENDING("pending"),
        ENCODED("encoded"),
        SUBMITTED("submitted"),
        CONFIRMED("confirmed"),
        FAILED("failed");

        private final String value;

        TransactionStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static TransactionStatus fromValue(String value) {
            for (TransactionStatus status : TransactionStatus.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown transaction status: " + value);
        }
    }

    /**
     * 检查交易是否完成
     * @return true if completed (confirmed or failed), false otherwise
     */
    public boolean isCompleted() {
        return status == TransactionStatus.CONFIRMED || status == TransactionStatus.FAILED;
    }

    /**
     * 检查交易是否成功
     * @return true if confirmed, false otherwise
     */
    public boolean isSuccessful() {
        return status == TransactionStatus.CONFIRMED;
    }
}