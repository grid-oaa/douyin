# 部署指南

本文档介绍如何在不同环境中部署抖音直播视频提取器。

## 目录
1. [开发环境部署](#开发环境部署)
2. [生产环境部署](#生产环境部署)
3. [Docker部署](#docker部署)
4. [Linux服务部署](#linux服务部署)
5. [性能优化](#性能优化)

---

## 开发环境部署

### 使用IDE运行

#### IntelliJ IDEA
1. 导入项目：`File` → `Open` → 选择项目目录
2. 等待Maven依赖下载完成
3. 找到 `LiveRecorderApplication.java`
4. 右键 → `Run 'LiveRecorderApplication'`

#### Eclipse
1. 导入项目：`File` → `Import` → `Existing Maven Projects`
2. 等待Maven依赖下载完成
3. 找到 `LiveRecorderApplication.java`
4. 右键 → `Run As` → `Java Application`

### 使用命令行运行

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

---

## 生产环境部署

### 1. 构建可执行JAR

```bash
# 清理并打包
mvn clean package -DskipTests

# 生成的JAR文件位于
# target/douyin-live-recorder-1.0.0.jar
```

### 2. 配置生产环境参数

创建 `application-prod.properties`：

```properties
# 服务端口
server.port=8080

# 录制文件存储路径（使用绝对路径）
recording.storage.path=/data/recordings

# 最大并发录制任务数
recording.max-concurrent-tasks=10

# FFmpeg路径
ffmpeg.path=/usr/bin/ffmpeg

# 日志配置
logging.level.root=WARN
logging.level.com.douyin.liverecorder=INFO
logging.file.name=/var/log/douyin-live-recorder/app.log
logging.file.max-size=50MB
logging.file.max-history=30

# HTTP客户端配置
http.client.connect-timeout=10000
http.client.read-timeout=10000
http.client.max-retries=5
```

### 3. 启动应用

```bash
# 基本启动
java -jar target/douyin-live-recorder-1.0.0.jar \
  --spring.profiles.active=prod

# 指定JVM参数
java -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -jar target/douyin-live-recorder-1.0.0.jar \
  --spring.profiles.active=prod

# 后台运行
nohup java -jar target/douyin-live-recorder-1.0.0.jar \
  --spring.profiles.active=prod > app.log 2>&1 &
```

### 4. 验证部署

```bash
# 检查健康状态
curl http://localhost:8080/actuator/health

# 查看日志
tail -f /var/log/douyin-live-recorder/app.log
```

---

## Docker部署

### 1. 创建Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

# 安装FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制JAR文件
COPY target/douyin-live-recorder-1.0.0.jar app.jar

# 创建录制文件目录
RUN mkdir -p /data/recordings

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 构建Docker镜像

```bash
# 首先构建JAR文件
mvn clean package -DskipTests

# 构建Docker镜像
docker build -t douyin-live-recorder:1.0.0 .
```

### 3. 运行Docker容器

```bash
# 基本运行
docker run -d \
  --name douyin-recorder \
  -p 8080:8080 \
  -v /data/recordings:/data/recordings \
  douyin-live-recorder:1.0.0

# 指定环境变量
docker run -d \
  --name douyin-recorder \
  -p 8080:8080 \
  -v /data/recordings:/data/recordings \
  -e RECORDING_MAX_CONCURRENT_TASKS=10 \
  -e LOGGING_LEVEL_ROOT=INFO \
  douyin-live-recorder:1.0.0

# 查看日志
docker logs -f douyin-recorder

# 停止容器
docker stop douyin-recorder

# 删除容器
docker rm douyin-recorder
```

### 4. 使用Docker Compose

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  douyin-recorder:
    image: douyin-live-recorder:1.0.0
    container_name: douyin-recorder
    ports:
      - "8080:8080"
    volumes:
      - /data/recordings:/data/recordings
      - ./logs:/var/log/douyin-live-recorder
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - RECORDING_MAX_CONCURRENT_TASKS=10
      - RECORDING_STORAGE_PATH=/data/recordings
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

启动：
```bash
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

---

## Linux服务部署

### 1. 创建系统服务

创建 `/etc/systemd/system/douyin-recorder.service`：

```ini
[Unit]
Description=Douyin Live Recorder Service
After=network.target

[Service]
Type=simple
User=recorder
Group=recorder
WorkingDirectory=/opt/douyin-recorder
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/douyin-recorder/douyin-live-recorder-1.0.0.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=douyin-recorder

# 环境变量
Environment="RECORDING_STORAGE_PATH=/data/recordings"
Environment="RECORDING_MAX_CONCURRENT_TASKS=10"

[Install]
WantedBy=multi-user.target
```

### 2. 部署步骤

```bash
# 创建用户
sudo useradd -r -s /bin/false recorder

# 创建目录
sudo mkdir -p /opt/douyin-recorder
sudo mkdir -p /data/recordings
sudo mkdir -p /var/log/douyin-live-recorder

# 复制JAR文件
sudo cp target/douyin-live-recorder-1.0.0.jar /opt/douyin-recorder/

# 设置权限
sudo chown -R recorder:recorder /opt/douyin-recorder
sudo chown -R recorder:recorder /data/recordings
sudo chown -R recorder:recorder /var/log/douyin-live-recorder

# 重载systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start douyin-recorder

# 设置开机自启
sudo systemctl enable douyin-recorder

# 查看状态
sudo systemctl status douyin-recorder

# 查看日志
sudo journalctl -u douyin-recorder -f
```

### 3. 服务管理命令

```bash
# 启动服务
sudo systemctl start douyin-recorder

# 停止服务
sudo systemctl stop douyin-recorder

# 重启服务
sudo systemctl restart douyin-recorder

# 查看状态
sudo systemctl status douyin-recorder

# 查看日志
sudo journalctl -u douyin-recorder -n 100 -f

# 禁用开机自启
sudo systemctl disable douyin-recorder
```

---

## 性能优化

### 1. JVM参数优化

```bash
java -Xms1g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/douyin-recorder/heap-dump.hprof \
  -jar douyin-live-recorder-1.0.0.jar
```

**参数说明：**
- `-Xms1g`: 初始堆内存1GB
- `-Xmx4g`: 最大堆内存4GB
- `-XX:+UseG1GC`: 使用G1垃圾收集器
- `-XX:MaxGCPauseMillis=200`: 最大GC暂停时间200ms
- `-XX:+HeapDumpOnOutOfMemoryError`: OOM时生成堆转储

### 2. 系统资源配置

#### 增加文件描述符限制

编辑 `/etc/security/limits.conf`：
```
recorder soft nofile 65536
recorder hard nofile 65536
```

#### 优化网络参数

编辑 `/etc/sysctl.conf`：
```
# 增加TCP连接队列
net.core.somaxconn = 1024

# 增加网络缓冲区
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
```

应用配置：
```bash
sudo sysctl -p
```

### 3. 应用配置优化

```properties
# 增加并发数
recording.max-concurrent-tasks=20

# 增加超时时间
http.client.connect-timeout=10000
http.client.read-timeout=10000

# 优化线程池
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=50
spring.task.execution.pool.queue-capacity=100
```

### 4. 存储优化

- 使用SSD存储录制文件
- 定期清理旧文件
- 考虑使用网络存储（NFS、NAS）

```bash
# 清理7天前的录制文件
find /data/recordings -name "*.mp4" -mtime +7 -delete
```

### 5. 监控和告警

#### 使用Prometheus监控

添加依赖：
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

配置：
```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.metrics.export.prometheus.enabled=true
```

访问指标：
```
http://localhost:8080/actuator/prometheus
```

---

## 安全建议

### 1. 网络安全

```bash
# 使用防火墙限制访问
sudo ufw allow 8080/tcp
sudo ufw enable

# 或使用iptables
sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
```

### 2. 反向代理（Nginx）

```nginx
server {
    listen 80;
    server_name recorder.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. HTTPS配置

使用Let's Encrypt获取免费SSL证书：
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d recorder.example.com
```

---

## 故障排查

### 查看日志
```bash
# 应用日志
tail -f /var/log/douyin-live-recorder/app.log

# 系统日志
sudo journalctl -u douyin-recorder -f

# Docker日志
docker logs -f douyin-recorder
```

### 常见问题

1. **端口被占用**
   ```bash
   # 查找占用端口的进程
   sudo lsof -i :8080
   # 或
   sudo netstat -tulpn | grep 8080
   ```

2. **内存不足**
   ```bash
   # 查看内存使用
   free -h
   # 增加JVM堆内存
   -Xmx4g
   ```

3. **磁盘空间不足**
   ```bash
   # 查看磁盘使用
   df -h
   # 清理旧文件
   find /data/recordings -mtime +7 -delete
   ```

---

## 备份和恢复

### 备份录制文件
```bash
# 使用rsync备份
rsync -avz /data/recordings/ backup@server:/backup/recordings/

# 使用tar压缩备份
tar -czf recordings-backup-$(date +%Y%m%d).tar.gz /data/recordings/
```

### 数据库备份（如果使用）
```bash
# 备份配置
cp /opt/douyin-recorder/application-prod.properties backup/
```

---

**部署完成后，建议进行完整的功能测试，确保所有接口正常工作。**
