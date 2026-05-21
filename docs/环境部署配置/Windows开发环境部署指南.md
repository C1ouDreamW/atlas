# Windows 开发环境部署指南

本文档面向 **Windows 10/11** 本地开发机，帮助你在单机上一站式跑通 Atlas 后端（`quiz-backend`）。内容基于仓库真实配置：`application.yaml`、`pom.xml`、`sql/schema/init_core_tables.sql`、`CorsConfig`、`WebMvcConfig` 等。

同目录可参考：[Linux生产环境部署指南.md](./Linux生产环境部署指南.md)。

---

## 1. 适用范围与目标

| 项 | 说明 |
|----|------|
| 操作系统 | Windows 10 / 11（64 位） |
| 终端 | PowerShell 5.1+ 或 Windows Terminal |
| 用途 | 本地开发、接口联调、单元测试、可选 AI 导入全链路调试 |
| 默认服务端口 | **8080**（Spring Boot 未在 `application.yaml` 中覆盖 `server.port`） |
| 前端联调 | `CorsConfig` 仅放行 `http://localhost:5173`（Vite 默认端口） |

完成本指南后，你应能：

- 使用 JDK 17 + Maven 编译并启动后端；
- 连接本机 MySQL 8 与 Redis；
- 人工执行 DDL 初始化库表；
- 访问 Swagger UI 调试 API；
- （可选）启动 `transf-python` Worker，跑通 Redis Stream AI 导入链路。

---

## 2. 部署架构速览

```text
┌─────────────────┐     HTTP :8080      ┌──────────────────────┐
│  Vite 前端       │ ──────────────────► │  Java quiz-backend    │
│  localhost:5173 │                     │  Spring Boot 3.5.x    │
└─────────────────┘                     └──────────┬───────────┘
                                                 │
                    ┌────────────────────────────┼────────────────────────────┐
                    ▼                            ▼                            ▼
              MySQL 8.x                    Redis 7.x                  ./data/upload
           (quiz_atlas)              Stream / 缓存 / 限流              上传文件落盘
                    ▲                            ▲
                    │                            │
                    └──────── transf-python Worker（可选）
                         XREADGROUP quiz:task:stream
                         MinerU API + DeepSeek API
```

**说明：** 新 AI 导入链路中，大模型与 MinerU 调用在 **Python Worker** 完成；Java 端负责入队、状态轮询、预览与幂等落库。不启用 AI 导入时，可不配置 `DEEPSEEK_API_KEY` / Python 环境，但 Redis 仍被热点缓存与（若测试导入）任务系统使用。

---

## 3. 前置软件清单

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | **17**（与 `pom.xml` 中 `java.version` 一致） | 编译与运行 |
| Maven | **≥ 3.8** | 依赖与构建 |
| MySQL | **≥ 8.0**，字符集 **utf8mb4** | 持久化 |
| Redis | 稳定版即可（建议 6.x/7.x） | 缓存、Stream、限流 |
| Git | 任意较新版本 | 拉取代码 |
| IDE（推荐） | IntelliJ IDEA | 开发调试；需 **Lombok** 插件 |
| Python（可选） | **≥ 3.10** | AI Worker：`transf-python/` |

---

## 4. 安装 JDK 17

### 4.1 下载与安装

