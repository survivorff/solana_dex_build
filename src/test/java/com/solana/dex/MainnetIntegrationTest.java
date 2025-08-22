package com.solana.dex;

import com.solana.dex.adapter.PumpfunAdapter;
import com.solana.dex.adapter.PumpSwapAdapter;
import com.solana.dex.adapter.RaydiumAdapter;
import com.solana.dex.config.SolanaConfig;
import com.solana.dex.dto.TransactionEncodeRequest;
import com.solana.dex.dto.TransactionEncodeResponse;
import com.solana.dex.service.SolanaTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.http.client.SolanaRpcClient;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Solana主网集成测试
 * 测试每个DEX在主网上的实际交易编码和上链功能
 * 
 * 注意：这些测试会连接到Solana主网，请确保：
 * 1. 有足够的SOL用于测试交易费用
 * 2. 测试钱包私钥已正确配置
 * 3. 网络连接稳定
 * 
 * 警告：测试涉及真实资金，请在测试网环境下进行初步验证
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MainnetIntegrationTest {

    @Autowired
    private PumpfunAdapter pumpfunAdapter;

    @Autowired
    private PumpSwapAdapter pumpSwapAdapter;

    @Autowired
    private RaydiumAdapter raydiumAdapter;

    @Autowired
    private SolanaTransactionService solanaTransactionService;

    @Autowired
    private SolanaConfig solanaConfig;

    @Autowired
    private SolanaRpcClient mainnetRpcClient;

    // 测试配置常量
    private static final String TEST_WALLET = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"; // 测试钱包地址
    private static final String TEST_TOKEN_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"; // USDC代币地址
    private static final String TEST_AMOUNT = "1000000"; // 1 USDC (6位小数)
    private static final BigDecimal TEST_SLIPPAGE = BigDecimal.valueOf(1.0); // 1%滑点
    private static final int TIMEOUT_SECONDS = 60;

    @BeforeAll
    static void setupClass() {
        log.info("=== 开始Solana主网集成测试 ===");
        log.warn("警告：此测试将连接到Solana主网，可能产生真实的交易费用");
    }

    @AfterAll
    static void teardownClass() {
        log.info("=== Solana主网集成测试完成 ===");
    }

    @BeforeEach
    void setup() {
        log.info("准备测试环境...");
        // 验证网络连接
        verifyNetworkConnection();
        // 验证钱包余额
        verifyWalletBalance();
    }

    /**
     * 测试Pumpfun DEX买入交易编码
     */
    @Test
    @Order(1)
    @DisplayName("测试Pumpfun买入交易编码")
    void testPumpfunBuyTransaction() {
        log.info("开始测试Pumpfun买入交易编码");
        
        TransactionEncodeRequest request = TransactionEncodeRequest.builder()
                .walletAddress(TEST_WALLET)
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("buy")
                .useMainnet(true)
                .maxWaitTime(30)
                .build();

        CompletableFuture<TransactionEncodeResponse> future = pumpfunAdapter.encodeTransaction(request);
        
        Assertions.assertDoesNotThrow(() -> {
            TransactionEncodeResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // 验证响应
            Assertions.assertNotNull(response, "响应不能为空");
            Assertions.assertTrue(response.isSuccessful(), "交易编码应该成功: " + response.getError());
            Assertions.assertNotNull(response.getTransactionData(), "交易数据不能为空");
            Assertions.assertNotNull(response.getTransactionId(), "交易ID不能为空");
            Assertions.assertNotNull(response.getEstimatedFee(), "预估费用不能为空");
            
            log.info("Pumpfun买入交易编码成功:");
            log.info("- 交易ID: {}", response.getTransactionId());
            log.info("- 预估费用: {} SOL", response.getEstimatedFee());
            log.info("- 交易数据长度: {} bytes", response.getTransactionData().length());
            
            // 验证交易数据格式
            validateTransactionData(response.getTransactionData());
        });
    }

    /**
     * 测试Pumpfun DEX卖出交易编码
     */
    @Test
    @Order(2)
    @DisplayName("测试Pumpfun卖出交易编码")
    void testPumpfunSellTransaction() {
        log.info("开始测试Pumpfun卖出交易编码");
        
        TransactionEncodeRequest request = TransactionEncodeRequest.builder()
                .walletAddress(TEST_WALLET)
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("sell")
                .useMainnet(true)
                .maxWaitTime(30)
                .build();

        CompletableFuture<TransactionEncodeResponse> future = pumpfunAdapter.encodeTransaction(request);
        
        Assertions.assertDoesNotThrow(() -> {
            TransactionEncodeResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            Assertions.assertNotNull(response);
            Assertions.assertTrue(response.isSuccessful(), "交易编码应该成功: " + response.getError());
            Assertions.assertEquals("sell", response.getOperationType());
            
            log.info("Pumpfun卖出交易编码成功: {}", response.getTransactionId());
        });
    }

    /**
     * 测试PumpSwap DEX交易编码
     */
    @Test
    @Order(3)
    @DisplayName("测试PumpSwap交易编码")
    void testPumpSwapTransaction() {
        log.info("开始测试PumpSwap交易编码");
        
        TransactionEncodeRequest request = TransactionEncodeRequest.builder()
                .walletAddress(TEST_WALLET)
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("buy")
                .useMainnet(true)
                .build();

        CompletableFuture<TransactionEncodeResponse> future = pumpSwapAdapter.encodeTransaction(request);
        
        Assertions.assertDoesNotThrow(() -> {
            TransactionEncodeResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            Assertions.assertNotNull(response);
            Assertions.assertTrue(response.isSuccessful(), "交易编码应该成功: " + response.getError());
            Assertions.assertEquals("pumpswap", response.getDexType());
            
            log.info("PumpSwap交易编码成功: {}", response.getTransactionId());
        });
    }

    /**
     * 测试Raydium DEX交易编码
     */
    @Test
    @Order(4)
    @DisplayName("测试Raydium交易编码")
    void testRaydiumTransaction() {
        log.info("开始测试Raydium交易编码");
        
        TransactionEncodeRequest request = TransactionEncodeRequest.builder()
                .walletAddress(TEST_WALLET)
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("buy")
                .useMainnet(true)
                .build();

        CompletableFuture<TransactionEncodeResponse> future = raydiumAdapter.encodeTransaction(request);
        
        Assertions.assertDoesNotThrow(() -> {
            TransactionEncodeResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            Assertions.assertNotNull(response);
            Assertions.assertTrue(response.isSuccessful(), "交易编码应该成功: " + response.getError());
            Assertions.assertEquals("raydium", response.getDexType());
            Assertions.assertNotNull(response.getMetadata(), "Raydium应该返回元数据");
            
            log.info("Raydium交易编码成功: {}", response.getTransactionId());
        });
    }

    /**
     * 测试并发交易编码
     */
    @Test
    @Order(5)
    @DisplayName("测试并发交易编码")
    void testConcurrentTransactionEncoding() {
        log.info("开始测试并发交易编码");
        
        TransactionEncodeRequest request = TransactionEncodeRequest.builder()
                .walletAddress(TEST_WALLET)
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("buy")
                .useMainnet(true)
                .build();

        // 并发执行三个DEX的交易编码
        CompletableFuture<TransactionEncodeResponse> pumpfunFuture = pumpfunAdapter.encodeTransaction(request);
        CompletableFuture<TransactionEncodeResponse> pumpswapFuture = pumpSwapAdapter.encodeTransaction(request);
        CompletableFuture<TransactionEncodeResponse> raydiumFuture = raydiumAdapter.encodeTransaction(request);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(pumpfunFuture, pumpswapFuture, raydiumFuture);
        
        Assertions.assertDoesNotThrow(() -> {
            allFutures.get(TIMEOUT_SECONDS * 3, TimeUnit.SECONDS);
            
            TransactionEncodeResponse pumpfunResponse = pumpfunFuture.get();
            TransactionEncodeResponse pumpswapResponse = pumpswapFuture.get();
            TransactionEncodeResponse raydiumResponse = raydiumFuture.get();
            
            // 验证所有响应都成功
            Assertions.assertTrue(pumpfunResponse.isSuccessful(), "Pumpfun并发编码失败");
            Assertions.assertTrue(pumpswapResponse.isSuccessful(), "PumpSwap并发编码失败");
            Assertions.assertTrue(raydiumResponse.isSuccessful(), "Raydium并发编码失败");
            
            log.info("并发交易编码测试成功");
            log.info("- Pumpfun: {}", pumpfunResponse.getTransactionId());
            log.info("- PumpSwap: {}", pumpswapResponse.getTransactionId());
            log.info("- Raydium: {}", raydiumResponse.getTransactionId());
        });
    }

    /**
     * 测试无效参数处理
     */
    @Test
    @Order(6)
    @DisplayName("测试无效参数处理")
    void testInvalidParameterHandling() {
        log.info("开始测试无效参数处理");
        
        // 测试无效钱包地址
        TransactionEncodeRequest invalidWalletRequest = TransactionEncodeRequest.builder()
                .walletAddress("invalid_wallet_address")
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("buy")
                .useMainnet(true)
                .build();

        CompletableFuture<TransactionEncodeResponse> future = pumpfunAdapter.encodeTransaction(invalidWalletRequest);
        
        Assertions.assertDoesNotThrow(() -> {
            TransactionEncodeResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            Assertions.assertNotNull(response);
            Assertions.assertFalse(response.isSuccessful(), "无效钱包地址应该导致编码失败");
            Assertions.assertNotNull(response.getError(), "应该返回错误信息");
            Assertions.assertEquals("VALIDATION_ERROR", response.getErrorCode());
            
            log.info("无效参数处理测试成功: {}", response.getError());
        });
    }

    /**
     * 测试网络连接和RPC调用
     */
    @Test
    @Order(7)
    @DisplayName("测试网络连接和RPC调用")
    void testNetworkConnectionAndRpcCalls() {
        log.info("开始测试网络连接和RPC调用");
        
        Assertions.assertDoesNotThrow(() -> {
            // 测试获取最新区块哈希
            String blockhash = solanaTransactionService.getLatestBlockhash(true).get(30, TimeUnit.SECONDS);
            Assertions.assertNotNull(blockhash, "区块哈希不能为空");
            Assertions.assertTrue(blockhash.length() > 0, "区块哈希长度应该大于0");
            log.info("获取最新区块哈希成功: {}", blockhash);
            
            // 测试账户存在性检查
            Boolean accountExists = solanaTransactionService.accountExists(TEST_WALLET, true).get(30, TimeUnit.SECONDS);
            Assertions.assertNotNull(accountExists, "账户存在性检查结果不能为空");
            log.info("账户存在性检查完成: {}", accountExists);
            
            // 测试地址验证
            boolean isValidWallet = solanaTransactionService.isValidWalletAddress(TEST_WALLET);
            Assertions.assertTrue(isValidWallet, "测试钱包地址应该有效");
            
            boolean isValidToken = solanaTransactionService.isValidTokenAddress(TEST_TOKEN_MINT);
            Assertions.assertTrue(isValidToken, "测试代币地址应该有效");
            
            log.info("网络连接和RPC调用测试成功");
        });
    }

    /**
     * 验证网络连接
     */
    private void verifyNetworkConnection() {
        try {
            String blockhash = solanaTransactionService.getLatestBlockhash(true).get(10, TimeUnit.SECONDS);
            log.info("网络连接正常，最新区块哈希: {}", blockhash.substring(0, 8) + "...");
        } catch (Exception e) {
            log.error("网络连接失败", e);
            Assertions.fail("无法连接到Solana主网: " + e.getMessage());
        }
    }

    /**
     * 验证钱包余额
     */
    private void verifyWalletBalance() {
        try {
            Boolean accountExists = solanaTransactionService.accountExists(TEST_WALLET, true).get(10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(accountExists)) {
                log.info("测试钱包账户存在: {}", TEST_WALLET);
            } else {
                log.warn("测试钱包账户不存在或余额为0: {}", TEST_WALLET);
            }
        } catch (Exception e) {
            log.warn("无法验证钱包余额: {}", e.getMessage());
        }
    }

    /**
     * 验证交易数据格式
     * @param transactionData Base64编码的交易数据
     */
    private void validateTransactionData(String transactionData) {
        Assertions.assertNotNull(transactionData, "交易数据不能为空");
        Assertions.assertTrue(transactionData.length() > 0, "交易数据长度应该大于0");
        
        // 验证Base64格式
        try {
            java.util.Base64.getDecoder().decode(transactionData);
            log.debug("交易数据Base64格式验证通过");
        } catch (IllegalArgumentException e) {
            Assertions.fail("交易数据不是有效的Base64格式: " + e.getMessage());
        }
    }

    /**
     * 性能测试：测量交易编码耗时
     */
    @Test
    @Order(8)
    @DisplayName("性能测试：交易编码耗时")
    void testTransactionEncodingPerformance() {
        log.info("开始性能测试：交易编码耗时");
        
        TransactionEncodeRequest request = TransactionEncodeRequest.builder()
                .walletAddress(TEST_WALLET)
                .tokenMint(TEST_TOKEN_MINT)
                .amount(TEST_AMOUNT)
                .slippage(TEST_SLIPPAGE)
                .operation("buy")
                .useMainnet(true)
                .build();

        // 测试Pumpfun编码性能
        long startTime = System.currentTimeMillis();
        CompletableFuture<TransactionEncodeResponse> future = pumpfunAdapter.encodeTransaction(request);
        
        Assertions.assertDoesNotThrow(() -> {
            TransactionEncodeResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            Assertions.assertTrue(response.isSuccessful(), "性能测试交易编码应该成功");
            Assertions.assertTrue(duration < 10000, "交易编码耗时应该小于10秒，实际耗时: " + duration + "ms");
            
            log.info("Pumpfun交易编码性能测试完成:");
            log.info("- 耗时: {}ms", duration);
            log.info("- 交易ID: {}", response.getTransactionId());
        });
    }
}