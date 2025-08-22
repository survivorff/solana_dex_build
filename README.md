# Solana DEX交易编码服务

一个基于Java Spring Boot的Solana区块链DEX交易编码服务，支持Pumpfun、PumpSwap、Raydium三个主要DEX平台的交易编码功能。

## 🚀 功能特性

- **多DEX支持**：支持Pumpfun、PumpSwap、Raydium三个DEX平台
- **交易编码**：使用Solana官方推荐的sava库进行交易编码
- **主网测试**：提供完整的主网集成测试功能
- **API接口**：RESTful API接口，支持异步处理
- **监控告警**：集成Prometheus监控和健康检查
- **数据持久化**：PostgreSQL数据库存储交易记录
- **缓存优化**：Redis缓存提升性能

## 🏗️ 技术架构

- **后端框架**：Spring Boot 3.2 + Java 17
- **区块链库**：Sava (Solana官方推荐)
- **数据库**：PostgreSQL 15
- **缓存**：Redis 7.0
- **API文档**：OpenAPI 3.0 + Swagger UI
- **监控**：Micrometer + Prometheus + Grafana
- **测试**：JUnit 5 + TestContainers

## 📦 项目结构

```
solana-dex-service/
├── src/main/java/com/solana/dex/
│   ├── adapter/              # DEX适配器
│   │   ├── PumpfunAdapter.java
│   │   ├── PumpSwapAdapter.java
│   │   └── RaydiumAdapter.java
│   ├── config/               # 配置类
│   │   └── SolanaConfig.java
│   ├── controller/           # API控制器
│   │   └── TransactionController.java
│   ├── dto/                  # 数据传输对象
│   │   ├── TransactionEncodeRequest.java
│   │   └── TransactionEncodeResponse.java
│   ├── entity/               # 数据库实体
│   │   ├── ApiKey.java
│   │   ├── Transaction.java
│   │   ├── DexConfig.java
│   │   └── TransactionLog.java
│   ├── repository/           # 数据访问层
│   │   ├── ApiKeyRepository.java
│   │   ├── TransactionRepository.java
│   │   ├── DexConfigRepository.java
│   │   └── TransactionLogRepository.java
│   ├── service/              # 业务服务层
│   │   └── SolanaTransactionService.java
│   └── SolanaDexServiceApplication.java
├── src/test/java/com/solana/dex/
│   ├── MainnetIntegrationTest.java    # 主网集成测试
│   └── util/
│       └── MainnetTransactionTester.java
├── src/main/resources/
│   └── application.yml
├── src/test/resources/
│   └── application-test.yml
└── pom.xml
```

## 🛠️ 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Redis 7.0+
- Docker (可选)

### 1. 克隆项目

```bash
git clone <repository-url>
cd solana-dex-service
```

### 2. 配置数据库

创建PostgreSQL数据库：

```sql
CREATE DATABASE solana_dex;
CREATE USER solana_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE solana_dex TO solana_user;
```

### 3. 配置环境变量

创建 `.env` 文件：

```bash
# 数据库配置
DB_USERNAME=solana_user
DB_PASSWORD=your_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Solana RPC配置
SOLANA_MAINNET_RPC=https://api.mainnet-beta.solana.com
SOLANA_DEVNET_RPC=https://api.devnet.solana.com
```

### 4. 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动服务
mvn spring-boot:run
```

### 5. 访问API文档

服务启动后，访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

## 🧪 测试指南

### 单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=MainnetIntegrationTest
```

### 主网集成测试

**⚠️ 警告：主网测试涉及真实资金，请谨慎操作！**

1. **配置测试环境**

```yaml
# src/test/resources/application-test.yml
test:
  wallet:
    address: "your_test_wallet_address"
    private-key: "your_test_wallet_private_key_base58"
  network:
    use-mainnet: true
    enable-onchain-test: false  # 设为true启用实际上链测试
```

2. **运行主网测试**

```bash
# 运行主网集成测试（仅编码测试）
mvn test -Dtest=MainnetIntegrationTest -Dspring.profiles.active=mainnet-test

# 运行开发网测试（安全）
mvn test -Dtest=MainnetIntegrationTest -Dspring.profiles.active=dev-test
```

3. **测试覆盖的功能**

- ✅ Pumpfun买入/卖出交易编码
- ✅ PumpSwap交易编码
- ✅ Raydium交易编码
- ✅ 并发交易编码测试
- ✅ 无效参数处理测试
- ✅ 网络连接和RPC调用测试
- ✅ 交易编码性能测试
- ✅ 交易数据格式验证

## 📡 API使用示例

### Pumpfun交易编码

```bash
curl -X POST "http://localhost:8080/api/v1/encode/pumpfun" \
  -H "Content-Type: application/json" \
  -d '{
    "walletAddress": "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM",
    "tokenMint": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
    "amount": "1000000",
    "slippage": 1.0,
    "operation": "buy",
    "useMainnet": true
  }'
```

### 响应示例

```json
{
  "success": true,
  "transactionData": "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAEDArczbMia1tLmq2poP39/+FVtZrfecZEL5pBOCJSfZUEABt324ddloZPZy+FGzut5rju4IKBhuz9zZG8XRgHdDrGQsQbd9uHXZaGT2cvhRs7rea47uCCgYbs/c2RvF0YB3Q6xkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAgEBARIJAQAAAADh9QUAAAAABgEBAgECAAkD",
  "estimatedFee": 0.000005,
  "transactionId": "pumpfun_1703123456789_123456",
  "dexType": "pumpfun",
  "operationType": "buy",
  "walletAddress": "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM",
  "tokenMint": "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
  "amount": "1000000",
  "slippage": 1.0,
  "createdAt": "2023-12-21T10:30:56.789Z"
}
```

### 获取支持的DEX列表

```bash
curl -X GET "http://localhost:8080/api/v1/encode/supported-dexes"
```

## 🔧 配置说明

### 主要配置项

```yaml
# Solana网络配置
solana:
  rpc:
    mainnet-url: https://api.mainnet-beta.solana.com
    devnet-url: https://api.devnet.solana.com
    timeout: 30000
    max-retries: 3
  
  # DEX配置
  dex:
    pumpfun:
      program-id: "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
      fee-rate: 0.0025
      min-amount: 1000
    pumpswap:
      program-id: "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin"
      fee-rate: 0.003
      min-amount: 500
    raydium:
      program-id: "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8"
      fee-rate: 0.0025
      min-amount: 1000
```

## 📊 监控和日志

### Prometheus指标

访问监控指标：

```
http://localhost:8080/actuator/prometheus
```

### 健康检查

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/encode/health
```

### 日志配置

日志文件位置：`logs/solana-dex-service.log`

## 🚨 注意事项

### 安全提醒

1. **私钥安全**：永远不要在代码中硬编码私钥
2. **测试环境**：优先在测试网进行验证
3. **资金安全**：主网测试使用少量资金
4. **API密钥**：生产环境必须配置API密钥认证

### 性能优化

1. **连接池**：合理配置数据库和Redis连接池
2. **缓存策略**：根据业务需求调整缓存TTL
3. **RPC限制**：注意Solana RPC节点的请求限制
4. **异步处理**：大量请求时使用异步API

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如有问题或建议，请：

1. 查看 [Issues](../../issues)
2. 创建新的 Issue
3. 联系开发团队

---

**⚠️ 免责声明**：本软件仅供学习和研究使用。使用本软件进行实际交易时，请自行承担风险。开发者不对任何资金损失负责。