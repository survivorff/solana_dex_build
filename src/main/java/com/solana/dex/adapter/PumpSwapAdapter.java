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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

/**
 * PumpSwap DEX适配器
 * 实现PumpSwap平台的交易编码逻辑
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PumpSwapAdapter {

    private final SolanaTransactionService solanaTransactionService;
    private final SolanaConfig solanaConfig;

    // PumpSwap程序指令类型
    private static final byte INSTRUCTION_SWAP = 0;
    private static final byte INSTRUCTION_ADD_LIQUIDITY = 1;
    private static final byte INSTRUCTION_REMOVE_LIQUIDITY = 2;
    private static final byte INSTRUCTION_CREATE_PAIR = 3;

    // PumpSwap相关常量
    private static final String DEX_NAME = "pumpswap";
    private static final long LAMPORTS_PER_SOL = 1_000_000_000L;

    /**
     * 编码PumpSwap交易
     * @param request 交易请求
     * @return 编码响应
     */
    public CompletableFuture<TransactionEncodeResponse> encodeTransaction(TransactionEncodeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始编码PumpSwap交易，操作: {}, 钱包: {}, 代币: {}, 数量: {}", 
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
                Instruction instruction = createPumpSwapInstruction(request);

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

                log.info("PumpSwap交易编码成功，交易ID: {}, 预估费用: {} SOL", transactionId, estimatedFeeSol);

                return TransactionEncodeResponse.success(encodedTransaction, estimatedFeeSol, transactionId)
                        .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                request.getTokenMint(), request.getAmount(), request.getSlippage());

            } catch (Exception e) {
                log.error("PumpSwap交易编码失败", e);
                return TransactionEncodeResponse.failure("交易编码失败: " + e.getMessage(), "ENCODING_ERROR")
                        .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                request.getTokenMint(), request.getAmount(), request.getSlippage());
            }
        });
    }

    /**
     * 创建PumpSwap交易指令
     * @param request 交易请求
     * @return 交易指令
     */
    private Instruction createPumpSwapInstruction(TransactionEncodeRequest request) {
        try {
            // 获取PumpSwap程序ID
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
                            // 代币A账户
                            Instruction.AccountMeta.createWritable(tokenMint),
                            // 代币B账户（SOL或USDC）
                            Instruction.AccountMeta.createWritable(getBaseTokenAccount()),
                            // 用户代币A账户
                            Instruction.AccountMeta.createWritable(getUserTokenAccount(wallet, tokenMint)),
                            // 用户代币B账户
                            Instruction.AccountMeta.createWritable(getUserTokenAccount(wallet, getBaseTokenAccount())),
                            // PumpSwap池账户
                            Instruction.AccountMeta.createWritable(getPumpSwapPoolAccount(tokenMint)),
                            // 池代币A账户
                            Instruction.AccountMeta.createWritable(getPoolTokenAccount(tokenMint, true)),
                            // 池代币B账户
                            Instruction.AccountMeta.createWritable(getPoolTokenAccount(tokenMint, false)),
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
            log.error("创建PumpSwap指令失败", e);
            throw new RuntimeException("Failed to create PumpSwap instruction: " + e.getMessage(), e);
        }
    }

    /**
     * 构建指令数据
     * @param request 交易请求
     * @return 指令数据字节数组
     */
    private byte[] buildInstructionData(TransactionEncodeRequest request) {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);

        // 指令类型（PumpSwap使用swap指令）
        buffer.put(INSTRUCTION_SWAP);

        // 交易数量
        long amount = request.getAmountAsLong();
        buffer.putLong(amount);

        // 最小输出数量（考虑滑点）
        double slippageDecimal = request.getSlippageAsDecimal();
        long minAmountOut = (long) (amount * (1.0 - slippageDecimal));
        buffer.putLong(minAmountOut);

        // 交易方向（0=买入，1=卖出）
        byte direction = request.isBuyOperation() ? (byte) 0 : (byte) 1;
        buffer.put(direction);

        // 优先费用
        long priorityFee = request.getPriorityFee() != null ? request.getPriorityFee() : 0L;
        buffer.putLong(priorityFee);

        // 截止时间（当前时间 + 最大等待时间）
        long deadline = System.currentTimeMillis() / 1000 + 
                       (request.getMaxWaitTime() != null ? request.getMaxWaitTime() : 30);
        buffer.putLong(deadline);

        // 填充剩余字节
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }

        return buffer.array();
    }

    /**
     * 获取基础代币账户（通常是SOL或USDC）
     * @return 基础代币账户地址
     */
    private PublicKey getBaseTokenAccount() {
        // 使用USDC作为基础代币
        return PublicKey.fromBase58("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");
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
            String seed = "user_token_" + wallet.toBase58() + "_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(wallet, seedBytes);
        } catch (Exception e) {
            log.warn("计算用户代币账户失败，使用默认地址", e);
            return wallet;
        }
    }

    /**
     * 获取PumpSwap池账户地址
     * @param tokenMint 代币地址
     * @return 池账户地址
     */
    private PublicKey getPumpSwapPoolAccount(PublicKey tokenMint) {
        try {
            String seed = "pumpswap_pool_" + tokenMint.toBase58() + "_" + getBaseTokenAccount().toBase58();
            byte[] seedBytes = seed.getBytes();
            PublicKey programId = solanaConfig.getDexProgramId(DEX_NAME);
            return PublicKey.fromSeed(programId, seedBytes);
        } catch (Exception e) {
            log.warn("计算PumpSwap池账户失败，使用默认地址", e);
            return tokenMint;
        }
    }

    /**
     * 获取池代币账户地址
     * @param tokenMint 代币地址
     * @param isTokenA 是否为代币A
     * @return 池代币账户地址
     */
    private PublicKey getPoolTokenAccount(PublicKey tokenMint, boolean isTokenA) {
        try {
            String tokenType = isTokenA ? "token_a" : "token_b";
            String seed = "pool_" + tokenType + "_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            PublicKey programId = solanaConfig.getDexProgramId(DEX_NAME);
            return PublicKey.fromSeed(programId, seedBytes);
        } catch (Exception e) {
            log.warn("计算池代币账户失败，使用默认地址", e);
            return tokenMint;
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

        // PumpSwap特定验证
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

        return null;
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
        return "pumpswap_" + timestamp + "_" + Math.abs(Integer.parseInt(hash));
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

    /**
     * 获取支持的操作列表
     * @return 支持的操作列表
     */
    public java.util.List<String> getSupportedOperations() {
        return java.util.Arrays.asList("buy", "sell", "add_liquidity", "remove_liquidity");
    }
}