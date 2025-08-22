package com.solana.dex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 交易日志实体类
 * 记录交易过程中的详细日志信息
 */
@Entity
@Table(name = "transaction_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * 关联的交易记录
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /**
     * 日志级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    private LogLevel logLevel;

    /**
     * 日志消息
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 异常堆栈信息
     */
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    /**
     * 执行阶段
     */
    @Column(name = "execution_phase", length = 50)
    private String executionPhase;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        TRACE("trace"),
        DEBUG("debug"),
        INFO("info"),
        WARN("warn"),
        ERROR("error");

        private final String value;

        LogLevel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static LogLevel fromValue(String value) {
            for (LogLevel level : LogLevel.values()) {
                if (level.value.equals(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Unknown log level: " + value);
        }
    }

    /**
     * 创建INFO级别日志
     * @param transaction 交易记录
     * @param message 日志消息
     * @return TransactionLog实例
     */
    public static TransactionLog info(Transaction transaction, String message) {
        return TransactionLog.builder()
                .transaction(transaction)
                .logLevel(LogLevel.INFO)
                .message(message)
                .build();
    }

    /**
     * 创建ERROR级别日志
     * @param transaction 交易记录
     * @param message 日志消息
     * @param stackTrace 异常堆栈
     * @return TransactionLog实例
     */
    public static TransactionLog error(Transaction transaction, String message, String stackTrace) {
        return TransactionLog.builder()
                .transaction(transaction)
                .logLevel(LogLevel.ERROR)
                .message(message)
                .stackTrace(stackTrace)
                .build();
    }

    /**
     * 创建带执行阶段的日志
     * @param transaction 交易记录
     * @param level 日志级别
     * @param message 日志消息
     * @param phase 执行阶段
     * @param executionTime 执行耗时
     * @return TransactionLog实例
     */
    public static TransactionLog withPhase(Transaction transaction, LogLevel level, String message, 
                                          String phase, Long executionTime) {
        return TransactionLog.builder()
                .transaction(transaction)
                .logLevel(level)
                .message(message)
                .executionPhase(phase)
                .executionTimeMs(executionTime)
                .build();
    }
}