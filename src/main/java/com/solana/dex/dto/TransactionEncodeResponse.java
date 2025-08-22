package com.solana.dex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易编码响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionEncodeResponse {

    /**
     * 编码是否成功
     */
    private Boolean success;

    /**
     * Base64编码的交易数据
     */
    private String transactionData;

    /**
     * 预估交易费用（SOL）
     */
    private BigDecimal estimatedFee;

    /**
     * 交易追踪ID
     */
    private String transactionId;

    /**
     * DEX类型
     */
    private String dexType;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 钱包地址
     */
    private String walletAddress;

    /**
     * 代币地址
     */
    private String tokenMint;

    /**
     * 交易数量
     */
    private String amount;

    /**
     * 滑点容忍度
     */
    private BigDecimal slippage;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 额外信息
     */
    private Object metadata;

    /**
     * 创建成功响应
     * @param transactionData 交易数据
     * @param estimatedFee 预估费用
     * @param transactionId 交易ID
     * @return 成功响应
     */
    public static TransactionEncodeResponse success(String transactionData, 
                                                   BigDecimal estimatedFee, 
                                                   String transactionId) {
        return TransactionEncodeResponse.builder()
                .success(true)
                .transactionData(transactionData)
                .estimatedFee(estimatedFee)
                .transactionId(transactionId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败响应
     * @param error 错误信息
     * @param errorCode 错误代码
     * @return 失败响应
     */
    public static TransactionEncodeResponse failure(String error, String errorCode) {
        return TransactionEncodeResponse.builder()
                .success(false)
                .error(error)
                .errorCode(errorCode)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败响应
     * @param error 错误信息
     * @return 失败响应
     */
    public static TransactionEncodeResponse failure(String error) {
        return failure(error, "ENCODING_ERROR");
    }

    /**
     * 设置交易详情
     * @param dexType DEX类型
     * @param operationType 操作类型
     * @param walletAddress 钱包地址
     * @param tokenMint 代币地址
     * @param amount 交易数量
     * @param slippage 滑点
     * @return 当前实例
     */
    public TransactionEncodeResponse withDetails(String dexType, 
                                               String operationType, 
                                               String walletAddress, 
                                               String tokenMint, 
                                               String amount, 
                                               BigDecimal slippage) {
        this.dexType = dexType;
        this.operationType = operationType;
        this.walletAddress = walletAddress;
        this.tokenMint = tokenMint;
        this.amount = amount;
        this.slippage = slippage;
        return this;
    }

    /**
     * 设置元数据
     * @param metadata 元数据
     * @return 当前实例
     */
    public TransactionEncodeResponse withMetadata(Object metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * 检查是否成功
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 检查是否有错误
     * @return true if has error, false otherwise
     */
    public boolean hasError() {
        return !isSuccessful();
    }
}