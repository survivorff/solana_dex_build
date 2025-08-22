package com.solana.dex.util;

import com.solana.dex.dto.TransactionEncodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.keypair.Keypair;
import software.sava.core.tx.Transaction;
import software.sava.rpc.json.http.client.SolanaRpcClient;
import software.sava.rpc.json.http.response.TransactionResponse;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 主网交易测试工具类
 * 提供实际上链交易的测试功能
 * 
 * 警告：此类包含真实的区块链交易功能，使用时请确保：
 * 1. 在测试环境中使用
 * 2. 使用测试钱包和少量资金
 * 3. 理解可能产生的费用
 */
@Component
@Slf4j
public class MainnetTransactionTester {

    @Autowired
    private SolanaRpcClient mainnetRpcClient;

    @Value("${test.wallet.private-key:}")
    private String testWalletPrivateKey;

    @Value("${test.network.enable-onchain-test:false}")
    private boolean enableOnchainTest;

    @Value("${test.parameters.timeout-seconds:60}")
    private int timeoutSeconds;

    /**
     * 执行实际的链上交易测试
     * @param response 交易编码响应
     * @return 交易签名
     */
    public CompletableFuture<String> executeOnchainTransaction(TransactionEncodeResponse response) {
        return CompletableFuture.supplyAsync(() -> {
            if (!enableOnchainTest) {
                log.warn("链上交易测试已禁用，跳过实际交易执行");
                return "SIMULATED_" + response.getTransactionId();
            }

            if (testWalletPrivateKey == null || testWalletPrivateKey.isEmpty()) {
                log.error("测试钱包私钥未配置，无法执行链上交易");
                throw new RuntimeException("Test wallet private key not configured");
            }

            try {
                log.info("开始执行链上交易，交易ID: {}", response.getTransactionId());

                // 解码交易数据
                byte[] transactionBytes = Base64.getDecoder().decode(response.getTransactionData());
                Transaction transaction = Transaction.fromBytes(transactionBytes);

                // 创建测试钱包密钥对
                Keypair testKeypair = Keypair.fromSecretKey(Base64.getDecoder().decode(testWalletPrivateKey));

                // 签名交易
                transaction.sign(testKeypair);

                // 发送交易到区块链
                String signature = mainnetRpcClient.sendTransaction(transaction).join();
                log.info("交易已发送到区块链，签名: {}", signature);

                // 等待交易确认
                boolean confirmed = waitForTransactionConfirmation(signature);
                if (confirmed) {
                    log.info("交易确认成功，签名: {}", signature);
                    return signature;
                } else {
                    log.warn("交易确认超时，签名: {}", signature);
                    return signature; // 即使超时也返回签名，可能稍后确认
                }

            } catch (Exception e) {
                log.error("链上交易执行失败，交易ID: {}", response.getTransactionId(), e);
                throw new RuntimeException("Failed to execute onchain transaction: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 模拟交易执行（不实际上链）
     * @param response 交易编码响应
     * @return 模拟结果
     */
    public CompletableFuture<TransactionSimulationResult> simulateTransaction(TransactionEncodeResponse response) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始模拟交易执行，交易ID: {}", response.getTransactionId());

                // 解码交易数据
                byte[] transactionBytes = Base64.getDecoder().decode(response.getTransactionData());
                Transaction transaction = Transaction.fromBytes(transactionBytes);

                // 使用RPC客户端模拟交易
                // 注意：这里需要根据sava库的实际API进行调整
                // var simulationResult = mainnetRpcClient.simulateTransaction(transaction).join();

                // 临时模拟结果
                TransactionSimulationResult result = TransactionSimulationResult.builder()
                        .success(true)
                        .transactionId(response.getTransactionId())
                        .estimatedFee(response.getEstimatedFee().longValue())
                        .logs(java.util.Arrays.asList(
                                "Program " + response.getDexType() + " invoke [1]",
                                "Program " + response.getDexType() + " success"
                        ))
                        .computeUnitsConsumed(50000L)
                        .build();

                log.info("交易模拟完成，结果: {}", result.isSuccess() ? "成功" : "失败");
                return result;

            } catch (Exception e) {
                log.error("交易模拟失败，交易ID: {}", response.getTransactionId(), e);
                return TransactionSimulationResult.builder()
                        .success(false)
                        .transactionId(response.getTransactionId())
                        .error(e.getMessage())
                        .build();
            }
        });
    }

    /**
     * 验证交易编码的正确性
     * @param response 交易编码响应
     * @return 验证结果
     */
    public TransactionValidationResult validateTransactionEncoding(TransactionEncodeResponse response) {
        try {
            log.debug("开始验证交易编码，交易ID: {}", response.getTransactionId());

            // 基础验证
            if (response.getTransactionData() == null || response.getTransactionData().isEmpty()) {
                return TransactionValidationResult.failure("交易数据为空");
            }

            // Base64格式验证
            byte[] transactionBytes;
            try {
                transactionBytes = Base64.getDecoder().decode(response.getTransactionData());
            } catch (IllegalArgumentException e) {
                return TransactionValidationResult.failure("交易数据不是有效的Base64格式");
            }

            // 交易结构验证
            try {
                Transaction transaction = Transaction.fromBytes(transactionBytes);
                
                // 验证交易基本结构
                if (transaction.instructions().isEmpty()) {
                    return TransactionValidationResult.failure("交易不包含任何指令");
                }

                // 验证费用支付者
                PublicKey feePayer = transaction.feePayer();
                if (feePayer == null) {
                    return TransactionValidationResult.failure("交易缺少费用支付者");
                }

                // 验证区块哈希
                String recentBlockhash = transaction.recentBlockhash();
                if (recentBlockhash == null || recentBlockhash.isEmpty()) {
                    return TransactionValidationResult.failure("交易缺少最新区块哈希");
                }

                log.debug("交易编码验证通过，指令数量: {}, 费用支付者: {}", 
                         transaction.instructions().size(), feePayer.toBase58());

                return TransactionValidationResult.success(
                        "交易编码验证通过",
                        transaction.instructions().size(),
                        feePayer.toBase58(),
                        recentBlockhash
                );

            } catch (Exception e) {
                return TransactionValidationResult.failure("交易数据解析失败: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("交易编码验证异常，交易ID: {}", response.getTransactionId(), e);
            return TransactionValidationResult.failure("验证过程发生异常: " + e.getMessage());
        }
    }

    /**
     * 等待交易确认
     * @param signature 交易签名
     * @return 是否确认成功
     */
    private boolean waitForTransactionConfirmation(String signature) {
        try {
            log.info("等待交易确认，签名: {}", signature);
            
            int maxAttempts = timeoutSeconds;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                try {
                    TransactionResponse txResponse = mainnetRpcClient.getTransaction(signature).join();
                    if (txResponse != null && txResponse.meta() != null) {
                        if (txResponse.meta().err() == null) {
                            log.info("交易确认成功，签名: {}, 尝试次数: {}", signature, attempt + 1);
                            return true;
                        } else {
                            log.error("交易执行失败，签名: {}, 错误: {}", signature, txResponse.meta().err());
                            return false;
                        }
                    }
                } catch (Exception e) {
                    log.debug("交易确认检查失败，尝试次数: {}, 错误: {}", attempt + 1, e.getMessage());
                }

                // 等待1秒后重试
                Thread.sleep(1000);
            }

            log.warn("交易确认超时，签名: {}, 最大尝试次数: {}", signature, maxAttempts);
            return false;

        } catch (Exception e) {
            log.error("等待交易确认时发生异常，签名: {}", signature, e);
            return false;
        }
    }

    /**
     * 检查测试环境配置
     * @return 配置检查结果
     */
    public TestEnvironmentStatus checkTestEnvironment() {
        TestEnvironmentStatus.TestEnvironmentStatusBuilder builder = TestEnvironmentStatus.builder();

        // 检查RPC连接
        try {
            String blockhash = mainnetRpcClient.getLatestBlockhash().get(10, TimeUnit.SECONDS).blockhash();
            builder.rpcConnected(true).latestBlockhash(blockhash);
            log.info("RPC连接正常，最新区块哈希: {}", blockhash.substring(0, 8) + "...");
        } catch (Exception e) {
            builder.rpcConnected(false).rpcError(e.getMessage());
            log.error("RPC连接失败", e);
        }

        // 检查钱包配置
        boolean walletConfigured = testWalletPrivateKey != null && !testWalletPrivateKey.isEmpty();
        builder.walletConfigured(walletConfigured);
        if (!walletConfigured) {
            log.warn("测试钱包私钥未配置");
        }

        // 检查链上测试开关
        builder.onchainTestEnabled(enableOnchainTest);
        log.info("链上测试开关: {}", enableOnchainTest ? "启用" : "禁用");

        return builder.build();
    }

    /**
     * 交易模拟结果
     */
    @lombok.Data
    @lombok.Builder
    public static class TransactionSimulationResult {
        private boolean success;
        private String transactionId;
        private Long estimatedFee;
        private java.util.List<String> logs;
        private Long computeUnitsConsumed;
        private String error;
    }

    /**
     * 交易验证结果
     */
    @lombok.Data
    @lombok.Builder
    public static class TransactionValidationResult {
        private boolean valid;
        private String message;
        private Integer instructionCount;
        private String feePayer;
        private String recentBlockhash;
        private String error;

        public static TransactionValidationResult success(String message, int instructionCount, 
                                                         String feePayer, String recentBlockhash) {
            return TransactionValidationResult.builder()
                    .valid(true)
                    .message(message)
                    .instructionCount(instructionCount)
                    .feePayer(feePayer)
                    .recentBlockhash(recentBlockhash)
                    .build();
        }

        public static TransactionValidationResult failure(String error) {
            return TransactionValidationResult.builder()
                    .valid(false)
                    .error(error)
                    .build();
        }
    }

    /**
     * 测试环境状态
     */
    @lombok.Data
    @lombok.Builder
    public static class TestEnvironmentStatus {
        private boolean rpcConnected;
        private String latestBlockhash;
        private String rpcError;
        private boolean walletConfigured;
        private boolean onchainTestEnabled;

        public boolean isReady() {
            return rpcConnected && walletConfigured;
        }
    }
}