package com.solana.dex.repository;

import com.solana.dex.entity.DexConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DEX配置数据访问层
 */
@Repository
public interface DexConfigRepository extends JpaRepository<DexConfig, UUID> {

    /**
     * 根据DEX名称查找配置
     * @param dexName DEX名称
     * @return DexConfig实例
     */
    Optional<DexConfig> findByDexName(String dexName);

    /**
     * 根据程序ID查找配置
     * @param programId 程序ID
     * @return DexConfig实例
     */
    Optional<DexConfig> findByProgramId(String programId);

    /**
     * 查找激活状态的DEX配置
     * @param isActive 是否激活
     * @return DexConfig列表
     */
    List<DexConfig> findByIsActive(Boolean isActive);

    /**
     * 查找所有激活的DEX配置
     * @return 激活的DexConfig列表
     */
    @Query("SELECT dc FROM DexConfig dc WHERE dc.isActive = true ORDER BY dc.dexName")
    List<DexConfig> findAllActive();

    /**
     * 根据DEX名称和激活状态查找
     * @param dexName DEX名称
     * @param isActive 是否激活
     * @return DexConfig实例
     */
    Optional<DexConfig> findByDexNameAndIsActive(String dexName, Boolean isActive);

    /**
     * 检查DEX名称是否存在
     * @param dexName DEX名称
     * @return true if exists, false otherwise
     */
    boolean existsByDexName(String dexName);

    /**
     * 检查程序ID是否存在
     * @param programId 程序ID
     * @return true if exists, false otherwise
     */
    boolean existsByProgramId(String programId);

    /**
     * 统计激活的DEX数量
     * @return 激活的DEX数量
     */
    long countByIsActive(Boolean isActive);

    /**
     * 根据配置数据中的特定字段查找
     * @param key 配置键
     * @param value 配置值
     * @return DexConfig列表
     */
    @Query(value = "SELECT * FROM dex_configs WHERE is_active = true AND config_data ->> :key = :value", 
           nativeQuery = true)
    List<DexConfig> findByConfigValue(@Param("key") String key, @Param("value") String value);

    /**
     * 查找支持特定操作的DEX
     * @param operation 操作类型
     * @return DexConfig列表
     */
    @Query(value = "SELECT * FROM dex_configs WHERE is_active = true AND " +
                   "config_data -> 'supported_operations' ? :operation", 
           nativeQuery = true)
    List<DexConfig> findBySupportsOperation(@Param("operation") String operation);

    /**
     * 查找费率在指定范围内的DEX
     * @param minFeeRate 最小费率
     * @param maxFeeRate 最大费率
     * @return DexConfig列表
     */
    @Query(value = "SELECT * FROM dex_configs WHERE is_active = true AND " +
                   "CAST(config_data ->> 'fee_rate' AS DECIMAL) BETWEEN :minFeeRate AND :maxFeeRate", 
           nativeQuery = true)
    List<DexConfig> findByFeeRateBetween(@Param("minFeeRate") Double minFeeRate, 
                                        @Param("maxFeeRate") Double maxFeeRate);
}