package com.solana.dex.service;

import com.solana.dex.config.SolanaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.TransactionInstruction;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.types.AccountInfo;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;

/**
 * Solana交易编码服务
 * 使用sava库实现交易的构建和编码
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolanaTransactionService {

    private final RpcClient mainnetRpcClient;
    private final RpcClient devnetRpcClient;
    private final SolanaConfig solanaConfig;

    /**
     * 创建并编码交易
     * @param walletAddress 钱包地址
     * @param instructions 交易指令列表
     * @param useMainnet 是否使用主网
     * @return 编码后的交易数据
     */
    public CompletableFuture<String> encodeTransaction(String walletAddress, 
                                                     List<TransactionInstruction> instructions, 
                                                     boolean useMainnet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始编码交易，钱包地址: {}, 指令数量: {}", walletAddress, instructions.size());
                
                // 选择RPC客户端
                RpcClient rpcClient = useMainnet ? mainnetRpcClient : devnetRpcClient;
                
                // 获取最新区块哈希
                String latestBlockhash = rpcClient.getApi().getLatestBlockhash();
                log.debug("获取到最新区块哈希: {}", latestBlockhash);
                
                // 创建钱包公钥
                PublicKey wallet = new PublicKey(walletAddress);
                
                // 构建交易
                Transaction transaction = new Transaction();
                transaction.setRecentBlockHash(latestBlockhash);
                transaction.setFeePayer(wallet);
                
                // 添加指令
                for (TransactionInstruction instruction : instructions) {
                    transaction.addInstruction(instruction);
                }
                
                // 序列化交易
                byte[] serializedTx = transaction.serialize();
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
                                                     TransactionInstruction instruction, 
                                                     boolean useMainnet) {
        return encodeTransaction(walletAddress, List.of(instruction), useMainnet);
    }

    /**
     * 估算交易费用
     * @param instructions 交易指令列表
     * @param useMainnet 是否使用主网
     * @return 预估费用（lamports）
     */
    public CompletableFuture<Long> estimateTransactionFee(List<TransactionInstruction> instructions, boolean useMainnet) {
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
            new PublicKey(walletAddress);
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
            new PublicKey(tokenAddress);
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
                RpcClient rpcClient = useMainnet ? mainnetRpcClient : devnetRpcClient;
                PublicKey publicKey = new PublicKey(address);
                
                return rpcClient.getApi().getAccountInfo(publicKey);
                
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
                RpcClient rpcClient = useMainnet ? mainnetRpcClient : devnetRpcClient;
                return rpcClient.getApi().getLatestBlockhash();
                
            } catch (Exception e) {
                log.error("获取最新区块哈希失败", e);
                throw new RuntimeException("Failed to get latest blockhash: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 创建交易指令
     * @param programId 程序ID
     * @param keys 账户元数据列表
     * @param data 指令数据
     * @return 交易指令
     */
    public TransactionInstruction createInstruction(PublicKey programId, List<org.p2p.solanaj.core.AccountMeta> keys, byte[] data) {
        return new TransactionInstruction(programId, keys, data);
    }

    /**
     * 创建账户元数据
     * @param publicKey 公钥
     * @param isSigner 是否签名者
     * @param isWritable 是否可写
     * @return 账户元数据
     */
    public org.p2p.solanaj.core.AccountMeta createAccountMeta(PublicKey publicKey, boolean isSigner, boolean isWritable) {
        return new org.p2p.solanaj.core.AccountMeta(publicKey, isSigner, isWritable);
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