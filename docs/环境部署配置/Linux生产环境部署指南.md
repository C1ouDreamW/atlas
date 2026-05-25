# Linux 生产环境部署指南

本文档面向 **Linux 服务器**（推荐 Ubuntu 22.04 LTS / Debian 12 / CentOS Stream 9 等），指导将 Atlas 后端以生产标准部署：专用系统用户、强密钥、systemd 托管、防火墙、可选 Nginx 反向代理、日志与备份、开机自启，以及可选 Python AI Worker。

配置事实来源：`application.yaml`、`pom.xml`、`IShuaRedisCacheConstants`、`WebMvcConfig`、`CorsConfig`、`transf-python/config.py` 等。

---

## 1. 适用范围与生产目标

| 项 | 说明 |
|----|------|
| 部署形态 | 单节点或小规模 VM（JDK 17 + MySQL 8 + Redis + 可选 Nginx） |
| Java 应用 | `ishua-backend-0.0.1-SNAPSHOT.jar`，默认端口 **8080** |
| 进程管理 | **systemd**（推荐） |
| 安全基线 | 非 root 运行、强 `JWT_SECRET`、数据库独立账号、Redis 密码、防火墙最小放行 |
| AI 链路 | Java 入队 + **transf-python** Worker 消费 Redis Stream |

生产环境**不要**直接对公网暴露 8080；对外通常仅开放 **80/443**（Nginx），MySQL/Redis **仅本机或内网**。

---

## 2. 生产架构

```text
                    Internet
                        │
                        ▼
                 ┌─────────────┐
                 │   Nginx     │  :443 TLS（可选）
                 │  反代 /api  │
                 └──────┬──────┘
                        │ 127.0.0.1:8080
                        ▼
              ┌─────────────────────┐
              │  ishua-backend.jar  │  用户: atlas
              │  Spring Boot 3.5   │
              └─────────┬───────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   MySQL 8.x       Redis 7.x      /var/lib/atlas/upload
   127.0.0.1       127.0.0.1         共享文件目录
        ▲               ▲
        │               │
        └───── transf-python Worker (systemd: atlas-worker)
              ishua:task:stream / ishua-ai-workers
```

**CORS 说明：** 当前 `CorsConfig` 仅允许 `http://localhost:5173`，**不适合生产前端域名**。上线前须将 `FRONTEND_ORIGIN` 改为正式前端地址（或改为配置化），重新打包部署；或由 **Nginx 同域托管** 前后端以避免跨域。

---

## 3. 服务器准备

### 3.1 硬件与系统建议

| 资源 | 最低建议 | 说明 |
|------|----------|------|
| CPU | 2 核 | AI Worker 与 MinerU 轮询会占 CPU |
| 内存 | 4 GB | MySQL + Redis + Java + Python 同机时建议 8 GB |
| 磁盘 | 40 GB+ | 上传目录与 MySQL 数据增长 |
| OS | Ubuntu 22.04 LTS | 下文命令以 `apt` 为例 |

