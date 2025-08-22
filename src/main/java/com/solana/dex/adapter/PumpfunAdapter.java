package com.solana.dex.adapter;

import com.solana.dex.config.SolanaConfig;
import com.solana.dex.dto.TransactionEncodeRequest;
import com.solana.dex.dto.TransactionEncodeResponse;
import com.solana.dex.service.SolanaTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.sava.core.accounts.PublicKey;
import software.sava.core.tx.Instruction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

/**
 * Pumpfun DEX适配器
 * 实现Pumpfun平台的交易编码逻辑
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PumpfunAdapter {

    private final SolanaTransactionService solanaTransactionService;
    private final SolanaConfig solanaConfig;

    // Pumpfun程序指令类型
    private static final byte INSTRUCTION_BUY = 0;
    private static final byte INSTRUCTION_SELL = 1;
    private static final byte INSTRUCTION_CREATE_POOL = 2;
    private static final byte INSTRUCTION_ADD_LIQUIDITY = 3;

    // Pumpfun相关常量
    private static final String DEX_NAME = "pumpfun";
    private static final long LAMPORTS_PER_SOL = 1_000_000_000L;

    /**
     * 编码Pumpfun交易
     * @param request 交易请求
     * @return 编码响应
     */
    public CompletableFuture<TransactionEncodeResponse> encodeTransaction(TransactionEncodeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始编码Pumpfun交易，操作: {}, 钱包: {}, 代币: {}, 数量: {}", 
                        request.getOperation(), request.getWalletAddress(), 
                        request.getTokenMint(), request.getAmount());

                // 验证请求参数
                String validationError = validateRequest(request);
                if (validationError != null) {
                    return TransactionEncodeResponse.failure(validationError, "VALIDATION_ERROR")
                            .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                    request.getTokenMint(), request.getAmount(), request.getSlippage());
                }

                // 创建交易指令
                Instruction instruction = createPumpfunInstruction(request);

                // 编码交易
                String encodedTransaction = solanaTransactionService
                        .encodeTransaction(request.getWalletAddress(), instruction, request.getUseMainnet())
                        .join();

                // 估算费用
                long estimatedFeeLamports = solanaTransactionService
                        .estimateTransactionFee(java.util.List.of(instruction), request.getUseMainnet())
                        .join();

                BigDecimal estimatedFeeSol = BigDecimal.valueOf(estimatedFeeLamports)
                        .divide(BigDecimal.valueOf(LAMPORTS_PER_SOL), 9, BigDecimal.ROUND_HALF_UP);

                // 生成交易ID
                String transactionId = generateTransactionId(request);

                log.info("Pumpfun交易编码成功，交易ID: {}, 预估费用: {} SOL", transactionId, estimatedFeeSol);

                return TransactionEncodeResponse.success(encodedTransaction, estimatedFeeSol, transactionId)
                        .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                request.getTokenMint(), request.getAmount(), request.getSlippage());

            } catch (Exception e) {
                log.error("Pumpfun交易编码失败", e);
                return TransactionEncodeResponse.failure("交易编码失败: " + e.getMessage(), "ENCODING_ERROR")
                        .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                request.getTokenMint(), request.getAmount(), request.getSlippage());
            }
        });
    }

    /**
     * 创建Pumpfun交易指令
     * @param request 交易请求
     * @return 交易指令
     */
    private Instruction createPumpfunInstruction(TransactionEncodeRequest request) {
        try {
            // 获取Pumpfun程序ID
            PublicKey programId = solanaConfig.getDexProgramId(DEX_NAME);
            PublicKey wallet = PublicKey.fromBase58(request.getWalletAddress());
            PublicKey tokenMint = PublicKey.fromBase58(request.getTokenMint());

            // 构建指令数据
            byte[] instructionData = buildInstructionData(request);

            // 创建指令构建器
            Instruction.Builder builder = solanaTransactionService.createInstructionBuilder(programId)
                    .accounts(
                            // 用户钱包账户（签名者）
                            Instruction.AccountMeta.createSigner(wallet),
                            // 代币账户
                            Instruction.AccountMeta.createWritable(tokenMint),
                            // 用户代币账户（需要创建或获取）
                            Instruction.AccountMeta.createWritable(getUserTokenAccount(wallet, tokenMint)),
                            // Pumpfun池账户
                            Instruction.AccountMeta.createWritable(getPumpfunPoolAccount(tokenMint)),
                            // 系统程序
                            Instruction.AccountMeta.create(PublicKey.fromBase58("11111111111111111111111111111112")),
                            // Token程序
                            Instruction.AccountMeta.create(PublicKey.fromBase58("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")),
                            // 关联Token程序
                            Instruction.AccountMeta.create(PublicKey.fromBase58("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"))
                    )
                    .data(instructionData);

            return builder.build();

        } catch (Exception e) {
            log.error("创建Pumpfun指令失败", e);
            throw new RuntimeException("Failed to create Pumpfun instruction: " + e.getMessage(), e);
        }
    }

    /**
     * 构建指令数据
     * @param request 交易请求
     * @return 指令数据字节数组
     */
    private byte[] buildInstructionData(TransactionEncodeRequest request) {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);

        // 指令类型
        if (request.isBuyOperation()) {
            buffer.put(INSTRUCTION_BUY);
        } else {
            buffer.put(INSTRUCTION_SELL);
        }

        // 交易数量
        long amount = request.getAmountAsLong();
        buffer.putLong(amount);

        // 滑点容忍度（以基点为单位，1% = 100基点）
        int slippageBps = (int) (request.getSlippageAsDecimal() * 10000);
        buffer.putInt(slippageBps);

        // 优先费用
        long priorityFee = request.getPriorityFee() != null ? request.getPriorityFee() : 0L;
        buffer.putLong(priorityFee);

        // 最大等待时间
        int maxWaitTime = request.getMaxWaitTime() != null ? request.getMaxWaitTime() : 30;
        buffer.putInt(maxWaitTime);

        // 填充剩余字节
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }

        return buffer.array();
    }

    /**
     * 获取用户代币账户地址
     * @param wallet 用户钱包地址
     * @param tokenMint 代币地址
     * @return 用户代币账户地址
     */
    private PublicKey getUserTokenAccount(PublicKey wallet, PublicKey tokenMint) {
        try {
            // 计算关联代币账户地址
            // 这里使用简化的计算方式，实际应该使用Solana的关联代币账户推导算法
            String seed = wallet.toBase58() + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            
            // 简化的地址生成（实际项目中应使用正确的PDA推导）
            return PublicKey.fromSeed(wallet, seedBytes);
            
        } catch (Exception e) {
            log.warn("计算用户代币账户失败，使用默认地址", e);
            return wallet; // 临时返回钱包地址
        }
    }

    /**
     * 获取Pumpfun池账户地址
     * @param tokenMint 代币地址
     * @return 池账户地址
     */
    private PublicKey getPumpfunPoolAccount(PublicKey tokenMint) {
        try {
            // 计算Pumpfun池账户地址
            // 这里使用简化的计算方式，实际应该使用Pumpfun的池地址推导算法
            String seed = "pumpfun_pool_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            
            PublicKey programId = solanaConfig.getDexProgramId(DEX_NAME);
            return PublicKey.fromSeed(programId, seedBytes);
            
        } catch (Exception e) {
            log.warn("计算Pumpfun池账户失败，使用默认地址", e);
            return tokenMint; // 临时返回代币地址
        }
    }

    /**
     * 验证交易请求
     * @param request 交易请求
     * @return 验证错误信息，null表示验证通过
     */
    private String validateRequest(TransactionEncodeRequest request) {
        // 基础验证
        String basicValidation = request.validate();
        if (basicValidation != null) {
            return basicValidation;
        }

        // Pumpfun特定验证
        SolanaConfig.DexProperties config = solanaConfig.getDexConfig(DEX_NAME);
        
        // 验证最小交易数量
        if (request.getAmountAsLong() < config.getMinAmount()) {
            return String.format("交易数量不能小于 %d", config.getMinAmount());
        }

        // 验证滑点范围
        if (!solanaConfig.isSlippageValid(DEX_NAME, request.getSlippage().doubleValue())) {
            return String.format("滑点必须在0-%s%%之间", config.getMaxSlippage());
        }

        // 验证地址格式
        if (!solanaTransactionService.isValidWalletAddress(request.getWalletAddress())) {
            return "无效的钱包地址格式";
        }

        if (!solanaTransactionService.isValidTokenAddress(request.getTokenMint())) {
            return "无效的代币地址格式";
        }

        return null; // 验证通过
    }

    /**
     * 生成交易ID
     * @param request 交易请求
     * @return 交易ID
     */
    private String generateTransactionId(TransactionEncodeRequest request) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String hash = String.valueOf((request.getWalletAddress() + request.getTokenMint() + 
                request.getAmount() + request.getOperation()).hashCode());
        return "pumpfun_" + timestamp + "_" + Math.abs(Integer.parseInt(hash));
    }

    /**
     * 获取DEX名称
     * @return DEX名称
     */
    public String getDexName() {
        return DEX_NAME;
    }

    /**
     * 检查是否支持指定操作
     * @param operation 操作类型
     * @return true if supported, false otherwise
     */
    public boolean supportsOperation(String operation) {
        return "buy".equalsIgnoreCase(operation) || "sell".equalsIgnoreCase(operation);
    }
}