1. 从 [Adoptium](https://adoptium.net/) 或 Oracle 下载 **JDK 17** Windows x64 安装包。
2. 安装到无空格路径，例如：`C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot`。

### 4.2 配置环境变量

**系统属性 → 高级 → 环境变量：**

| 变量名 | 示例值 |
|--------|--------|
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot` |
| `Path`（追加） | `%JAVA_HOME%\bin` |

PowerShell 验证：

```powershell
java -version
# 应显示 17.x
echo $env:JAVA_HOME
```

---

## 5. 安装 Maven

1. 下载 [Apache Maven](https://maven.apache.org/download.cgi) 二进制 zip，解压到例如 `D:\dev\apache-maven-3.9.x`。
2. 环境变量：

| 变量名 | 示例值 |
|--------|--------|
| `MAVEN_HOME` | `D:\dev\apache-maven-3.9.x` |
| `Path`（追加） | `%MAVEN_HOME%\bin` |

验证：

```powershell
mvn -v
# Java version 应为 17
```

**加速（可选）：** 在 `%USERPROFILE%\.m2\settings.xml` 配置国内镜像（阿里云等），缩短首次 `mvn install` 时间。

---

## 6. 安装与配置 MySQL 8

### 6.1 安装

- 使用 [MySQL Installer](https://dev.mysql.com/downloads/installer/) 安装 **MySQL Server 8.0**。
- 安装时选择 **UTF-8（utf8mb4）** 作为默认字符集。

### 6.2 创建数据库

使用 **MySQL Workbench** 或命令行（需已加入 `Path`）：

```sql
CREATE DATABASE IF NOT EXISTS quiz_atlas
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

开发环境可使用 root；生产请使用独立账号（见 Linux 指南）。

### 6.3 与项目数据源对齐

`application.yaml` 默认 JDBC URL：

```text
jdbc:mysql://127.0.0.1:3306/quiz_atlas?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

用户名/密码通过环境变量 `DB_USER`、`DB_PASSWORD` 注入，默认均为 `root`。

---

## 7. 安装与配置 Redis

### 7.1 安装方式（任选其一）

| 方式 | 说明 |
|------|------|
| **Memurai** | Windows 原生 Redis 兼容方案，适合不想用 WSL 的开发者 |
| **WSL2 + Ubuntu** | 在 WSL 内 `sudo apt install redis-server`，Windows 侧连 `127.0.0.1:6379` |
| **Docker Desktop** | `docker run -d -p 6379:6379 redis:7`（需自行安装 Docker） |

### 7.2 验证

```powershell
# 若 redis-cli 在 PATH 中
redis-cli ping
# 期望返回 PONG
```

无密码时，`REDIS_PASSWORD` 留空即可（与 `application.yaml` 默认一致）。

---

## 8. 获取项目代码

```powershell
cd D:\C1ouD\Shua\backend   # 替换为你的实际路径
git status
```

仓库关键目录：

| 路径 | 说明 |
|------|------|
| `src/main/resources/application.yaml` | 数据源、Redis、JWT、上传目录、AI 超时等 |
| `sql/schema/init_core_tables.sql` | 核心表 DDL（**须人工执行**） |
| `transf-python/` | 可选 AI Worker |
| `docs/环境部署配置/` | 环境与部署文档 |

---

## 9. 环境变量

敏感与环境相关配置**均通过环境变量注入**（见 `application.yaml` 占位符）。下表与 **AGENTS.md** 保持一致；另附本项目在 yaml 中扩展的变量。

### 9.1 与 AGENTS.md 一致的核心变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | `127.0.0.1` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `quiz_atlas` | 数据库名 |
| `DB_USER` | `root` | 数据库用户 |
| `DB_PASSWORD` | `root` | 数据库密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | （空） | Redis 密码 |
| `DEEPSEEK_API_KEY` | （空） | 大模型 API Key（**不跑 AI 导入时可空**；新链路中 Java 端不直接调 LLM，实际由 Python 的 `LLM_API_KEY` 使用） |
| `JWT_SECRET` | `YOUR_SUPER_SECRET_KEY_MUST_BE_LONG_ENOUGH` | JWT HMAC 签名密钥，**开发也建议改为随机长串** |

### 9.2 application.yaml 扩展变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `AI_IMPORT_RATE_LIMIT_PER_HOUR` | `5` | 每用户每小时 AI 导入提交次数（Redis 滑动窗口） |
| `AI_IMPORT_TASK_TIMEOUT_MS` | `1800000` | 任务看门狗超时（30 分钟，毫秒），须 ≥ MinerU 轮询超时 + LLM 耗时 |
| `FILE_UPLOAD_DIR` | `./data/upload` | 上传文件落盘目录；**Java 与 Python Worker 必须能访问同一路径**（建议改为绝对路径） |

### 9.3 PowerShell 会话级设置（当前窗口有效）

```powershell
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "3306"
$env:DB_NAME = "quiz_atlas"
$env:DB_USER = "root"
$env:DB_PASSWORD = "你的密码"

$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = ""

$env:JWT_SECRET = "请替换为至少32字符的随机字符串"

# 建议 Windows 下使用绝对路径，便于 Python Worker 读取 file:// 路径
$env:FILE_UPLOAD_DIR = "D:\C1ouD\Shua\backend\data\upload"

# 可选
# $env:AI_IMPORT_RATE_LIMIT_PER_HOUR = "5"
# $env:AI_IMPORT_TASK_TIMEOUT_MS = "1800000"
```

### 9.4 用户级持久化（推荐）

**设置 → 系统 → 关于 → 高级系统设置 → 环境变量 → 用户变量**，追加上表变量，避免每次开终端重复输入。

### 9.5 JWT 与其它内置配置（非环境变量）

| 配置项 | 值 | 位置 |
|--------|-----|------|
| JWT 过期时间 | `864000000` ms（10 天） | `application.yaml` → `jwt.expiration` |
| 单文件上传上限 | `10MB` | `spring.servlet.multipart.max-file-size` |
| 整请求上限 | `12MB` | `spring.servlet.multipart.max-request-size` |

---

## 10. 数据库初始化（须人工执行）

> **注意：** 按项目规范，自动化 Agent **不会**代你执行数据库命令；以下步骤请在本机手动完成。

### 10.1 执行 DDL

在 PowerShell 中（将密码改为你的 root 密码）：

```powershell
cd D:\C1ouD\Shua\backend
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS quiz_atlas DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p quiz_atlas < sql\schema\init_core_tables.sql
```

或在 MySQL Workbench 中打开 `sql/schema/init_core_tables.sql` 对 `quiz_atlas` 执行。

### 10.2 初始化结果校验

脚本会创建（并在开头 `DROP` 后重建）以下表：

- `sys_user` — 用户（`username` + `is_deleted` 唯一）
- `question_bank` — 题库
- `question` — 试题（含 `options_json`、`answer_json` JSON 列）
- `wrong_question` — 错题本

验证：

```sql
USE quiz_atlas;
SHOW TABLES;
```

### 10.3 重要说明

- `init_core_tables.sql` 含 **DROP TABLE**，切勿对已有生产数据直接执行。
- 开发环境重复初始化会清空上述四张表数据。

---

## 11. 构建与运行

### 11.1 编译（跳过测试）

```powershell
cd D:\C1ouD\Shua\backend
mvn clean install -DskipTests
```

产物：`target\quiz-backend-0.0.1-SNAPSHOT.jar`（`pom.xml` 中 `artifactId` 为 `quiz-backend`）。

### 11.2 开发启动（推荐）

```powershell
mvn spring-boot:run
```

启动成功日志中应出现 Tomcat 监听 **8080**，以及文件存储目录就绪信息（`LocalFileStorageService`）。

### 11.3 打包后运行

```powershell
mvn package -DskipTests
java -jar target\quiz-backend-0.0.1-SNAPSHOT.jar
```

### 11.4 运行单元测试

```powershell
mvn test
```

测试使用 H2 内存库，**不依赖**本机 MySQL，但需 JDK 17 与 Maven 正常。

---

## 12. IntelliJ IDEA 开发配置

### 12.1 导入项目

1. **File → Open** 选择仓库根目录（含 `pom.xml`）。
2. 等待 Maven 索引与依赖下载完成。
3. **File → Project Structure → Project SDK** 选择 **17**。

### 12.2 Lombok

- **Settings → Plugins** 安装 **Lombok**。
- **Settings → Build → Compiler → Annotation Processors** 勾选 **Enable annotation processing**。

### 12.3 运行配置

- Main class：`cn.heycloudream.quiz_backend.QuizBackendApplication`
- **Environment variables**：填入第 9 节变量（或勾选「从系统环境继承」）。
- **Working directory**：仓库根目录（保证 `./data/upload` 相对路径正确）。

### 12.4 热启动与调试

- 使用 **Debug** 运行上述 Main；修改业务代码后 **Build → Rebuild Project**，DevTools 未在 `pom.xml` 中引入时，需手动重启或依赖 IDEA 热交换（方法体小改）。
- 断点调试：在 `Controller` / `Service` 层打断点，请求经 Swagger 或前端触发。

---

## 13. 前端联调与 CORS

`CorsConfig` 当前写死：

- 允许来源：`http://localhost:5173`
- 允许凭证：`true`
- 允许方法/头：全部

若前端端口不是 5173，需修改 `CorsConfig.FRONTEND_ORIGIN` 并重新编译，或临时用 Swagger / Postman 联调。

前端请求需带 JWT 的接口，在 Header 中加：

```http
Authorization: Bearer <登录返回的token>
```

---

## 14. Swagger / OpenAPI

启动后访问：

| 地址 | 说明 |
|------|------|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

`WebMvcConfig` 白名单（**无需登录**）包括：

- `/api/v1/users/register`、`/api/v1/users/login`
- `/api/v1/question-banks/public`
- `/api/v1/question-banks/*/hot-practice-detail`
- `/swagger-ui/**`、`/swagger-ui.html`、`/v3/api-docs/**`

其余 `/api/**` 路径需 Bearer Token。Swagger UI 已开启 **persist-authorization**，可在 Authorize 中填入 Token 后持久化。

---

## 15. 可选：Python AI Worker（transf-python）

仅当需要调试 **文档 → MinerU → LLM → 预览 → 确认落库** 全链路时配置。

### 15.1 前置

- Java 后端与 Redis 已启动；
- `FILE_UPLOAD_DIR` 与 Worker 可读路径一致；
- 已申请 [MinerU](https://mineru.net/) Token 与 DeepSeek（或兼容 OpenAI 接口）API Key。

### 15.2 安装依赖

```powershell
cd D:\C1ouD\Shua\backend\transf-python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

若 PowerShell 禁止脚本执行，可先：`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

### 15.3 配置 `.env`

复制 `transf-python\.env.example` 为 `.env`：

```ini
REDIS_URL=redis://127.0.0.1:6379/0
REDIS_STREAM=quiz:task:stream
REDIS_GROUP=quiz-ai-workers
REDIS_CONSUMER=worker_node_1

MINERU_TOKEN=你的MinerU令牌
MINERU_BASE_URL=https://mineru.net/api/v4

LLM_API_KEY=你的DeepSeek密钥
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat

# 仅调试 MinerU、不调用大模型时可设 true
SKIP_LLM=false
```

与 Java 端对齐的 Redis 约定见 `QuizRedisCacheConstants`：Stream `quiz:task:stream`，消费组 `quiz-ai-workers`。

### 15.4 启动 Worker

```powershell
python main.py
```

### 15.5 仅调试 MinerU

设置 `SKIP_LLM=true` 可跳过 LLM（`config.py` 校验），用于验证 Redis 消费与 MinerU 链路。

更细的 Worker 规约见：[AI解析服务Python端开发规约.md](../AI解析服务Python端开发规约.md)。

---

## 16. 功能验证清单

按顺序自检：

1. **健康启动**：`mvn spring-boot:run` 无报错，8080 可访问。
2. **Swagger**：打开 swagger-ui，调用 `POST /api/v1/users/register` 注册用户。
3. **登录**：`POST /api/v1/users/login` 获取 token，Authorize 填入 Bearer。
4. **题库**：创建私有题库、增删改试题（写操作会驱逐热点缓存 `smart_quiz:bank_detail:{id}`）。
5. **公开大厅**：未登录访问 `GET /api/v1/question-banks/public`。
6. **Redis**：`redis-cli KEYS "smart_quiz:*"` 在访问热点公开题库后可看到缓存 Key（可选）。
7. **AI 导入（可选）**：上传文件 → 轮询任务状态 → 预览 → 批量确认落库；Worker 日志无持续报错。

---

## 17. 常见问题

### 17.1 启动报错：Communications link failure（MySQL）

- 确认 MySQL 服务已启动（服务管理器中的 MySQL80）。
- 核对 `DB_*` 环境变量与账号密码。
- 确认已创建库 `quiz_atlas` 且执行过 DDL。

### 17.2 启动报错：Unable to connect to Redis

- 确认 Redis 进程监听 6379。
- WSL Redis 需在 WSL 内 `sudo service redis-server start`。
- 若设置了密码，必须配置 `REDIS_PASSWORD`。

### 17.3 表不存在 / Table 'quiz_atlas.xxx' doesn't exist

人工执行 `sql/schema/init_core_tables.sql`（见第 10 节）。

### 17.4 401 未提供有效的认证 Token

除白名单外，所有 `/api/**` 需 Header：`Authorization: Bearer <token>`。

### 17.5 前端跨域失败

确认前端源为 `http://localhost:5173`，或修改 `CorsConfig` 后重启后端。

### 17.6 AI 任务一直 PROCESSING 后变 FAILED

- 确认 Python Worker 已启动且 `MINERU_TOKEN`、`LLM_API_KEY` 有效。
- `AI_IMPORT_TASK_TIMEOUT_MS` 默认 30 分钟；MinerU 轮询默认最长 1800s，超大文档需调大超时（见 `application.yaml` 注释）。
- 检查 `FILE_UPLOAD_DIR`：Java 写入的 `file://` 绝对路径 Worker 能否 `open()`。

### 17.7 Maven 编译 Lombok 报错

安装 Lombok 插件并开启 Annotation Processing（见第 12 节）。

### 17.8 端口 8080 被占用

```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

或在启动时临时指定：`mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`

### 17.9 上传超过大小限制

单文件最大 **10MB**（`max-file-size`），整请求 **12MB**（`max-request-size`）。

---

## 18. 相关文档

| 文档 | 说明 |
|------|------|
| [AGENTS.md](../../AGENTS.md) | 项目规范、环境变量、构建命令 |
| [README.md](../../README.md) | 项目概览 |
| [docs/Background.md](../Background.md) | 业务背景 |
| [docs/AI解析服务Python端开发规约.md](../AI解析服务Python端开发规约.md) | Worker 与 Redis 约定 |
| [Linux生产环境部署指南.md](./Linux生产环境部署指南.md) | 生产部署 |

---

*文档版本与仓库实现同步；若 `application.yaml` 或鉴权白名单变更，请以代码为准并更新本文档。*
