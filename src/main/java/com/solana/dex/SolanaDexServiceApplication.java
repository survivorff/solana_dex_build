package com.solana.dex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Solana DEX交易编码服务主应用类
 * 
 * 支持的DEX平台：
 * - Pumpfun
 * - PumpSwap  
 * - Raydium
 * 
 * 主要功能：
 * - 交易指令编码
 * - 批量交易打包
 * - 交易状态查询
 * - 系统监控
 * 
 * @author Solana DEX Service Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class SolanaDexServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolanaDexServiceApplication.class, args);
    }

}