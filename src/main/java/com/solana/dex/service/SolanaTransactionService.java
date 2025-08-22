package com.solana.dex.service;

import com.solana.dex.config.SolanaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.sava.core.accounts.PublicKey;
import software.sava.core.tx.Instruction;
import software.sava.core.tx.Transaction;
import software.sava.rpc.json.http.client.SolanaRpcClient;
import software.sava.rpc.json.http.response.AccountInfo;
import software.sava.rpc.json.http.response.LatestBlockhash;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Solana交易编码服务
 * 使用sava库实现交易的构建和编码
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolanaTransactionService {

    private final SolanaRpcClient mainnetRpcClient;
    private final SolanaRpcClient devnetRpcClient;
    private final SolanaConfig solanaConfig;

    /**
     * 创建并编码交易
     * @param walletAddress 钱包地址
     * @param instructions 交易指令列表
     * @param useMainnet 是否使用主网
     * @return 编码后的交易数据
     */
    public CompletableFuture<String> encodeTransaction(String walletAddress, 
                                                     List<Instruction> instructions, 
                                                     boolean useMainnet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始编码交易，钱包地址: {}, 指令数量: {}", walletAddress, instructions.size());
                
                // 选择RPC客户端
                SolanaRpcClient rpcClient = useMainnet ? mainnetRpcClient : devnetRpcClient;
                
                // 获取最新区块哈希
                LatestBlockhash latestBlockhash = rpcClient.getLatestBlockhash().join();
                log.debug("获取到最新区块哈希: {}", latestBlockhash.blockhash());
                
                // 创建钱包公钥
                PublicKey wallet = PublicKey.fromBase58(walletAddress);
                
                // 构建交易
                Transaction transaction = Transaction.createTx(
                    wallet, // 费用支付者
                    latestBlockhash.blockhash(), // 最新区块哈希
                    instructions.toArray(new Instruction[0]) // 指令数组
                );
                
                // 序列化交易
                byte[] serializedTx = transaction.serialized();
                String encodedTx = Base64.getEncoder().encodeToString(serializedTx);
                
                log.info("交易编码完成，编码长度: {} bytes", serializedTx.length);
                return encodedTx;
                
            } catch (Exception e) {
                log.error("交易编码失败，钱包地址: {}", walletAddress, e);
                throw new RuntimeException("Failed to encode transaction: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 创建单个指令的交易
     * @param walletAddress 钱包地址
     * @param instruction 交易指令
     * @param useMainnet 是否使用主网
     * @return 编码后的交易数据
     */
    public CompletableFuture<String> encodeTransaction(String walletAddress, 
                                                     Instruction instruction, 
                                                     boolean useMainnet) {
        return encodeTransaction(walletAddress, List.of(instruction), useMainnet);
    }

    /**
     * 估算交易费用
     * @param instructions 交易指令列表
     * @param useMainnet 是否使用主网
     * @return 预估费用（lamports）
     */
    public CompletableFuture<Long> estimateTransactionFee(List<Instruction> instructions, boolean useMainnet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 基础交易费用（5000 lamports）
                long baseFee = 5000L;
                
                // 每个指令的额外费用（估算）
                long instructionFee = instructions.size() * 1000L;
                
                // 计算总费用
                long totalFee = baseFee + instructionFee;
                
                log.debug("预估交易费用: {} lamports，指令数量: {}", totalFee, instructions.size());
                return totalFee;
                
            } catch (Exception e) {
                log.error("费用估算失败", e);
                return 10000L; // 返回默认费用
            }
        });
    }

    /**
     * 验证钱包地址格式
     * @param walletAddress 钱包地址
     * @return true if valid, false otherwise
     */
    public boolean isValidWalletAddress(String walletAddress) {
        try {
            PublicKey.fromBase58(walletAddress);
            return true;
        } catch (Exception e) {
            log.warn("无效的钱包地址: {}", walletAddress);
            return false;
        }
    }

    /**
     * 验证代币地址格式
     * @param tokenAddress 代币地址
     * @return true if valid, false otherwise
     */
    public boolean isValidTokenAddress(String tokenAddress) {
        try {
            PublicKey.fromBase58(tokenAddress);
            return true;
        } catch (Exception e) {
            log.warn("无效的代币地址: {}", tokenAddress);
            return false;
        }
    }

    /**
     * 获取账户信息
     * @param address 账户地址
     * @param useMainnet 是否使用主网
     * @return 账户信息
     */
    public CompletableFuture<AccountInfo> getAccountInfo(String address, boolean useMainnet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SolanaRpcClient rpcClient = useMainnet ? mainnetRpcClient : devnetRpcClient;
                PublicKey publicKey = PublicKey.fromBase58(address);
                
                return rpcClient.getAccountInfo(publicKey).join();
                
            } catch (Exception e) {
                log.error("获取账户信息失败，地址: {}", address, e);
                throw new RuntimeException("Failed to get account info: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 检查账户是否存在
     * @param address 账户地址
     * @param useMainnet 是否使用主网
     * @return true if exists, false otherwise
     */
    public CompletableFuture<Boolean> accountExists(String address, boolean useMainnet) {
        return getAccountInfo(address, useMainnet)
                .thenApply(accountInfo -> accountInfo != null)
                .exceptionally(throwable -> false);
    }

    /**
     * 获取最新区块哈希
     * @param useMainnet 是否使用主网
     * @return 最新区块哈希
     */
    public CompletableFuture<String> getLatestBlockhash(boolean useMainnet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SolanaRpcClient rpcClient = useMainnet ? mainnetRpcClient : devnetRpcClient;
                LatestBlockhash latestBlockhash = rpcClient.getLatestBlockhash().join();
                return latestBlockhash.blockhash();
                
            } catch (Exception e) {
                log.error("获取最新区块哈希失败", e);
                throw new RuntimeException("Failed to get latest blockhash: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 创建基础指令构建器
     * @param programId 程序ID
     * @return 指令构建器
     */
    public Instruction.Builder createInstructionBuilder(PublicKey programId) {
        return Instruction.builder().program(programId);
    }

    /**
     * 将数值转换为字节数组（小端序）
     * @param value 数值
     * @return 字节数组
     */
    public byte[] toBytes(long value) {
        return BigInteger.valueOf(value).toByteArray();
    }

    /**
     * 将数值转换为字节数组（小端序）
     * @param value 数值
     * @return 字节数组
     */
    public byte[] toBytes(BigInteger value) {
        return value.toByteArray();
    }
}