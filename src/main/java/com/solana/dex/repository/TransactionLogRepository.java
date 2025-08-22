package com.solana.dex.repository;

import com.solana.dex.entity.Transaction;
import com.solana.dex.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 交易日志数据访问层
 */
@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, UUID> {

    /**
     * 根据交易记录查找日志
     * @param transaction 交易记录
     * @param pageable 分页参数
     * @return 交易日志分页
     */
    Page<TransactionLog> findByTransaction(Transaction transaction, Pageable pageable);

    /**
     * 根据交易ID查找日志
     * @param transactionId 交易ID
     * @param pageable 分页参数
     * @return 交易日志分页
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.transaction.id = :transactionId ORDER BY tl.createdAt DESC")
    Page<TransactionLog> findByTransactionId(@Param("transactionId") UUID transactionId, Pageable pageable);

    /**
     * 根据日志级别查找
     * @param logLevel 日志级别
     * @param pageable 分页参数
     * @return 交易日志分页
     */
    Page<TransactionLog> findByLogLevel(TransactionLog.LogLevel logLevel, Pageable pageable);

    /**
     * 根据执行阶段查找日志
     * @param executionPhase 执行阶段
     * @param pageable 分页参数
     * @return 交易日志分页
     */
    Page<TransactionLog> findByExecutionPhase(String executionPhase, Pageable pageable);

    /**
     * 根据时间范围查找日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 交易日志分页
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.createdAt BETWEEN :startTime AND :endTime ORDER BY tl.createdAt DESC")
    Page<TransactionLog> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              Pageable pageable);

    /**
     * 查找指定交易的错误日志
     * @param transaction 交易记录
     * @return 错误日志列表
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.transaction = :transaction AND tl.logLevel = 'ERROR' ORDER BY tl.createdAt DESC")
    List<TransactionLog> findErrorLogsByTransaction(@Param("transaction") Transaction transaction);

    /**
     * 查找最近的错误日志
     * @param limit 限制数量
     * @return 最近错误日志列表
     */
    @Query(value = "SELECT * FROM transaction_logs WHERE log_level = 'ERROR' " +
                   "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<TransactionLog> findRecentErrorLogs(@Param("limit") int limit);

    /**
     * 统计指定时间范围内的日志数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    @Query("SELECT COUNT(tl) FROM TransactionLog tl WHERE tl.createdAt BETWEEN :startTime AND :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定级别的日志数量
     * @param logLevel 日志级别
     * @return 日志数量
     */
    long countByLogLevel(TransactionLog.LogLevel logLevel);

    /**
     * 统计指定交易的日志数量
     * @param transaction 交易记录
     * @return 日志数量
     */
    long countByTransaction(Transaction transaction);

    /**
     * 查找执行时间超过阈值的日志
     * @param threshold 时间阈值（毫秒）
     * @param pageable 分页参数
     * @return 慢执行日志分页
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.executionTimeMs > :threshold ORDER BY tl.executionTimeMs DESC")
    Page<TransactionLog> findSlowExecutionLogs(@Param("threshold") Long threshold, Pageable pageable);

    /**
     * 根据日志级别和时间范围统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志级别统计
     */
    @Query("SELECT tl.logLevel, COUNT(tl) FROM TransactionLog tl " +
           "WHERE tl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY tl.logLevel")
    List<Object[]> findLogLevelStatistics(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 根据执行阶段统计平均执行时间
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行阶段统计
     */
    @Query("SELECT tl.executionPhase, AVG(tl.executionTimeMs), COUNT(tl) FROM TransactionLog tl " +
           "WHERE tl.createdAt BETWEEN :startTime AND :endTime AND tl.executionTimeMs IS NOT NULL " +
           "GROUP BY tl.executionPhase")
    List<Object[]> findExecutionPhaseStatistics(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的日志
     * @param cutoffTime 截止时间
     * @return 删除的记录数
     */
    @Query("DELETE FROM TransactionLog tl WHERE tl.createdAt < :cutoffTime")
    int deleteOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查找包含特定关键词的日志
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 匹配的日志分页
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.message LIKE %:keyword% ORDER BY tl.createdAt DESC")
    Page<TransactionLog> findByMessageContaining(@Param("keyword") String keyword, Pageable pageable);
}