package com.solana.dex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.sava.rpc.json.http.client.SolanaRpcClient;
import software.sava.rpc.json.http.response.AccountInfo;
import software.sava.core.accounts.PublicKey;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Solana配置类
 * 管理sava库的连接和配置
 */
@Configuration
@ConfigurationProperties(prefix = "solana")
@Data
public class SolanaConfig {

    /**
     * RPC配置
     */
    private Rpc rpc = new Rpc();

    /**
     * DEX配置
     */
    private Map<String, DexProperties> dex;

    @Data
    public static class Rpc {
        private String mainnetUrl = "https://api.mainnet-beta.solana.com";
        private String devnetUrl = "https://api.devnet.solana.com";
        private int timeout = 30000;
        private int maxRetries = 3;
    }

    @Data
    public static class DexProperties {
        private String programId;
        private double feeRate;
        private long minAmount;
        private double maxSlippage = 10.0;
        private int rpcTimeout = 30000;
        private int maxRetries = 3;
    }

    /**
     * 创建主网RPC客户端
     * @return SolanaRpcClient实例
     */
    @Bean(name = "mainnetRpcClient")
    public SolanaRpcClient mainnetRpcClient() {
        return createRpcClient(rpc.getMainnetUrl());
    }

    /**
     * 创建测试网RPC客户端
     * @return SolanaRpcClient实例
     */
    @Bean(name = "devnetRpcClient")
    public SolanaRpcClient devnetRpcClient() {
        return createRpcClient(rpc.getDevnetUrl());
    }

    /**
     * 创建RPC客户端
     * @param url RPC节点URL
     * @return SolanaRpcClient实例
     */
    private SolanaRpcClient createRpcClient(String url) {
        try {
            return SolanaRpcClient.createClient(URI.create(url));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Solana RPC client for URL: " + url, e);
        }
    }

    /**
     * 获取指定DEX的配置
     * @param dexName DEX名称
     * @return DEX配置
     */
    public DexProperties getDexConfig(String dexName) {
        DexProperties config = dex.get(dexName.toLowerCase());
        if (config == null) {
            throw new IllegalArgumentException("Unknown DEX: " + dexName);
        }
        return config;
    }

    /**
     * 获取指定DEX的程序ID
     * @param dexName DEX名称
     * @return 程序ID的PublicKey
     */
    public PublicKey getDexProgramId(String dexName) {
        DexProperties config = getDexConfig(dexName);
        return PublicKey.fromBase58(config.getProgramId());
    }

    /**
     * 检查DEX是否支持
     * @param dexName DEX名称
     * @return true if supported, false otherwise
     */
    public boolean isDexSupported(String dexName) {
        return dex.containsKey(dexName.toLowerCase());
    }

    /**
     * 获取所有支持的DEX名称
     * @return 支持的DEX名称集合
     */
    public java.util.Set<String> getSupportedDexes() {
        return dex.keySet();
    }

    /**
     * 验证滑点是否在允许范围内
     * @param dexName DEX名称
     * @param slippage 滑点值
     * @return true if valid, false otherwise
     */
    public boolean isSlippageValid(String dexName, double slippage) {
        DexProperties config = getDexConfig(dexName);
        return slippage >= 0 && slippage <= config.getMaxSlippage();
    }

    /**
     * 验证交易数量是否满足最小要求
     * @param dexName DEX名称
     * @param amount 交易数量
     * @return true if valid, false otherwise
     */
    public boolean isAmountValid(String dexName, long amount) {
        DexProperties config = getDexConfig(dexName);
        return amount >= config.getMinAmount();
    }

    /**
     * 计算交易费用
     * @param dexName DEX名称
     * @param amount 交易数量
     * @return 预估费用
     */
    public double calculateFee(String dexName, long amount) {
        DexProperties config = getDexConfig(dexName);
        return amount * config.getFeeRate();
    }
}