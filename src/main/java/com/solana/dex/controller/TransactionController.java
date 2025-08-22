package com.solana.dex.controller;

import com.solana.dex.adapter.PumpfunAdapter;
import com.solana.dex.adapter.PumpSwapAdapter;
import com.solana.dex.adapter.RaydiumAdapter;
import com.solana.dex.dto.TransactionEncodeRequest;
import com.solana.dex.dto.TransactionEncodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 交易编码控制器
 * 提供各个DEX的交易编码API接口
 */
@RestController
@RequestMapping("/api/v1/encode")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "交易编码", description = "Solana DEX交易编码API")
public class TransactionController {

    private final PumpfunAdapter pumpfunAdapter;
    private final PumpSwapAdapter pumpSwapAdapter;
    private final RaydiumAdapter raydiumAdapter;

    /**
     * Pumpfun DEX交易编码
     */
    @PostMapping("/pumpfun")
    @Operation(summary = "Pumpfun交易编码", description = "为Pumpfun DEX生成交易编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "编码成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public CompletableFuture<ResponseEntity<TransactionEncodeResponse>> encodePumpfunTransaction(
            @Parameter(description = "交易编码请求参数", required = true)
            @Valid @RequestBody TransactionEncodeRequest request) {
        
        log.info("收到Pumpfun交易编码请求，钱包: {}, 操作: {}, 代币: {}, 数量: {}", 
                request.getWalletAddress(), request.getOperation(), 
                request.getTokenMint(), request.getAmount());

        return pumpfunAdapter.encodeTransaction(request)
                .thenApply(response -> {
                    if (response.isSuccessful()) {
                        log.info("Pumpfun交易编码成功，交易ID: {}", response.getTransactionId());
                        return ResponseEntity.ok(response);
                    } else {
                        log.warn("Pumpfun交易编码失败，错误: {}", response.getError());
                        return ResponseEntity.badRequest().body(response);
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Pumpfun交易编码异常", throwable);
                    TransactionEncodeResponse errorResponse = TransactionEncodeResponse
                            .failure("服务器内部错误: " + throwable.getMessage(), "INTERNAL_ERROR");
                    return ResponseEntity.internalServerError().body(errorResponse);
                });
    }

    /**
     * PumpSwap DEX交易编码
     */
    @PostMapping("/pumpswap")
    @Operation(summary = "PumpSwap交易编码", description = "为PumpSwap DEX生成交易编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "编码成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public CompletableFuture<ResponseEntity<TransactionEncodeResponse>> encodePumpSwapTransaction(
            @Parameter(description = "交易编码请求参数", required = true)
            @Valid @RequestBody TransactionEncodeRequest request) {
        
        log.info("收到PumpSwap交易编码请求，钱包: {}, 操作: {}, 代币: {}, 数量: {}", 
                request.getWalletAddress(), request.getOperation(), 
                request.getTokenMint(), request.getAmount());

        return pumpSwapAdapter.encodeTransaction(request)
                .thenApply(response -> {
                    if (response.isSuccessful()) {
                        log.info("PumpSwap交易编码成功，交易ID: {}", response.getTransactionId());
                        return ResponseEntity.ok(response);
                    } else {
                        log.warn("PumpSwap交易编码失败，错误: {}", response.getError());
                        return ResponseEntity.badRequest().body(response);
                    }
                })
                .exceptionally(throwable -> {
                    log.error("PumpSwap交易编码异常", throwable);
                    TransactionEncodeResponse errorResponse = TransactionEncodeResponse
                            .failure("服务器内部错误: " + throwable.getMessage(), "INTERNAL_ERROR");
                    return ResponseEntity.internalServerError().body(errorResponse);
                });
    }

    /**
     * Raydium DEX交易编码
     */
    @PostMapping("/raydium")
    @Operation(summary = "Raydium交易编码", description = "为Raydium DEX生成交易编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "编码成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public CompletableFuture<ResponseEntity<TransactionEncodeResponse>> encodeRaydiumTransaction(
            @Parameter(description = "交易编码请求参数", required = true)
            @Valid @RequestBody TransactionEncodeRequest request) {
        
        log.info("收到Raydium交易编码请求，钱包: {}, 操作: {}, 代币: {}, 数量: {}", 
                request.getWalletAddress(), request.getOperation(), 
                request.getTokenMint(), request.getAmount());

        return raydiumAdapter.encodeTransaction(request)
                .thenApply(response -> {
                    if (response.isSuccessful()) {
                        log.info("Raydium交易编码成功，交易ID: {}", response.getTransactionId());
                        return ResponseEntity.ok(response);
                    } else {
                        log.warn("Raydium交易编码失败，错误: {}", response.getError());
                        return ResponseEntity.badRequest().body(response);
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Raydium交易编码异常", throwable);
                    TransactionEncodeResponse errorResponse = TransactionEncodeResponse
                            .failure("服务器内部错误: " + throwable.getMessage(), "INTERNAL_ERROR");
                    return ResponseEntity.internalServerError().body(errorResponse);
                });
    }

    /**
     * 批量交易打包编码
     */
    @PostMapping("/batch")
    @Operation(summary = "批量交易编码", description = "将多个DEX操作打包成单个交易")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "编码成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "501", description = "功能暂未实现")
    })
    public ResponseEntity<TransactionEncodeResponse> encodeBatchTransaction(
            @Parameter(description = "批量交易编码请求参数", required = true)
            @Valid @RequestBody java.util.List<TransactionEncodeRequest> requests) {
        
        log.info("收到批量交易编码请求，交易数量: {}", requests.size());
        
        // TODO: 实现批量交易打包功能
        TransactionEncodeResponse response = TransactionEncodeResponse
                .failure("批量交易功能暂未实现", "NOT_IMPLEMENTED");
        
        return ResponseEntity.status(501).body(response);
    }

    /**
     * 获取支持的DEX列表
     */
    @GetMapping("/supported-dexes")
    @Operation(summary = "获取支持的DEX列表", description = "返回当前支持的所有DEX平台")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<java.util.Map<String, Object>> getSupportedDexes() {
        
        java.util.Map<String, Object> supportedDexes = new java.util.HashMap<>();
        
        // Pumpfun信息
        java.util.Map<String, Object> pumpfunInfo = new java.util.HashMap<>();
        pumpfunInfo.put("name", pumpfunAdapter.getDexName());
        pumpfunInfo.put("operations", java.util.Arrays.asList("buy", "sell"));
        pumpfunInfo.put("status", "active");
        supportedDexes.put("pumpfun", pumpfunInfo);
        
        // PumpSwap信息
        java.util.Map<String, Object> pumpswapInfo = new java.util.HashMap<>();
        pumpswapInfo.put("name", pumpSwapAdapter.getDexName());
        pumpswapInfo.put("operations", pumpSwapAdapter.getSupportedOperations());
        pumpswapInfo.put("status", "active");
        supportedDexes.put("pumpswap", pumpswapInfo);
        
        // Raydium信息
        java.util.Map<String, Object> raydiumInfo = new java.util.HashMap<>();
        raydiumInfo.put("name", raydiumAdapter.getDexName());
        raydiumInfo.put("operations", raydiumAdapter.getSupportedOperations());
        raydiumInfo.put("status", "active");
        supportedDexes.put("raydium", raydiumInfo);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("supported_dexes", supportedDexes);
        response.put("total_count", supportedDexes.size());
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "检查交易编码服务的健康状态")
    @ApiResponse(responseCode = "200", description = "服务正常")
    public ResponseEntity<java.util.Map<String, Object>> healthCheck() {
        
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("service", "solana-dex-transaction-encoding");
        health.put("version", "1.0.0");
        
        // 检查各个适配器状态
        java.util.Map<String, String> adapters = new java.util.HashMap<>();
        adapters.put("pumpfun", "UP");
        adapters.put("pumpswap", "UP");
        adapters.put("raydium", "UP");
        health.put("adapters", adapters);
        
        return ResponseEntity.ok(health);
    }
}