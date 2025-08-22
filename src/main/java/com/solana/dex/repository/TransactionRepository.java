package com.solana.dex.repository;

import com.solana.dex.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 交易记录数据访问层
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * 根据交易ID查找
     * @param transactionId 交易ID
     * @return Transaction实例
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * 根据钱包地址查找交易记录
     * @param walletAddress 钱包地址
     * @param pageable 分页参数
     * @return 交易记录分页
     */
    Page<Transaction> findByWalletAddress(String walletAddress, Pageable pageable);

    /**
     * 根据DEX类型查找交易记录
     * @param dexType DEX类型
     * @param pageable 分页参数
     * @return 交易记录分页
     */
    Page<Transaction> findByDexType(Transaction.DexType dexType, Pageable pageable);

    /**
     * 根据交易状态查找
     * @param status 交易状态
     * @param pageable 分页参数
     * @return 交易记录分页
     */
    Page<Transaction> findByStatus(Transaction.TransactionStatus status, Pageable pageable);

    /**
     * 根据钱包地址和DEX类型查找
     * @param walletAddress 钱包地址
     * @param dexType DEX类型
     * @param pageable 分页参数
     * @return 交易记录分页
     */
    Page<Transaction> findByWalletAddressAndDexType(String walletAddress, 
                                                    Transaction.DexType dexType, 
                                                    Pageable pageable);

    /**
     * 根据时间范围查找交易记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 交易记录分页
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startTime AND :endTime ORDER BY t.createdAt DESC")
    Page<Transaction> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           Pageable pageable);

    /**
     * 查找待处理的交易
     * @return 待处理交易列表
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('PENDING', 'ENCODED', 'SUBMITTED') ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions();

    /**
     * 查找超时的交易
     * @param timeoutThreshold 超时阈值
     * @return 超时交易列表
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN ('PENDING', 'ENCODED', 'SUBMITTED') AND t.createdAt < :timeoutThreshold")
    List<Transaction> findTimeoutTransactions(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 统计指定时间范围内的交易数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 交易数量
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt BETWEEN :startTime AND :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定DEX的交易数量
     * @param dexType DEX类型
     * @return 交易数量
     */
    long countByDexType(Transaction.DexType dexType);

    /**
     * 统计指定状态的交易数量
     * @param status 交易状态
     * @return 交易数量
     */
    long countByStatus(Transaction.TransactionStatus status);

    /**
     * 统计用户的交易数量
     * @param walletAddress 钱包地址
     * @return 交易数量
     */
    long countByWalletAddress(String walletAddress);

    /**
     * 查找成功率统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成功率统计
     */
    @Query("SELECT t.dexType, COUNT(t), " +
           "SUM(CASE WHEN t.status = 'CONFIRMED' THEN 1 ELSE 0 END) " +
           "FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY t.dexType")
    List<Object[]> findSuccessRateStatistics(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找交易量统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 交易量统计
     */
    @Query("SELECT DATE(t.createdAt), t.dexType, COUNT(t), SUM(t.estimatedFee) " +
           "FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(t.createdAt), t.dexType " +
           "ORDER BY DATE(t.createdAt) DESC")
    List<Object[]> findVolumeStatistics(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的交易记录
     * @param walletAddress 钱包地址
     * @param limit 限制数量
     * @return 最近交易列表
     */
    @Query(value = "SELECT * FROM transactions WHERE wallet_address = :walletAddress " +
                   "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Transaction> findRecentTransactions(@Param("walletAddress") String walletAddress,
                                           @Param("limit") int limit);

    /**
     * 根据签名查找交易
     * @param signature 交易签名
     * @return Transaction实例
     */
    Optional<Transaction> findBySignature(String signature);

    /**
     * 检查交易ID是否存在
     * @param transactionId 交易ID
     * @return true if exists, false otherwise
     */
    boolean existsByTransactionId(String transactionId);
}