### 3.2 系统更新与基础工具

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git unzip ca-certificates
```

### 3.3 时区

```bash
sudo timedatectl set-timezone Asia/Shanghai
```

与 JDBC `serverTimezone=Asia/Shanghai` 一致。

---

## 4. 安装 JDK 17 与 Maven

### 4.1 OpenJDK 17（Ubuntu/Debian）

```bash
sudo apt install -y openjdk-17-jdk maven
java -version   # 应显示 17
mvn -v
```

### 4.2 仅运行 JAR（可选）

若构建在 CI 本机完成，服务器只需 JRE：

```bash
sudo apt install -y openjdk-17-jre-headless
```

---

## 5. 安装 MySQL 8

### 5.1 安装

```bash
sudo apt install -y mysql-server
sudo systemctl enable --now mysql
```

### 5.2 创建库与专用账号（推荐）

```bash
sudo mysql
```

```sql
CREATE DATABASE IF NOT EXISTS ishua_atlas
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER 'quiz_app'@'localhost' IDENTIFIED BY '请替换为强密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON ishua_atlas.* TO 'quiz_app'@'localhost';
FLUSH PRIVILEGES;
```

生产禁止使用 root 连接应用。

### 5.3 初始化表结构（须人工执行）

在部署机上传仓库后：

```bash
cd /opt/atlas/ishua-backend   # 你的部署路径
mysql -u quiz_app -p ishua_atlas < sql/schema/init_core_tables.sql
```

> **警告：** `init_core_tables.sql` 含 `DROP TABLE`，仅用于**首次**建库；已有数据环境请用迁移脚本，勿直接执行全量 DDL。

### 5.4 MySQL 安全加固（建议）

- `bind-address = 127.0.0.1`（`/etc/mysql/mysql.conf.d/mysqld.cnf`）
- 禁止远程 root 登录
- 定期备份（见第 14 节）

---

## 6. 安装 Redis

```bash
sudo apt install -y redis-server
```

### 6.1 生产建议配置

编辑 `/etc/redis/redis.conf`（路径因发行版而异）：

```conf
bind 127.0.0.1 ::1
protected-mode yes
requirepass 请替换为强密码
```

```bash
sudo systemctl restart redis-server
sudo systemctl enable redis-server
```

应用侧设置 `REDIS_PASSWORD` 与 `REDIS_HOST=127.0.0.1`。

---

## 7. 创建专用系统用户（非 root）

```bash
sudo useradd -r -m -s /bin/bash atlas
sudo mkdir -p /opt/atlas/ishua-backend
sudo mkdir -p /var/lib/atlas/upload
sudo mkdir -p /var/log/atlas
sudo chown -R atlas:atlas /opt/atlas /var/lib/atlas /var/log/atlas
```

后续 Java、Python 进程均以 **`atlas`** 运行。

---

## 8. 环境变量

### 8.1 与 AGENTS.md 一致的核心变量

| 变量 | 默认值 | 生产说明 |
|------|--------|----------|
| `DB_HOST` | `127.0.0.1` | 本机 MySQL |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `ishua_atlas` | |
| `DB_USER` | `root` | **改为 `quiz_app` 等专用账号** |
| `DB_PASSWORD` | `root` | **强密码** |
| `REDIS_HOST` | `127.0.0.1` | |
| `REDIS_PORT` | `6379` | |
| `REDIS_PASSWORD` | （空） | **必须设置** |
| `DEEPSEEK_API_KEY` | （空） | Java 新链路不直接调 LLM；可留空 |
| `JWT_SECRET` | 占位符 | **必须替换**，建议 ≥ 32 字节随机（`openssl rand -base64 48`） |

### 8.2 application.yaml 扩展变量

| 变量 | 默认值 | 生产说明 |
|------|--------|----------|
| `AI_IMPORT_RATE_LIMIT_PER_HOUR` | `5` | 可按运营调整 |
| `AI_IMPORT_TASK_TIMEOUT_MS` | `1800000` | 30 分钟；大文档场景勿随意改小 |
| `FILE_UPLOAD_DIR` | `./data/upload` | **生产设为** `/var/lib/atlas/upload` |

### 8.3 systemd 环境文件

```bash
sudo nano /etc/atlas/ishua-backend.env
```

示例（权限 600，属主 root:atlas 或 atlas:atlas）：

```bash
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=ishua_atlas
DB_USER=quiz_app
DB_PASSWORD=强密码

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=Redis强密码

JWT_SECRET=由openssl生成的长随机串

FILE_UPLOAD_DIR=/var/lib/atlas/upload
AI_IMPORT_RATE_LIMIT_PER_HOUR=5
AI_IMPORT_TASK_TIMEOUT_MS=1800000
```

```bash
sudo chmod 600 /etc/atlas/ishua-backend.env
sudo chown root:atlas /etc/atlas/ishua-backend.env
```

---

## 9. 构建与部署应用

### 9.1 在构建机打包

```bash
git clone <你的仓库地址> ishua-backend
cd ishua-backend
mvn clean package -DskipTests
```

产物：`target/ishua-backend-0.0.1-SNAPSHOT.jar`。

### 9.2 上传到服务器

```bash
scp target/ishua-backend-0.0.1-SNAPSHOT.jar atlas@your-server:/opt/atlas/ishua-backend/
scp -r sql atlas@your-server:/opt/atlas/ishua-backend/
```

### 9.3 目录布局建议

```text
/opt/atlas/ishua-backend/
  ├── ishua-backend-0.0.1-SNAPSHOT.jar
  └── sql/

/var/lib/atlas/upload/          # FILE_UPLOAD_DIR
/var/log/atlas/                 # 应用日志（若重定向）
/etc/atlas/ishua-backend.env     # 环境变量
```

---

## 10. systemd：Java 后端

创建 `/etc/systemd/system/ishua-backend.service`：

```ini
[Unit]
Description=Atlas Quiz Backend
After=network.target mysql.service redis-server.service
Wants=mysql.service redis-server.service

[Service]
Type=simple
User=atlas
Group=atlas
WorkingDirectory=/opt/atlas/ishua-backend
EnvironmentFile=/etc/atlas/ishua-backend.env
ExecStart=/usr/bin/java -jar /opt/atlas/ishua-backend/ishua-backend-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
StandardOutput=append:/var/log/atlas/ishua-backend.log
StandardError=append:/var/log/atlas/ishua-backend.err.log

