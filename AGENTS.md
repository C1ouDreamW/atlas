# Project Instructions

**其他重要说明文件在docs目录下，docs/Background.md 是项目背景与目标，docs/plans_1.md 是项目落地路径与架构规划**

## Project Identity

**Atlas** — 智能在线题库与刷题平台后端。面向大学生期末备考场景，核心壁垒是以 LLM（大语言模型）实现非结构化文档到结构化题库的一键解析，并通过 Redis 缓存应对期末周高并发刷题读压力。

- **语言**: Java 17
- **框架**: Spring Boot 3.5.x (jakarta.*), MyBatis-Plus 3.5.10.1
- **数据库**: MySQL 8.x
- **缓存**: Redis (StringRedisTemplate, 非 Spring Cache 抽象)
- **AI**: DeepSeek Chat API (OpenAI 兼容 Chat Completions)
- **API 文档**: SpringDoc OpenAPI 2.7.0 (Swagger UI)
- **构建**: Maven, 无 Docker 化（待实现）

## Build & Run

```bash
# 编译（跳过测试）
mvn clean install -DskipTests

# 启动
mvn spring-boot:run

# 运行测试
mvn test

# 打包
mvn package -DskipTests
java -jar target/quiz-backend-0.0.1-SNAPSHOT.jar
```

### 环境变量

所有敏感配置通过环境变量注入，`application.yaml` 中已预留占位符：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | 127.0.0.1 | MySQL 地址 |
| `DB_PORT` | 3306 | MySQL 端口 |
| `DB_NAME` | quiz_atlas | 数据库名 |
| `DB_USER` | root | 数据库用户 |
| `DB_PASSWORD` | root | 数据库密码 |
| `REDIS_HOST` | 127.0.0.1 | Redis 地址 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `DEEPSEEK_API_KEY` | (空) | 大模型 API Key（不跑 AI 导入时可空） |
| `JWT_SECRET` | YOUR_SUPER... | JWT 签名密钥（HMAC-SHA，已实装） |

数据库初始化（人工执行，Agent请勿操作）：`mysql -u root -p < sql/schema/init_core_tables.sql`

---

## Package Structure & Layer Convention

```
cn.heycloudream.quiz_backend
├── client/          # 第三方 API 调用（LlmChatClient）
├── common/
│   ├── constants/   # 全局常量（Redis Key、校验长度）
│   ├── dto/         # 通用 DTO（PageRequestDTO）
│   └── vo/          # 通用 VO（Result<T>, PageResultVO<T>）
├── config/          # @Configuration（AsyncConfig, QuizLlmProperties）
├── controller/      # REST Controller
├── dto/             # 业务 DTO（按模块分子包）
├── entity/          # MyBatis-Plus Entity（禁止暴露到 Controller）
├── enums/           # 枚举
├── exception/       # BusinessException + GlobalExceptionHandler
├── mapper/          # MyBatis-Plus Mapper
├── service/         # Service 接口 + impl/ + 横切子包
├── util/            # 工具类
└── vo/              # 响应 VO（按模块分子包）
```

### 硬规则

