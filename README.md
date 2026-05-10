# Atlas - 智能在线题库与刷题平台 (Quiz Backend)

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.5.x-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.x-orange.svg)
![Redis](https://img.shields.io/badge/Redis-Cache-red.svg)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-yellow.svg)

Atlas 是一款专为大学生考试备考痛点设计的智能在线刷题平台后端服务。本项目旨在提供高效的习题录入、智能化试题解析、高并发访问下的热点题库管理以及个性化错题本追踪等核心功能，打造一体化、智能通用的试题与学习管理解决方案。

## 🎯 核心特性 (Key Features)

- **🔐 安全可靠的用户与鉴权系统**：基于 JWT 和全无状态的令牌认证体系，结合 BCrypt 强哈希算法保护密码安全。支持角色权限隔离。
- **🤖 多模态 AI 智能导题引擎**：支持提取 `.docx`、`.pdf`、`.txt` 文本，并通过异步集成大语言模型（如 Kimi/DeepSeek）实现非结构化数据到结构化考题（选项、答案、解析）的自动化转换。
- **📚 完善的题库管理生态**：对海量试题与聚合题库提供完整的 CRUD 生命周期管理，支持公开热点题库分享机制。
- **⚡ 高并发缓存调优**：创新性结合 Redis，深入处理热点题库数据的共享读取（Cache-Aside），应对高并发查询挑战，缓解 MySQL 数据库峰值加载压力。
- **📈 智能错题追踪机制**：支持顺序与随机刷题模式，自动归档并记录多次错题重做记录，提升复习与巩固效率。

## 🛠️ 系统架构与技术栈 (Architecture & Tech Stack)

- **核心语言**：Java 17
- **基础框架**：Spring Boot 3.x
- **持久层封装平台**：MyBatis-Plus 3.5.x
- **数据库存储**：MySQL 8.x
- **高速缓存中心**：Redis
- **安全与校验**：JSON Web Token (JWT) / Spring Validation
- **文档与调试工具**：Knife4j (Swagger / OpenAPI 3)
- **依赖与构建**：Maven

## ⚙️ 前置要求 (Prerequisites)

为了成功搭建和运行本项目，你需要提前在本地或服务器环境中准备：

- **JDK**: >= 17 （建议配置好 `JAVA_HOME`）
- **Maven**: >= 3.8.x
- **MySQL**: >= 8.0（开启 utf8mb4 支持）
- **Redis**: 稳定运行版本即可
- **IDE 推荐**: IntelliJ IDEA，需安装并启用 Lombok 插件。

### 核心环境变量规划 (.env 或 application.yml)

配置服务器端相关变量，例如：
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/quiz_atlas?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: YOUR_PASSWORD
  redis:
    host: 127.0.0.1
    port: 6379
    password: YOUR_REDIS_PASSWORD

# AI 大模型 API 相关配置示例
quiz:
  llm:
    api-key: YOUR_DEEPSEEK_OR_KIMI_KEY
    endpoint: https://api.moonshot.cn/v1/chat/completions # 仅示例
```

## 🚀 安装与启动逻辑 (Getting Started)

### 1. 数据库初始化
进入 MySQL，执行仓库中提供的 `sql/schema/init_core_tables.sql` 初始化数据表：
```bash
mysql -u root -p < sql/schema/init_core_tables.sql
```

### 2. 构建与运行代码
克隆项目后，可以使用 Maven 在本地迅速编译并启动：
```bash
# 进入项目目录
cd atlas

# 下载第三方依赖与刷新
mvn clean install -DskipTests

# 本地启动 Spring Boot 应用程序
mvn spring-boot:run
```

或通过 `java -jar` 运行构建产物：
```bash
mvn package
java -jar target/quiz-backend-0.0.1-SNAPSHOT.jar
```

## 🐛 调试与开发指南 (Debugging & Development)

### 调试规范
1. 均采用 MVC 标准的三层架构推进流：`Controller` -> `Service` -> `Mapper` -> `Entity`。
2. 数据流转规范采用完整的实体切分策略：接收使用 `DTO`，返回前端装配 `VO`，严禁将数据层 `Entity` 直接通过 API 暴露。
3. 异常处理：在 `GlobalExceptionHandler` 捕获异常，绝不在业务代码中过度散落 `try/catch`，推荐显式抛出 `BusinessException` 等自定义异常。

### 单元测试集成
项目中集成了强大的 H2 内存数据库以及单元测试模块：
```bash
# 运行单元测试
mvn test
```
测试报告和异常堆栈会输出至 `target/surefire-reports` 中。

## 📜 核心 API 接口概览 (API Reference)

*完整的交互契约与请求示例，可通过启动应用并访问 Knife4j 面板获得 (通常位于 `http://localhost:8080/doc.html`)*。以下为项目核心 RESTful 范式的 API 总览：

| 功能模块 | HTTP | 路由路径 | 接口描述 | 预期参数预估 |
| --- | --- | --- | --- | --- |
| **题库管理** | `POST` | `/api/v1/question-banks` | 创建题库 | `QuestionBankCreateDTO` |
| **题库管理** | `PUT` | `/api/v1/question-banks/{id}`| 更新题库 | `QuestionBankUpdateDTO` |
| **智能导题** | `POST` | `/api/v1/ai/import/text` | 异步 AI 文本导题 | `AiQuestionImportTextDTO` |
| **试题管理** | `POST` | `/api/v1/questions` | 手动新增试题 | `QuestionCreateDTO` |
| **试题管理** | `PUT` | `/api/v1/questions/{id}` | 更新试题内容 | `QuestionUpdateDTO` |

> 所有接口统一返回标准化 `Result<T>` 格式，包含标识码 `code`、提示消息 `message` 以及业务承载体 `data`。

## 📁 核心项目目录结构 (Project Structure)

```text
├── sql/
│   └── schema/              # 数据库核心初始化部署 SQL 脚本
├── src/
│   ├── main/
│   │   ├── java/cn/.../quiz_backend/
│   │   │   ├── client/      # 第三方 API 发起层 (LLM等)
│   │   │   ├── common/      # 公共常量定义、通用分页DTO和统一ResultVO封装
│   │   │   ├── config/      # Bean 全局配置 (跨域, 异步线程池, AI参数组)
│   │   │   ├── controller/  # 控制器层 (HTTP 路由)
│   │   │   ├── dto/         # Data Transfer Object，负责接受前端请求
│   │   │   ├── entity/      # MyBatis-Plus 与 MySQL 表对应的数据库实体映射模型
│   │   │   ├── enums/       # 业务类型枚举层
│   │   │   ├── exception/   # 自定义异常处理机制及全局拦截器
│   │   │   ├── mapper/      # 数据访问层接口 (MyBatis/SQL)
│   │   │   ├── service/     # 业务逻辑接口定义与层设计
│   │   │   │   ├── ai/      # AI 响应逻辑、异步处理实现
│   │   │   │   └── impl/    # 核心服务逻辑默认实现类
│   │   │   ├── util/        # 静态工具类 (如 JSON 解析净化工具工具)
│   │   │   └── vo/          # 展现给前台的数据承载对象
│   │   └── resources/
│   │       ├── application.yaml     # Spring Boot 环境配置文件
│   │       └── prompts/             # 向 LLM 投喂的基础 System Prompt 配置库
│   └── test/                        # 单元与集成测试模块 
└── pom.xml                  # Maven 核心配置清单
```

## 🤝 贡献规范与协议 (Contributing & License)

所有 PRs 都应满足以下工程标准：
- 新需求代码应该使用驼峰命名法（`camelCase` / `PascalCase`）；
- 中文注释，且方法名具有高度可读性；
- 若引入新的第三方调用，务必实现 `@Async` 异步无阻塞回调处理。