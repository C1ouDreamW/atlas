# iShua 后端测试说明

本文档说明后端测试配置、测试分层、运行命令和新增测试时的约定。

## 快速运行

在 `backend/` 目录执行：

```bash
mvn test
```

运行指定测试类：

```bash
mvn -Dtest=PracticeServiceImplTest test
```

运行指定测试方法：

```bash
mvn -Dtest=PracticeServiceImplTest#submitAnswer* test
```

项目在 `pom.xml` 中为 Surefire 设置了：

```xml
<argLine>-Dfile.encoding=UTF-8</argLine>
```

因此测试输出和中文断言按 UTF-8 处理。

## 测试 Profile

测试资源目录：

```text
src/test/resources/
```

关键配置：

| 文件 | 作用 |
| --- | --- |
| `application.yml` | 默认激活 `test` profile |
| `application-test.yml` | H2、固定 JWT、关闭调度、关闭 Swagger |
| `schema.sql` | H2 建表 |
| `data.sql` | 测试种子数据 |

测试使用 H2 内存数据库：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:ishua_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE
```

测试 profile 中：

- Redis 仓库关闭。
- Spring 调度关闭，避免 Watchdog 和同步任务影响测试。
- Swagger/OpenAPI 关闭。
- JWT 密钥固定，便于测试签发 Token。
- AI 导入限流调高为 100 次/小时。
- 文件上传目录使用系统临时目录。

## 测试分层

| 类型 | 建议方式 | 说明 |
| --- | --- | --- |
| Service 单元测试 | Mockito | 不启动 Spring，专注业务分支 |
| Controller 测试 | `@WebMvcTest` + `MockMvc` | 验证路由、鉴权、参数校验、统一响应 |
| Mapper/上下文测试 | H2 + Spring 上下文 | 验证 SQL、MyBatis Plus 映射和上下文加载 |
| Redis 相关测试 | Mock Redis 或 Testcontainers | 默认使用 Mock，真 Redis 测试需 Docker |

已有测试覆盖：

- 用户注册、登录、当前用户信息。
- 题库公开列表、热点详情、创建、权限拦截。
- 试题详情、更新、删除、权限与参数校验。
- 刷题列表、提交答案、简答题参考答案。
- 错题记录、移除、错题重刷。
- 公开题库热点缓存 Cache-Aside。
- H2 schema 和种子数据可用性。

## 测试支撑类

| 类 | 用途 |
| --- | --- |
| `JwtTestHelper` | 签发测试 Bearer Token |
| `UserContextTestSupport` | 设置和清理 `UserContextHolder` |
| `MockMvcTestSupport` | 为 MockMvc 请求附加 `Authorization` 头 |
| `WebMvcAuthTestSupport` | Web 层鉴权辅助 |
| `AtlasWebMvcTestConfig` | WebMvcTest 导入拦截器和全局异常处理 |
| `AbstractMockRedisSpringBootTest` | Spring Boot 测试中 Mock `StringRedisTemplate` |
| `RedisTestcontainersSupport` | 需要真 Redis 时使用 Testcontainers |

默认 `mvn test` 不依赖本机 Redis。涉及 Redis 的 Spring 上下文测试优先继承 `AbstractMockRedisSpringBootTest`。

## Controller 测试约定

Controller 测试位于：

```text
src/test/java/cn/heycloudream/ishua_test/controller/
```

推荐结构：

```java
@WebMvcTest(controllers = XxxController.class)
@ContextConfiguration(classes = {IShuaTestApplication.class, XxxController.class})
@Import(AtlasWebMvcTestConfig.class)
class XxxControllerTest {
}
```

测试重点：

- 无 Token 返回 `code=401`。
- 角色不足返回 `code=403`。
- 参数校验失败返回 `code=400`。
- 正常请求返回 `code=200`。
- 资源不存在或无权访问返回 `code=404` 或业务定义的错误码。

注意：项目多数接口 HTTP 状态仍为 200，断言应检查响应 JSON 中的 `code`。

示例断言：

```java
mockMvc.perform(get("/api/v1/users/me")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
```

## Service 测试约定

Service 测试位于：

```text
src/test/java/cn/heycloudream/ishua_backend/service/
```

建议：

- 使用 Mockito 构造依赖，不启动完整 Spring。
- 对权限、资源归属、状态流转、缓存驱逐、异常分支写明确断言。
- 涉及用户上下文时，用 `UserContextTestSupport` 设置并在测试后清理。
- 涉及错题、刷题判分时，覆盖正确、错误、边界和主观题分支。

典型覆盖点：

- `PracticeServiceImplTest`：公开/私有题库访问、客观题判分、错题记录、简答题处理。
- `QuestionBankHotDetailServiceImplTest`：缓存命中、空值占位、互斥锁回源、自旋超时。
- `WrongQuestionServiceImplTest`：首次做错、复活错题、移除错题、防越权。

## Redis 测试策略

默认策略：

- 使用 `@MockBean StringRedisTemplate`。
- 不要求本地安装 Redis。
- 适合大多数 Service 和上下文测试。

真 Redis 策略：

- 继承 `RedisTestcontainersSupport`。
- 需要 Docker。
- 类上已有 `@Testcontainers(disabledWithoutDocker = true)`，无 Docker 时跳过。

使用真 Redis 的场景：

- 需要验证 Redis Lua/CAS 行为。
- 需要验证 Stream、TTL、锁等 Redis 原生能力。
- 需要验证 Cache-Aside 与 Redis 序列化细节。

## AI 导入相关测试建议

AI 导入链路跨 Java API、Redis Stream、Python Worker、MinerU 和 LLM。自动化测试应分层处理：

| 层级 | 测试方式 |
| --- | --- |
| Java 提交接口 | Mock 文件上传、Mock Redis Stream 派发、断言任务创建 |
| 状态查询 | 构造 DB/Redis 状态，断言 `SUBMITTED/PROCESSING/PARSED/FAILED` 响应 |
| 批量入库 | 构造 `QuestionPreviewVO[]`，断言幂等、状态校验和题目写入 |
| Worker | 使用单元测试 Mock MinerU/LLM/Redis，避免真实第三方调用 |
| 端到端联调 | 手动或独立集成环境运行，避免纳入默认 `mvn test` |

不建议在默认单元测试中真实调用 MinerU 或 LLM API。第三方 API 调用慢、费用不可控，也容易造成 CI 不稳定。

## 测试数据

测试种子位于：

```text
src/test/resources/data.sql
```

已有约定：

- 存在公开题库 `bankId=1`。
- 存在测试用户 `testuser`。

新增测试数据时：

- 保持 ID 稳定，便于 Controller 和 Service 测试复用。
- 避免与现有 ID 冲突。
- 同时更新必要的题库、试题、错题关系数据。

## 新增测试清单

新增后端功能时，至少检查：

- 成功路径是否有测试。
- 参数校验失败是否有测试。
- 未登录和角色不足是否有测试。
- 私有资源是否有越权访问测试。
- 涉及缓存的写操作是否驱逐缓存。
- 涉及任务状态的功能是否覆盖非法状态和重复提交。
- 涉及数据库写入的功能是否验证关键字段。

提交前执行：

```bash
mvn test
```
