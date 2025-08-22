package com.solana.dex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 交易编码请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionEncodeRequest {

    /**
     * 用户钱包地址
     */
    @NotBlank(message = "钱包地址不能为空")
    @Size(min = 32, max = 44, message = "钱包地址长度必须在32-44字符之间")
    private String walletAddress;

    /**
     * 代币合约地址
     */
    @NotBlank(message = "代币地址不能为空")
    @Size(min = 32, max = 44, message = "代币地址长度必须在32-44字符之间")
    private String tokenMint;

    /**
     * 交易数量（以最小单位计算）
     */
    @NotBlank(message = "交易数量不能为空")
    @Pattern(regexp = "^[1-9]\\d*$", message = "交易数量必须为正整数")
    private String amount;

    /**
     * 滑点容忍度（百分比）
     */
    @DecimalMin(value = "0.0", message = "滑点不能小于0")
    @DecimalMax(value = "50.0", message = "滑点不能大于50%")
    @Builder.Default
    private BigDecimal slippage = BigDecimal.valueOf(1.0);

    /**
     * 操作类型（buy/sell）
     */
    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "^(buy|sell)$", message = "操作类型只能是buy或sell")
    private String operation;

    /**
     * 优先费用（lamports）
     */
    @Min(value = 0, message = "优先费用不能小于0")
    @Max(value = 1000000, message = "优先费用不能大于1000000 lamports")
    private Long priorityFee;

    /**
     * 是否使用主网
     */
    @Builder.Default
    private Boolean useMainnet = true;

    /**
     * 最大等待时间（秒）
     */
    @Min(value = 1, message = "最大等待时间不能小于1秒")
    @Max(value = 300, message = "最大等待时间不能大于300秒")
    @Builder.Default
    private Integer maxWaitTime = 30;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * 验证钱包地址格式
     * @return true if valid, false otherwise
     */
    public boolean isValidWalletAddress() {
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            return false;
        }
        // 简单的Base58格式验证
        return walletAddress.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$");
    }

    /**
     * 验证代币地址格式
     * @return true if valid, false otherwise
     */
    public boolean isValidTokenMint() {
        if (tokenMint == null || tokenMint.trim().isEmpty()) {
            return false;
        }
        // 简单的Base58格式验证
        return tokenMint.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$");
    }

    /**
     * 获取数值形式的交易数量
     * @return 交易数量
     */
    public Long getAmountAsLong() {
        try {
            return Long.parseLong(amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取滑点的小数形式
     * @return 滑点小数值（如1%返回0.01）
     */
    public double getSlippageAsDecimal() {
        if (slippage == null) {
            return 0.01; // 默认1%
        }
        return slippage.doubleValue() / 100.0;
    }

    /**
     * 检查是否为买入操作
     * @return true if buy operation, false otherwise
     */
    public boolean isBuyOperation() {
        return "buy".equalsIgnoreCase(operation);
    }

    /**
     * 检查是否为卖出操作
     * @return true if sell operation, false otherwise
     */
    public boolean isSellOperation() {
        return "sell".equalsIgnoreCase(operation);
    }

    /**
     * 获取额外参数
     * @param key 参数键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 参数值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtraParam(String key, T defaultValue) {
        if (extraParams == null || !extraParams.containsKey(key)) {
            return defaultValue;
        }
        try {
            return (T) extraParams.get(key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * 设置额外参数
     * @param key 参数键
     * @param value 参数值
     * @return 当前实例
     */
    public TransactionEncodeRequest withExtraParam(String key, Object value) {
        if (extraParams == null) {
            extraParams = new java.util.HashMap<>();
        }
        extraParams.put(key, value);
        return this;
    }

    /**
     * 验证请求参数的完整性
     * @return 验证结果消息，null表示验证通过
     */
    public String validate() {
        if (!isValidWalletAddress()) {
            return "无效的钱包地址格式";
        }
        if (!isValidTokenMint()) {
            return "无效的代币地址格式";
        }
        if (getAmountAsLong() == null || getAmountAsLong() <= 0) {
            return "无效的交易数量";
        }
        if (slippage != null && (slippage.compareTo(BigDecimal.ZERO) < 0 || slippage.compareTo(BigDecimal.valueOf(50)) > 0)) {
            return "滑点必须在0-50%之间";
        }
        if (!isBuyOperation() && !isSellOperation()) {
            return "操作类型必须是buy或sell";
        }
        return null; // 验证通过
    }
}