1. **DTO/VO 严格隔离**：Controller 入参只用 DTO（带 `@Valid` 校验），返回值统一包装为 `Result<T>` 或 `Result<PageResultVO<T>>`。**Entity 绝不直接暴露到 Controller 层**，转换在 Service 层完成。
2. **异常不散落 try/catch**：业务异常统一抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` (@RestControllerAdvice) 捕获并转为 `Result.fail()`。不要在各处吞异常或返回 null。
3. **校验在 DTO 层**：所有校验注解（`@NotBlank`, `@NotNull`, `@Size` 等）放在 DTO 字段上。Controller 类上加 `@Validated`，方法参数上加 `@Valid`。校验常量引用 `ValidationConstants`，禁止魔法数字。
4. **时间统一 `LocalDateTime`**：Entity/DTO/VO 中所有时间字段使用 `LocalDateTime`，MySQL 列类型 `DATETIME`。Jackson 序列化依赖 Spring Boot 默认配置。
5. **逻辑删除**：所有表通过 `is_deleted` 字段逻辑删除，Entity 上加 `@TableLogic`，MyBatis-Plus 自动拼接 `is_deleted=0`。

---

## Core Patterns (Do NOT Break)

### 1. 归属权校验

题库和试题的所有写操作、以及试题的读操作，均需校验当前用户是否拥有该题库。校验方法统一为：

```java
private void requireOwnedBank(Long currentUserId, Long bankId) {
    QuestionBank bank = questionBankMapper.selectById(bankId);
    if (bank == null || bank.getUserId() == null 
        || !bank.getUserId().equals(currentUserId)) {
        throw new BusinessException(404, "题库不存在或无权访问");
    }
}
```

> **当前状态**：JWT 鉴权已实装。`JwtAuthInterceptor` 从 `Authorization: Bearer <token>` 提取 Token 验签后写入 `UserContextHolder`，Controller 中统一通过 `UserContextHolder.get()` 获取当前用户 ID。白名单路径：注册/登录、Swagger、题库大厅、公开热点题库详情。

### 2. Redis 缓存 (Cache-Aside)

热点公开题库聚合数据（`QuestionBankHotDetailServiceImpl`）采用手动 Cache-Aside，**不使用** Spring Cache `@Cacheable` 抽象：

- **读**：先查 Redis（Key: `smart_quiz:bank_detail:{bankId}`），命中直接返回 JSON 反序列化对象
- **未命中**：`SET NX EX` 获取互斥锁，持锁线程回源 MySQL → 写 Redis → 释放锁；未获锁线程自旋等待（最多 2s）
- **防雪崩**：缓存 TTL 随机 30~40 分钟
- **防穿透**：不存在的题库写 `NULL_BANK` 占位，TTL 5 分钟
- **写失效**：题库/试题的任何增删改操作，通过 `QuestionBankDetailCacheEvictor.evict(bankId)` 删除缓存
- **序列化**：缓存值存完整 JSON（`QuestionBankDetailBundleVO`），用 `ObjectMapper` 手动序列化/反序列化

> 新增缓存场景时必须沿用此模式，不要混入 Spring Cache 自动管理。

### 3. AI 智能导入（已重构为 Redis Stream 任务体系）

```
【新链路 — 基于 Redis Stream】
                                                        
前端上传文件 → Java后端（生产者）接收后，将任务存入Redis Stream中，并返回任务ID给前端。
Python后端（消费者）从Redis Stream中拉取任务，并调用 MinerU API 将文档转换为Markdown文本（异步处理）。
Python后端将Markdown文本发送给大模型API，并返回标准的题目 JSON 数组（异步处理）。
Java后端解析JSON，先返回前端预览，用户确认后，再批量插入 (Batch Insert) 到 MySQL 的试题表中。

注意：Python后端需要实现一个独立的Worker，不要与Java后端耦合在一起。Python程序在transf-python目录下。

