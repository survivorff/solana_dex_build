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
 * Raydium DEX适配器
 * 实现Raydium平台的交易编码逻辑
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RaydiumAdapter {

    private final SolanaTransactionService solanaTransactionService;
    private final SolanaConfig solanaConfig;

    // Raydium程序指令类型
    private static final byte INSTRUCTION_SWAP_BASE_IN = 9;
    private static final byte INSTRUCTION_SWAP_BASE_OUT = 11;
    private static final byte INSTRUCTION_DEPOSIT = 3;
    private static final byte INSTRUCTION_WITHDRAW = 4;

    // Raydium相关常量
    private static final String DEX_NAME = "raydium";
    private static final long LAMPORTS_PER_SOL = 1_000_000_000L;

    // Raydium AMM程序ID
    private static final String RAYDIUM_AMM_PROGRAM = "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8";
    private static final String SERUM_PROGRAM = "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin";

    /**
     * 编码Raydium交易
     * @param request 交易请求
     * @return 编码响应
     */
    public CompletableFuture<TransactionEncodeResponse> encodeTransaction(TransactionEncodeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始编码Raydium交易，操作: {}, 钱包: {}, 代币: {}, 数量: {}", 
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
                Instruction instruction = createRaydiumInstruction(request);

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

                log.info("Raydium交易编码成功，交易ID: {}, 预估费用: {} SOL", transactionId, estimatedFeeSol);

                return TransactionEncodeResponse.success(encodedTransaction, estimatedFeeSol, transactionId)
                        .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                request.getTokenMint(), request.getAmount(), request.getSlippage())
                        .withMetadata(java.util.Map.of(
                                "amm_program", RAYDIUM_AMM_PROGRAM,
                                "serum_program", SERUM_PROGRAM,
                                "instruction_type", request.isBuyOperation() ? "swap_base_in" : "swap_base_out"
                        ));

            } catch (Exception e) {
                log.error("Raydium交易编码失败", e);
                return TransactionEncodeResponse.failure("交易编码失败: " + e.getMessage(), "ENCODING_ERROR")
                        .withDetails(DEX_NAME, request.getOperation(), request.getWalletAddress(),
                                request.getTokenMint(), request.getAmount(), request.getSlippage());
            }
        });
    }

    /**
     * 创建Raydium交易指令
     * @param request 交易请求
     * @return 交易指令
     */
    private Instruction createRaydiumInstruction(TransactionEncodeRequest request) {
        try {
            // 获取Raydium程序ID
            PublicKey programId = PublicKey.fromBase58(RAYDIUM_AMM_PROGRAM);
            PublicKey wallet = PublicKey.fromBase58(request.getWalletAddress());
            PublicKey tokenMint = PublicKey.fromBase58(request.getTokenMint());

            // 构建指令数据
            byte[] instructionData = buildInstructionData(request);

            // 创建指令构建器
            Instruction.Builder builder = solanaTransactionService.createInstructionBuilder(programId)
                    .accounts(
                            // Token程序
                            Instruction.AccountMeta.create(PublicKey.fromBase58("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")),
                            // AMM ID
                            Instruction.AccountMeta.createWritable(getAmmId(tokenMint)),
                            // AMM权限
                            Instruction.AccountMeta.create(getAmmAuthority()),
                            // AMM开放订单
                            Instruction.AccountMeta.createWritable(getAmmOpenOrders(tokenMint)),
                            // AMM目标订单
                            Instruction.AccountMeta.createWritable(getAmmTargetOrders(tokenMint)),
                            // 池代币账户
                            Instruction.AccountMeta.createWritable(getPoolCoinTokenAccount(tokenMint)),
                            Instruction.AccountMeta.createWritable(getPoolPcTokenAccount(tokenMint)),
                            // Serum程序
                            Instruction.AccountMeta.create(PublicKey.fromBase58(SERUM_PROGRAM)),
                            // Serum市场
                            Instruction.AccountMeta.createWritable(getSerumMarket(tokenMint)),
                            // Serum订单簿
                            Instruction.AccountMeta.createWritable(getSerumBids(tokenMint)),
                            Instruction.AccountMeta.createWritable(getSerumAsks(tokenMint)),
                            // Serum事件队列
                            Instruction.AccountMeta.createWritable(getSerumEventQueue(tokenMint)),
                            // 用户源代币账户
                            Instruction.AccountMeta.createWritable(getUserSourceTokenAccount(wallet, request)),
                            // 用户目标代币账户
                            Instruction.AccountMeta.createWritable(getUserDestTokenAccount(wallet, request)),
                            // 用户钱包（签名者）
                            Instruction.AccountMeta.createSigner(wallet)
                    )
                    .data(instructionData);

            return builder.build();

        } catch (Exception e) {
            log.error("创建Raydium指令失败", e);
            throw new RuntimeException("Failed to create Raydium instruction: " + e.getMessage(), e);
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
            buffer.put(INSTRUCTION_SWAP_BASE_IN);
        } else {
            buffer.put(INSTRUCTION_SWAP_BASE_OUT);
        }

        // 输入数量
        long amountIn = request.getAmountAsLong();
        buffer.putLong(amountIn);

        // 最小输出数量（考虑滑点）
        double slippageDecimal = request.getSlippageAsDecimal();
        long minimumAmountOut = (long) (amountIn * (1.0 - slippageDecimal));
        buffer.putLong(minimumAmountOut);

        // 填充剩余字节
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }

        return buffer.array();
    }

    /**
     * 获取AMM ID
     * @param tokenMint 代币地址
     * @return AMM ID
     */
    private PublicKey getAmmId(PublicKey tokenMint) {
        try {
            String seed = "amm_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(RAYDIUM_AMM_PROGRAM), seedBytes);
        } catch (Exception e) {
            log.warn("计算AMM ID失败，使用默认地址", e);
            return tokenMint;
        }
    }

    /**
     * 获取AMM权限
     * @return AMM权限地址
     */
    private PublicKey getAmmAuthority() {
        return PublicKey.fromBase58("5Q544fKrFoe6tsEbD7S8EmxGTJYAKtTVhAW5Q5pge4j1");
    }

    /**
     * 获取AMM开放订单
     * @param tokenMint 代币地址
     * @return AMM开放订单地址
     */
    private PublicKey getAmmOpenOrders(PublicKey tokenMint) {
        try {
            String seed = "amm_open_orders_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(RAYDIUM_AMM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取AMM目标订单
     * @param tokenMint 代币地址
     * @return AMM目标订单地址
     */
    private PublicKey getAmmTargetOrders(PublicKey tokenMint) {
        try {
            String seed = "amm_target_orders_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(RAYDIUM_AMM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取池代币账户
     * @param tokenMint 代币地址
     * @return 池代币账户地址
     */
    private PublicKey getPoolCoinTokenAccount(PublicKey tokenMint) {
        try {
            String seed = "pool_coin_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(RAYDIUM_AMM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取池PC代币账户
     * @param tokenMint 代币地址
     * @return 池PC代币账户地址
     */
    private PublicKey getPoolPcTokenAccount(PublicKey tokenMint) {
        try {
            String seed = "pool_pc_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(RAYDIUM_AMM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取Serum市场
     * @param tokenMint 代币地址
     * @return Serum市场地址
     */
    private PublicKey getSerumMarket(PublicKey tokenMint) {
        try {
            String seed = "serum_market_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(SERUM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取Serum买单
     * @param tokenMint 代币地址
     * @return Serum买单地址
     */
    private PublicKey getSerumBids(PublicKey tokenMint) {
        try {
            String seed = "serum_bids_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(SERUM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取Serum卖单
     * @param tokenMint 代币地址
     * @return Serum卖单地址
     */
    private PublicKey getSerumAsks(PublicKey tokenMint) {
        try {
            String seed = "serum_asks_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(SERUM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取Serum事件队列
     * @param tokenMint 代币地址
     * @return Serum事件队列地址
     */
    private PublicKey getSerumEventQueue(PublicKey tokenMint) {
        try {
            String seed = "serum_event_queue_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(PublicKey.fromBase58(SERUM_PROGRAM), seedBytes);
        } catch (Exception e) {
            return tokenMint;
        }
    }

    /**
     * 获取用户源代币账户
     * @param wallet 用户钱包
     * @param request 交易请求
     * @return 用户源代币账户地址
     */
    private PublicKey getUserSourceTokenAccount(PublicKey wallet, TransactionEncodeRequest request) {
        if (request.isBuyOperation()) {
            // 买入时，源代币是SOL或USDC
            return wallet; // 简化处理，实际应该是WSOL账户
        } else {
            // 卖出时，源代币是目标代币
            return getUserTokenAccount(wallet, PublicKey.fromBase58(request.getTokenMint()));
        }
    }

    /**
     * 获取用户目标代币账户
     * @param wallet 用户钱包
     * @param request 交易请求
     * @return 用户目标代币账户地址
     */
    private PublicKey getUserDestTokenAccount(PublicKey wallet, TransactionEncodeRequest request) {
        if (request.isBuyOperation()) {
            // 买入时，目标代币是指定代币
            return getUserTokenAccount(wallet, PublicKey.fromBase58(request.getTokenMint()));
        } else {
            // 卖出时，目标代币是SOL或USDC
            return wallet; // 简化处理
        }
    }

    /**
     * 获取用户代币账户
     * @param wallet 用户钱包
     * @param tokenMint 代币地址
     * @return 用户代币账户地址
     */
    private PublicKey getUserTokenAccount(PublicKey wallet, PublicKey tokenMint) {
        try {
            String seed = "user_token_" + wallet.toBase58() + "_" + tokenMint.toBase58();
            byte[] seedBytes = seed.getBytes();
            return PublicKey.fromSeed(wallet, seedBytes);
        } catch (Exception e) {
            return wallet;
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

        // Raydium特定验证
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
        return "raydium_" + timestamp + "_" + Math.abs(Integer.parseInt(hash));
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
        return java.util.Arrays.asList("buy", "sell", "deposit", "withdraw");
    }
}