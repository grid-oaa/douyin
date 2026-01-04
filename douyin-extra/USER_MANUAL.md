# 抖音直播视频提取器 - 使用手册

## 目录
1. [系统简介](#系统简介)
2. [环境要求](#环境要求)
3. [安装步骤](#安装步骤)
4. [启动项目](#启动项目)
5. [API使用说明](#api使用说明)
6. [配置说明](#配置说明)
7. [常见问题](#常见问题)
8. [错误处理](#错误处理)

---

## 系统简介

抖音直播视频提取器是一个基于Spring Boot的REST API服务，能够自动检测、提取和录制抖音直播流为MP4格式的视频文件。

### 主要功能
- ✅ 自动检测直播状态
- ✅ 提取直播流URL
- ✅ 录制为MP4格式
- ✅ 支持并发录制多个直播间（默认最多5个）
- ✅ 实时查询录制状态
- ✅ 优雅停止录制
- ✅ 完整的错误处理和日志记录

---

## 环境要求

### 必需软件
1. **Java 17 或更高版本**
   - 下载地址：https://www.oracle.com/java/technologies/downloads/
   - 验证安装：`java -version`

2. **Maven 3.6+ 或 Gradle 7+**
   - Maven下载：https://maven.apache.org/download.cgi
   - 验证安装：`mvn -version`

3. **FFmpeg**（用于视频录制）
   - Windows下载：https://ffmpeg.org/download.html
   - 安装后确保`ffmpeg`命令可在命令行中使用
   - 验证安装：`ffmpeg -version`

### 系统要求
- 操作系统：Windows 10/11, Linux, macOS
- 内存：至少2GB可用内存
- 磁盘空间：根据录制需求，建议至少10GB可用空间

---

## 安装步骤

### 1. 安装FFmpeg（重要！）

#### Windows系统
1. 下载FFmpeg：https://www.gyan.dev/ffmpeg/builds/
2. 解压到目录，例如：`C:\ffmpeg`
3. 添加到系统环境变量PATH：
   - 右键"此电脑" → "属性" → "高级系统设置" → "环境变量"
   - 在"系统变量"中找到"Path"，点击"编辑"
   - 添加FFmpeg的bin目录：`C:\ffmpeg\bin`
4. 打开新的命令行窗口，验证：`ffmpeg -version`

#### Linux系统
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install ffmpeg

# CentOS/RHEL
sudo yum install ffmpeg

# 验证安装
ffmpeg -version
```

#### macOS系统
```bash
# 使用Homebrew
brew install ffmpeg

# 验证安装
ffmpeg -version
```

### 2. 克隆或下载项目
```bash
# 如果使用Git
git clone <项目地址>
cd douyin-live-recorder

# 或直接解压下载的项目文件
```

### 3. 构建项目
```bash
# 使用Maven
mvn clean package

# 或使用Maven Wrapper（Windows）
mvnw.cmd clean package

# 构建成功后会在target目录生成jar文件
```

---

## 启动项目

### 方式一：使用Maven运行（开发模式）
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

### 方式二：运行打包的JAR文件（生产模式）
```bash
# 首先构建项目
mvn clean package

# 运行JAR文件
java -jar target/douyin-live-recorder-1.0.0.jar
```

### 方式三：后台运行（Linux/macOS）
```bash
nohup java -jar target/douyin-live-recorder-1.0.0.jar > app.log 2>&1 &
```

### 启动成功标志
看到以下日志表示启动成功：
```
Started LiveRecorderApplication in X.XXX seconds
```

默认服务地址：`http://localhost:8080`

---

## API使用说明

### 基础URL
```
http://localhost:8080/api/recordings
```

### 1. 开始录制直播

**接口：** `POST /api/recordings/start`

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/recordings/start \
  -H "Content-Type: application/json" \
  -d '{"douyinId": "w2511839220"}'
```

**请求参数：**
```json
{
  "douyinId": "w2511839220"  // 抖音号（必填）
}
```

**成功响应：**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "douyinId": "w2511839220",
  "status": "DETECTING",
  "streamUrl": null,
  "outputPath": null,
  "startTime": "2026-01-03T14:30:25",
  "endTime": null,
  "fileSize": 0,
  "error": null
}
```

**状态说明：**
- `PENDING`: 等待中
- `DETECTING`: 检测直播状态中
- `RECORDING`: 录制中
- `STOPPING`: 停止中
- `COMPLETED`: 已完成
- `FAILED`: 失败
- `CANCELLED`: 已取消

---

### 2. 停止录制

**接口：** `POST /api/recordings/{taskId}/stop`

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/recordings/550e8400-e29b-41d4-a716-446655440000/stop
```

**成功响应：**
```json
{
  "success": true,
  "message": "录制已停止"
}
```

---

### 3. 查询录制状态

**接口：** `GET /api/recordings/{taskId}/status`

**请求示例：**
```bash
curl http://localhost:8080/api/recordings/550e8400-e29b-41d4-a716-446655440000/status
```

**成功响应：**
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RECORDING",
  "progress": {
    "duration": 120,      // 已录制时长（秒）
    "fileSize": 15728640, // 文件大小（字节）
    "bitrate": "1024kbps" // 比特率
  },
  "error": null
}
```

---

### 4. 列出所有录制任务

**接口：** `GET /api/recordings`

**请求示例：**
```bash
curl http://localhost:8080/api/recordings
```

**成功响应：**
```json
[
  {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "douyinId": "w2511839220",
    "status": "RECORDING",
    "streamUrl": "http://...",
    "outputPath": "./recordings/w2511839220_20260103_143025.mp4",
    "startTime": "2026-01-03T14:30:25",
    "endTime": null,
    "fileSize": 15728640,
    "error": null
  }
]
```

---

### 5. 健康检查

**接口：** `GET /actuator/health`

**请求示例：**
```bash
curl http://localhost:8080/actuator/health
```

**成功响应：**
```json
{
  "status": "UP",
  "components": {
    "ffmpeg": {
      "status": "UP",
      "details": {
        "available": true
      }
    },
    "storage": {
      "status": "UP",
      "details": {
        "availableSpace": "100GB",
        "totalSpace": "500GB",
        "usagePercentage": 80.0
      }
    }
  }
}
```

---

## 完整使用示例

### 场景：录制一个抖音直播

```bash
# 1. 开始录制
curl -X POST http://localhost:8080/api/recordings/start \
  -H "Content-Type: application/json" \
  -d '{"douyinId": "w2511839220"}'

# 响应会返回taskId，例如：550e8400-e29b-41d4-a716-446655440000

# 2. 查询录制状态（可多次查询）
curl http://localhost:8080/api/recordings/550e8400-e29b-41d4-a716-446655440000/status

# 3. 停止录制
curl -X POST http://localhost:8080/api/recordings/550e8400-e29b-41d4-a716-446655440000/stop

# 4. 查看所有任务
curl http://localhost:8080/api/recordings
```

### 使用Postman测试

1. **开始录制**
   - 方法：POST
   - URL：`http://localhost:8080/api/recordings/start`
   - Headers：`Content-Type: application/json`
   - Body (raw JSON)：
     ```json
     {
       "douyinId": "w2511839220"
     }
     ```

2. **查询状态**
   - 方法：GET
   - URL：`http://localhost:8080/api/recordings/{taskId}/status`

3. **停止录制**
   - 方法：POST
   - URL：`http://localhost:8080/api/recordings/{taskId}/stop`

---

## 配置说明

配置文件位置：`src/main/resources/application.properties`

### 核心配置项

```properties
# 服务端口
server.port=8080

# 录制文件存储路径
recording.storage.path=./recordings

# 最大并发录制任务数
recording.max-concurrent-tasks=5

# 直播检测超时时间（毫秒）
recording.detection-timeout=5000

# 停止录制超时时间（毫秒）
recording.stop-timeout=3000

# FFmpeg可执行文件路径
ffmpeg.path=ffmpeg

# 日志级别
logging.level.com.douyin.liverecorder=DEBUG
```

### 修改配置

#### 方式一：修改配置文件
直接编辑`application.properties`文件，然后重启应用。

#### 方式二：启动时指定参数
```bash
java -jar target/douyin-live-recorder-1.0.0.jar \
  --server.port=9090 \
  --recording.storage.path=/data/recordings \
  --recording.max-concurrent-tasks=10
```

#### 方式三：使用环境变量
```bash
export SERVER_PORT=9090
export RECORDING_STORAGE_PATH=/data/recordings
java -jar target/douyin-live-recorder-1.0.0.jar
```

---

## 常见问题

### Q1: 启动时提示"FFmpeg不可用"
**A:** 
1. 确认FFmpeg已正确安装：`ffmpeg -version`
2. 确认FFmpeg在系统PATH中
3. Windows用户需要重启命令行窗口
4. 如果FFmpeg安装在自定义路径，修改配置：
   ```properties
   ffmpeg.path=C:/ffmpeg/bin/ffmpeg.exe
   ```

### Q2: 录制失败，提示"直播未开始"
**A:** 
- 确认该抖音号当前正在直播
- 检查网络连接是否正常
- 查看日志文件了解详细错误信息

### Q3: 提示"并发限制"错误
**A:** 
- 当前已有5个（默认）录制任务在运行
- 等待其他任务完成或停止部分任务
- 或增加并发限制：`recording.max-concurrent-tasks=10`

### Q4: 录制的视频文件在哪里？
**A:** 
- 默认位置：项目根目录的`./recordings`文件夹
- 文件命名格式：`{抖音号}_{日期}_{时间}.mp4`
- 例如：`w2511839220_20260103_143025.mp4`

### Q5: 如何查看日志？
**A:** 
- 控制台会实时显示日志
- 日志文件位置：`logs/live-recorder.log`
- 可以使用文本编辑器或tail命令查看：
  ```bash
  tail -f logs/live-recorder.log
  ```

### Q6: 磁盘空间不足怎么办？
**A:** 
1. 清理旧的录制文件
2. 修改存储路径到更大的磁盘：
   ```properties
   recording.storage.path=/path/to/large/disk/recordings
   ```
3. 系统会在磁盘空间不足时自动拒绝新的录制请求

### Q7: 如何停止服务？
**A:** 
- 前台运行：按`Ctrl+C`
- 后台运行：
  ```bash
  # 查找进程ID
  ps aux | grep douyin-live-recorder
  # 停止进程
  kill <PID>
  ```

---

## 错误处理

### 常见错误码

| HTTP状态码 | 错误类型 | 说明 | 解决方法 |
|-----------|---------|------|---------|
| 400 | 无效的抖音号 | 抖音号格式不正确 | 检查抖音号格式（字母和数字组合） |
| 404 | 直播未开始 | 该用户当前未在直播 | 等待用户开播后重试 |
| 429 | 并发限制 | 达到最大并发录制数 | 等待其他任务完成或增加并发限制 |
| 500 | 录制错误 | 录制过程中发生错误 | 查看日志了解详细原因 |
| 503 | 网络错误 | 无法连接到抖音服务器 | 检查网络连接，稍后重试 |

### 错误响应格式

```json
{
  "timestamp": "2026-01-03T14:30:25",
  "status": 400,
  "error": "Bad Request",
  "message": "抖音号格式无效，请输入有效的抖音号（字母和数字组合）",
  "path": "/api/recordings/start"
}
```

---

## 高级功能

### 1. 自定义录制参数

虽然当前版本使用默认的FFmpeg参数，但你可以通过修改代码来自定义：

编辑`FFmpegWrapper.java`中的命令构建逻辑：
```java
// 当前默认命令
ffmpeg -i <stream_url> -c copy -bsf:a aac_adtstoasc -movflags +faststart -y <output_file>

// 可以修改为自定义参数，例如：
// - 改变视频编码：-c:v libx264
// - 改变音频编码：-c:a aac
// - 设置比特率：-b:v 2M
```

### 2. 监控和告警

系统提供健康检查端点，可以集成到监控系统：
- Prometheus
- Grafana
- Zabbix
- Nagios

### 3. 日志分析

日志文件包含详细的操作记录，可用于：
- 故障排查
- 性能分析
- 使用统计

---

## 技术支持

### 查看日志
```bash
# 实时查看日志
tail -f logs/live-recorder.log

# 搜索错误日志
grep ERROR logs/live-recorder.log

# 查看特定任务的日志
grep "taskId=550e8400" logs/live-recorder.log
```

### 性能优化建议

1. **增加并发数**：如果硬件资源充足
   ```properties
   recording.max-concurrent-tasks=10
   ```

2. **使用SSD存储**：提高写入性能

3. **增加JVM内存**：
   ```bash
   java -Xmx2G -jar target/douyin-live-recorder-1.0.0.jar
   ```

4. **网络优化**：确保稳定的网络连接

---

## 许可证

本项目仅供学习和研究使用，请遵守相关法律法规和平台服务条款。

---

## 更新日志

### v1.0.0 (2026-01-03)
- ✅ 初始版本发布
- ✅ 支持基本的录制功能
- ✅ 支持并发录制
- ✅ 完整的错误处理
- ✅ 健康检查功能

---

**祝使用愉快！如有问题，请查看日志文件或联系技术支持。**