```

关键约束：
- 任务调度通过 Redis Stream（`quiz:task:stream`，消费组 `quiz-ai-workers`），解耦生产者/消费者
- 解析结果**不直接落库**，先写 `quiz:task:result:{taskId}` 供前端预览
- 用户确认后通过 `POST /{bankId}/questions/batch` 幂等落库（SETNX 锁，同一 taskId 只入库一次）
- 文件上传入口接入限流：`AiImportRateLimiter`（Redis Lua 滑动窗口，默认 5 次/小时/用户）
- `AiImportTaskWatchdog` @Scheduled 每 2 分钟扫描超时 PROCESSING 任务，自动标记 FAILED
- 旧 `@Async` 链路（`AiQuestionImportAsyncProcessor`）已标记 @Deprecated，兼容保留

> 旧端点 `POST /{bankId}/ai-import/text|file` 和 `GET /{bankId}/ai-import/status` 已标注废弃，指向新 `/api/v1/ai-import/*` 路径。

### 4. 大模型 JSON 清洗

`LlmJsonPayloadSanitizer.stripMarkdownCodeFence()` 去除 LLM 输出中可能的 markdown 代码围栏：

```
输入: ```json\n[...]\n```   →  输出: [...]
```

调用方在调用 `ObjectMapper.readValue()` 前必须先经过此清洗。

---

## Database Schema (Current)

| 表 | 说明 | 关键索引 |
|----|------|----------|
| `sys_user` | 用户表 | `uk_username(username, is_deleted)` |
| `question_bank` | 题库表 | `idx_user_public(user_id, is_public, is_deleted)` |
| `question` | 试题表（JSON 列存选项/答案） | `idx_bank_sort(question_bank_id, sort_no, is_deleted)` |
| `wrong_question` | 错题本 | `uk_user_question(user_id, question_id)`, `idx_user_create(user_id, create_time)` |

注意：
- `question.options_json` 和 `question.answer_json` 是 MySQL JSON 列，Java 侧用 `String` 存储 JSON 文本，不依赖 MySQL JSON 函数
- `wrong_question` 的业务约定：首次做错 INSERT；移除时 UPDATE `is_deleted=1`（逻辑删）；再次做错同一题 UPDATE 复活 `is_deleted=0` + 递增 `wrong_count`。禁止重复 INSERT。

---

## Current Progress & TODO

### 已完成 ✓
- [x] 数据库建模与 DDL
- [x] Entity/Mapper 层
- [x] DTO/VO 分层 + 校验注解 + SpringDoc 文档
- [x] 统一 Result/PageResultVO 响应 + GlobalExceptionHandler
- [x] 题库 CRUD + 试题 CRUD（含归属校验）
- [x] 题库大厅（公开题库分页列表 `GET /api/v1/question-banks/public`，无需登录）
- [x] AI 智能导入全链路（大模型调用 → 异步处理 → JSON 清洗 → 校验落库 → 状态轮询）
- [x] Redis 热点缓存（Cache-Aside + 防击穿 + 防穿透 + 防雪崩 + 写驱逐）
- [x] 细化的 AI 系统提示词

**阶段三 — 用户鉴权：**
- [x] `UserService` + `UserController`：注册（BCrypt 加密）、登录
- [x] JWT 签发/解析工具类（`JwtUtils`）
- [x] Spring `Interceptor` 拦截认证（`JwtAuthInterceptor`）
- [x] `UserContextHolder`（ThreadLocal）存取当前用户
- [x] 移除 Controller 中 11 处 `userId = 1L` 硬编码（已替换为 `UserContextHolder.get()`）

**阶段四 — 文档解析 + AI 导入重构：**
- [x] PDFBox 集成：PDF 文件 → 纯文本抽取
- [x] Apache POI 集成：.docx 文件 → 纯文本抽取
- [x] `DocumentParseUtils` 统一解析引擎（.txt/.pdf/.docx，try-with-resources）
- [x] Redis Stream 任务体系：生产者/消费者解耦，消费组 `quiz-ai-workers`
- [x] 预览→确认→幂等落库：沙盒草稿箱机制，SETNX 防重
- [x] 限流防护：AiImportRateLimiter（Redis Lua，5 次/小时/用户）
- [x] 超时清理：AiImportTaskWatchdog（@Scheduled，10 分钟超时）
- [x] 旧 @Async 链路标记 @Deprecated，兼容保留



**阶段五 — 刷题与错题本：**
- [x] 刷题接口（按题库获取试题列表 / 提交单题答案 / 判分）
- [x] 错题本接口（自动收集错题 / 查看错题本 / 按错题本重刷）
- [x] `WrongQuestionService` + `WrongQuestionController`
- [x] `QuestionBankHotDetailService` 可能需要区分「刷题模式」与「详情模式」

### 待完成 ○

**阶段六 — 测试与部署：**
- [ ] Controller / Service 单元测试（H2 内存库）
- [ ] JMeter 压测脚本 + 有无缓存对照报告
- [ ] Dockerfile + docker-compose.yml

---

## Development Workflow

### 新增一个业务功能的标准步骤

1. **如果涉及新表**：先在 `sql/` 下写 DDL，再生成 Entity + Mapper
2. **定义 DTO**：放在 `dto/{module}/` 下，字段上加 `@Schema` 和 Validation 注解，引用 `ValidationConstants`
3. **定义 VO**：放在 `vo/{module}/` 下，不暴露内部字段（如 `rawLlmJson`）
4. **写 Service 接口 + Impl**：
   - 需要事务的方法加 `@Transactional(rollbackFor = Exception.class)`
   - 涉及题库归属的在入口处调用 `requireOwnedBank()`
   - 写操作后调用 `questionBankDetailCacheEvictor.evict()`（如果影响题库内容）
5. **写 Controller**：
   - 类上加 `@Validated`，参数上加 `@Valid`
   - 返回值包装 `Result.success()`，分页用 `PageResultVO<T>`
   - 加上 `@Operation` 和 `@Tag` 的 Swagger 注解（`io.swagger.v3.oas.annotations`）
   - 通过 `UserContextHolder.get()` 从 JWT 鉴权拦截器注入的线程上下文获取当前用户 ID

### 代码审查检查点

- [ ] 是否有 NPE 风险（大模型返回 null、JSON 解析失败、查询结果为空）？
- [ ] 是否抛 `BusinessException` 而非裸 `RuntimeException` 或吞异常返回 null？
- [ ] 是否记录了 `@Slf4j` 日志（入参 + 异常原因）？
- [ ] 是否暴露了 Entity 到 Controller？
- [ ] DTO 是否加了 Validation？
- [ ] 写操作是否驱逐了相关 Redis 缓存？

---

## Key Files Reference

| 文件 | 重要性 | 说明 |
|------|--------|------|
| `docs/plans_1.md` | ★★★ | 六阶段完整落地计划 + 排坑指南 + 面试包装点 |
| `docs/待办清单.md` | ★★★ | 总 TODO 列表（含代码行号） |
| `src/main/java/.../config/JwtAuthInterceptor.java` | ★★★ | JWT 鉴权拦截器 |
| `src/main/java/.../util/JwtUtils.java` | ★★★ | JWT 签发/验签工具 |
| `src/main/java/.../controller/UserController.java` | ★★★ | 用户注册/登录接口 |
| `docs/prompt_1.md` | ★★ | 各阶段 AI 辅助开发 Prompt 模板 |
| `docs/JMeter压测.md` | ★★ | 压测方案（对照实验设计 + 报告指标） |
| `sql/schema/init_core_tables.sql` | ★★★ | 数据库 DDL |
| `src/main/resources/prompts/ai-import-system.txt` | ★★★ | LLM 系统提示词 |
| `src/main/resources/application.yaml` | ★★ | 配置（含环境变量占位） |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | ★★ | 全局异常映射 |
| `src/main/java/.../controller/AiImportController.java` | ★★★ | AI 导入新 Controller（提交/轮询） |
| `src/main/java/.../service/ai/AiImportStreamConsumer.java` | ★★★ | Redis Stream 消费者 |
| `src/main/java/.../service/ai/RedisStreamTaskDispatcher.java` | ★★★ | Stream 任务派发 |
| `src/main/java/.../service/ai/AiImportTaskStatusStore.java` | ★★ | 任务状态存储 |
| `src/main/java/.../service/ai/AiImportResultStore.java` | ★★ | 任务结果存储 |
| `src/main/java/.../service/ai/ImportIdempotentService.java` | ★★ | 批量导入幂等锁 |
| `src/main/java/.../service/ai/AiImportRateLimiter.java` | ★★ | 提交限流器 |
| `src/main/java/.../service/ai/AiImportTaskWatchdog.java` | ★★ | 超时任务清理 |
| `src/main/java/.../util/DocumentParseUtils.java` | ★★★ | 文档解析引擎（.txt/.pdf/.docx → 纯文本） |
| `src/main/java/.../service/impl/QuestionBankHotDetailServiceImpl.java` | ★★★ | Redis 缓存全部逻辑 |
| `src/main/java/.../service/impl/AiQuestionImportAsyncProcessor.java` | ★★★ | AI 导入异步核心链路（@Deprecated） |