# 生产 JVM 示例（按内存调整）
Environment=JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

[Install]
WantedBy=multi-user.target
```

> 若使用 `Environment=JAVA_OPTS=...`，需在 `ExecStart` 中写为 `java $JAVA_OPTS -jar ...`，或直接把参数写在 `ExecStart` 一行。

启用：

```bash
sudo systemctl daemon-reload
sudo systemctl enable ishua-backend
sudo systemctl start ishua-backend
sudo systemctl status ishua-backend
```

日志：

```bash
sudo journalctl -u ishua-backend -f
tail -f /var/log/atlas/ishua-backend.log
```

---

## 11. 可选：systemd 部署 Python AI Worker

### 11.1 安装 Python 环境

```bash
sudo apt install -y python3 python3-venv python3-pip
sudo -u atlas -i
cd /opt/atlas/transf-python   # 将 transf-python 目录部署到此
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
nano .env
```

### 11.2 Worker `.env` 要点

```ini
REDIS_URL=redis://:Redis强密码@127.0.0.1:6379/0
REDIS_STREAM=ishua:task:stream
REDIS_GROUP=ishua-ai-workers
REDIS_CONSUMER=worker_prod_1

MINERU_TOKEN=生产令牌
LLM_API_KEY=生产DeepSeek密钥
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-chat
```

`REDIS_URL` 密码须与 `REDIS_PASSWORD` 一致。Stream 与消费组名称须与 Java `IShuaRedisCacheConstants` 一致。

### 11.3 systemd 单元

`/etc/systemd/system/atlas-ai-worker.service`：

```ini
[Unit]
Description=Atlas AI Import Python Worker
After=redis-server.service ishua-backend.service
Wants=redis-server.service

[Service]
Type=simple
User=atlas
Group=atlas
WorkingDirectory=/opt/atlas/transf-python
ExecStart=/opt/atlas/transf-python/.venv/bin/python main.py
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/atlas/ai-worker.log
StandardError=append:/var/log/atlas/ai-worker.err.log

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable atlas-ai-worker
sudo systemctl start atlas-ai-worker
```

---

## 12. 防火墙

### 12.1 UFW（Ubuntu 常见）

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
# 不要对公网开放 8080、3306、6379
sudo ufw enable
sudo ufw status
```

