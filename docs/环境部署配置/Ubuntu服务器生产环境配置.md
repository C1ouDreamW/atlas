# 🚀 Ubuntu 服务器生产环境部署指南

本文档将指导您如何在一台全新的 Ubuntu Server (推荐 20.04 LTS 或 22.04 LTS) 上，从零搭建本在线题库平台（Atlas）的生产运行环境。

## 阶段一：基础环境准备与系统配置

### 1. 更新系统包
通过 SSH 登录到您的 Ubuntu 服务器，首先更新软件包列表并升级已有软件：
```bash
sudo apt update && sudo apt upgrade -y
```

### 2. 配置防火墙 (UFW)
生产环境下务必开启防火墙。我们需要开放 SSH 端口、Web 端口等：
```bash
sudo ufw allow 22/tcp       # 允许 SSH
sudo ufw allow 80/tcp       # 允许 HTTP
sudo ufw allow 443/tcp      # 允许 HTTPS
sudo ufw enable             # 启动防火墙并设置开机自启
sudo ufw status             # 检查防火墙状态
```

---

## 阶段二：安装 Docker 与基础设施

在生产环境中，推荐使用 Docker 部署 MySQL 和 Redis 中间件，以保证环境的一致性和隔离性。

### 1. 安装 Docker 与 Docker Compose
```bash
# 获取一键安装脚本并执行
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo apt-get install docker-compose-plugin -y

# 启动并设置开机自启
sudo systemctl enable docker
sudo systemctl start docker
```

### 2. 部署 MySQL 8.0 & Redis
在服务区上创建一个目录用于存放中间件配置与数据：
```bash
mkdir -p /opt/atlas/middleware
cd /opt/atlas/middleware
```

创建一个 `docker-compose.yml` 文件：
```bash
nano docker-compose.yml
```
填入以下内容（**注意修改 MySQL 的密码和 Redis 的密码**）：
```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: mysql-prod
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: YourStrongPasswordHere # 替换为复杂密码
    ports:
      - "127.0.0.1:3306:3306" # 生产环境仅绑定本地，不暴露公网
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/conf:/etc/mysql/conf.d

  redis:
    image: redis:latest
    container_name: redis-prod
    restart: always
    command: redis-server --requirepass YourRedisPasswordHere # 替换为复杂密码
    ports:
      - "127.0.0.1:6379:6379" # 生产环境仅绑定本地
    volumes:
      - ./redis/data:/data
```

启动数据库缓存服务：
```bash
docker compose up -d
docker ps # 检查运行状态
```

---

## 阶段三：安装 Java 17 运行环境

生产环境无需安装完整 JDK，仅安装 JRE 运行环境即可。但如果需要在服务器上打包，可以安装完整的 JDK。此处推荐直接安装 JDK。

```bash
sudo apt install openjdk-17-jdk -y

# 验证安装
java -version
```

---

## 阶段四：部署 Spring Boot 后端应用 (Systemd 守护进程)

推荐将打包完成的 Spring Boot `jar` 包通过 `systemd` 配置为系统服务，实现后台常驻与开机自启。

### 1. 上传 Jar 包
事先在本地使用 `mvn clean package` 打包项目。
将 `target/quiz-backend-xx.jar` 上传至服务器的 `/opt/atlas/backend/` 目录下（重命名为 `app.jar`）：
```bash
mkdir -p /opt/atlas/backend
# 使用 scp 或 sftp 将本地 jar 包上传到 /opt/atlas/backend/app.jar
```

### 2. 编写 Systemd 服务配置文件
```bash
sudo nano /etc/systemd/system/atlas-backend.service
```

填入以下配置（请自行替换 API Key 和数据库密码）：
```ini
[Unit]
Description=Atlas Quiz Backend Spring Boot User Service
After=syslog.target network.target docker.service

[Service]
User=root
WorkingDirectory=/opt/atlas/backend
# 在这里通过环境变量注入生产环境的具体配置，避免明文写在 jar 包内
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="KIMI_API_KEY=sk-your-kimi-api-key"
Environment="MYSQL_PASSWORD=YourStrongPasswordHere"
Environment="REDIS_PASSWORD=YourRedisPasswordHere" 

ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/atlas/backend/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 3. 启动并测试后端服务
```bash
sudo systemctl daemon-reload           # 重载配置
sudo systemctl start atlas-backend     # 启动服务
sudo systemctl enable atlas-backend    # 设置开机自启动

# 实时查看后端日志，确认启动是否成功
sudo journalctl -u atlas-backend -f
```

---

## 阶段五：配置 Nginx 反向代理 (必备)

出于安全性考虑及便于配置 SSL/HTTPS，前端和后端统一由 Nginx 代理分发。

### 1. 安装 Nginx
```bash
sudo apt install nginx -y
sudo systemctl enable nginx
```

### 2. 配置 Nginx
```bash
sudo nano /etc/nginx/sites-available/atlas
```

写入代理配置：
```nginx
server {
    listen 80;
    server_name api.yourdomain.com; # 替换为您的后端域名或服务器 IP

    # 代理所有请求到后端的 8080 端口
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 允许跨域（视您的微服务规划定，也可以在 Spring Boot 中配置）
        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS, PUT, DELETE';
        add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization';
    }
}
```

启用配置并重启 Nginx：
```bash
sudo ln -s /etc/nginx/sites-available/atlas /etc/nginx/sites-enabled/
sudo nginx -t             # 测试配置文件格式是否正确
sudo systemctl reload nginx
```

---

## 阶段六：测试与验证

在配置好一切后，可以通过以下几种方式验证系统是否正常运行：

1. **测试 Nginx & API 连通性**
   在您的电脑或终端通过 `curl` 或 Postman/浏览器请求您的接口（如 Swagger 地址）：
   ```bash
   curl http://<您的服务器IP>/doc.html
   ```
   如果有 HTML 返回，说明 Nginx 与 Spring Boot 连接畅通。

2. **验证 MySQL 连通性 (容器内)**
   ```bash
   docker exec -it mysql-prod mysql -u root -p
   # 输入密码后应当能成功进入 MySQL 终端
   ```

3. **测试 LLM 多模态解析功能**
   使用 Postman 上传文件调用 `/api/ai/parse` 相关接口，观察后端日志（`journalctl -u atlas-backend -f`）中 Kimi/DeepSeek 接口回调时间与响应是否正常。