### 12.2 firewalld（RHEL/CentOS）

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload
```

---

## 13. 可选：Nginx 反向代理

### 13.1 安装

```bash
sudo apt install -y nginx
```

### 13.2 示例站点（HTTP，TLS 请用 certbot 另配）

`/etc/nginx/sites-available/atlas-api`：

```nginx
upstream ishua_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;

    client_max_body_size 12m;   # 对齐 spring multipart max-request-size

    location / {
        proxy_pass http://ishua_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/atlas-api /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 13.3 HTTPS

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.example.com
```

### 13.4 生产 Swagger 建议

当前 `springdoc.swagger-ui.enabled=true`，生产环境建议：

- 仅内网或 VPN 访问管理端口；或
- Nginx 对 `/swagger-ui.html`、`/v3/api-docs` 做 IP 白名单 / Basic Auth；或
- 通过配置关闭 Swagger（需改 `application.yaml` 并重新发布）。

---

## 14. 日志、轮转与备份

### 14.1 应用日志

systemd 已追加到 `/var/log/atlas/*.log`。可配置 **logrotate**，例如 `/etc/logrotate.d/atlas`：

```text
/var/log/atlas/*.log {
    weekly
    rotate 8
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
}
```

### 14.2 MySQL 备份（示例 cron）

```bash
sudo crontab -e
```

```cron
0 3 * * * mysqldump -u quiz_app -p'密码' --single-transaction ishua_atlas | gzip > /var/backups/ishua_atlas_$(date +\%F).sql.gz
```

确保 `/var/backups` 存在且权限安全。

### 14.3 Redis 持久化

默认 RDB 通常足够；任务状态与 Stream 数据有 TTL，**不可替代 MySQL 备份**。重要业务数据以 MySQL 为准。

### 14.4 上传目录备份

```bash
sudo tar -czf /var/backups/atlas-upload-$(date +%F).tar.gz /var/lib/atlas/upload
```

---

## 15. 开机自启与健康检查

| 服务 | 启用命令 |
|------|----------|
| MySQL | `sudo systemctl enable mysql` |
| Redis | `sudo systemctl enable redis-server` |
| Java | `sudo systemctl enable ishua-backend` |
| Worker | `sudo systemctl enable atlas-ai-worker` |
| Nginx | `sudo systemctl enable nginx` |

**健康检查（本机）：**

```bash
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/v3/api-docs
# 期望 200
```

项目引入 `spring-boot-starter-actuator`，但未在 `application.yaml` 定制端点；生产若暴露 actuator，须限制访问，避免泄露敏感信息。

**业务看门狗：** `AiImportTaskWatchdog` 每 **2 分钟** 扫描超时 `PROCESSING` 任务（默认超时 **30 分钟**），无需单独部署。

---

## 16. 安全清单（生产必查）

- [ ] 应用以 **atlas** 等非特权用户运行
- [ ] `JWT_SECRET`、数据库与 Redis 密码为强随机值
- [ ] `/etc/atlas/ishua-backend.env` 权限 **600**
- [ ] MySQL、Redis **不监听公网**
- [ ] 防火墙仅开放 SSH + 80/443
- [ ] 修改 `CorsConfig` 为生产前端域或同域部署
- [ ] Swagger 不对公网裸奔
- [ ] `init_core_tables.sql` 不在已有数据环境误执行
- [ ] `FILE_UPLOAD_DIR` 权限仅 `atlas` 可写，防止任意文件读取
- [ ] 定期 `mysqldump` 与上传目录备份

---

## 17. 功能验证

1. `systemctl status ishua-backend` 为 active。
2. 本机 `curl http://127.0.0.1:8080/v3/api-docs` 返回 JSON。
3. 经 Nginx：`curl https://api.example.com/api/v1/question-banks/public`（若已配置域名）。
4. 注册/登录获取 JWT，访问受保护接口。
5. 启用 Worker 后提交 AI 导入，轮询任务至 `PARSED` 并可预览落库。
6. `redis-cli -a <密码> XINFO GROUPS ishua:task:stream` 可见消费组 `ishua-ai-workers`。

---

## 18. 常见问题

### 18.1 ishua-backend 启动后立即退出

- `journalctl -u ishua-backend -n 100` 查看栈追踪。
- 常见原因：MySQL/Redis 未启动、密码错误、表未初始化。

### 18.2 无法连接 Redis

- `requirepass` 与 `REDIS_PASSWORD` 不一致。
- URL 中特殊字符需编码（Python `REDIS_URL`）。

### 18.3 AI 任务失败或 Watchdog 误杀

- 确认 `AI_IMPORT_TASK_TIMEOUT_MS` ≥ MinerU `MINERU_POLL_TIMEOUT_SECONDS`（默认 1800）+ LLM 时间。
- Worker 日志：`/var/log/atlas/ai-worker.err.log`。
- Java 与 Worker 是否共用 **同一绝对路径** `FILE_UPLOAD_DIR`。

### 18.4 413 Request Entity Too Large（Nginx）

增大 `client_max_body_size`（建议 ≥ 12m，见第 13 节）。

### 18.5 跨域错误

生产前端域名须写入 `CorsConfig` 或改为配置项后重新打包。

### 18.6 权限拒绝写上传目录

```bash
sudo chown -R atlas:atlas /var/lib/atlas/upload
sudo chmod 750 /var/lib/atlas/upload
```

### 18.7 仅重启应用

```bash
sudo systemctl restart ishua-backend
sudo systemctl restart atlas-ai-worker
```

---

## 19. 关键配置摘要（与代码对齐）

| 类别 | 值 |
|------|-----|
| Spring Boot | 3.5.14（`pom.xml` parent） |
| Java | 17 |
| 默认 HTTP 端口 | 8080 |
| 数据库名 | `ishua_atlas` |
| JWT 过期 | `864000000` ms（10 天，`application.yaml`） |
| Redis Stream | `ishua:task:stream` |
| 消费组 | `ishua-ai-workers` |
| 热点缓存 Key 前缀 | `smart_ishua:bank_detail:{bankId}` |
| 上传目录 | `FILE_UPLOAD_DIR`，默认 `./data/upload`，生产建议 `/var/lib/atlas/upload` |
| Swagger | `/swagger-ui.html`、`/v3/api-docs` |
| 鉴权白名单 | 注册/登录、公开题库列表、热点详情、Swagger 路径（见 `WebMvcConfig`） |

---

## 20. 相关文档

| 文档 | 说明 |
|------|------|
| [AGENTS.md](../../AGENTS.md) | 环境变量与构建命令 |
| [Windows开发环境部署指南.md](./Windows开发环境部署指南.md) | 本地 Windows 开发 |
| [docs/AI解析服务Python端开发规约.md](../AI解析服务Python端开发规约.md) | Worker Redis 协议 |
| [docs/Background.md](../Background.md) | 业务背景 |

---

*生产部署请以当前分支代码为准；Docker / K8s 编排为规划项（AGENTS.md 标注待实现），本文采用传统 systemd 方案。*
