# 快速开始指南

## 5分钟快速上手

### 第一步：安装FFmpeg（必需）

#### Windows
1. 下载：https://www.gyan.dev/ffmpeg/builds/
2. 解压到 `C:\ffmpeg`
3. 添加 `C:\ffmpeg\bin` 到系统PATH环境变量
4. 验证：打开新的命令行，输入 `ffmpeg -version`

#### Linux
```bash
sudo apt install ffmpeg  # Ubuntu/Debian
# 或
sudo yum install ffmpeg  # CentOS/RHEL
```

#### macOS
```bash
brew install ffmpeg
```

---

### 第二步：启动项目

```bash
# Windows
start.bat

# Linux/macOS
./mvnw spring-boot:run
```

**Windows用户注意**: 如果看到中文乱码，这是正常的终端编码问题，不影响功能。已更新为英文版脚本避免此问题。详见 [ENCODING_FIX.md](ENCODING_FIX.md)

看到 `Started LiveRecorderApplication` 表示启动成功！

---

### 第三步：开始录制

打开新的命令行窗口，执行：

```bash
# 开始录制（替换为真实的抖音号）
curl -X POST http://localhost:8080/api/recordings/start ^
  -H "Content-Type: application/json" ^
  -d "{\"douyinId\": \"w2511839220\"}"
```

**注意：** 
- Windows CMD使用 `^` 作为换行符
- PowerShell使用 `` ` `` 作为换行符
- Linux/macOS使用 `\` 作为换行符

响应示例：
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "douyinId": "w2511839220",
  "status": "DETECTING",
  ...
}
```

**保存返回的 taskId，后续需要用它来查询和停止录制！**

---

### 第四步：查询录制状态

```bash
# 替换为你的taskId
curl http://localhost:8080/api/recordings/550e8400-e29b-41d4-a716-446655440000/status
```

---

### 第五步：停止录制

```bash
# 替换为你的taskId
curl -X POST http://localhost:8080/api/recordings/550e8400-e29b-41d4-a716-446655440000/stop
```

---

### 第六步：查找录制的视频

录制完成的视频文件位于：
```
./recordings/w2511839220_20260103_143025.mp4
```

文件命名格式：`{抖音号}_{日期}_{时间}.mp4`

---

## 使用Postman测试（推荐）

### 1. 开始录制
- **方法**: POST
- **URL**: `http://localhost:8080/api/recordings/start`
- **Headers**: 
  - Key: `Content-Type`
  - Value: `application/json`
- **Body** (选择raw, JSON):
```json
{
  "douyinId": "w2511839220"
}
```

### 2. 查询状态
- **方法**: GET
- **URL**: `http://localhost:8080/api/recordings/{taskId}/status`

### 3. 停止录制
- **方法**: POST
- **URL**: `http://localhost:8080/api/recordings/{taskId}/stop`

---

## 常见问题速查

### ❌ 提示"FFmpeg不可用"
```bash
# 验证FFmpeg是否安装
ffmpeg -version

# 如果没有，请返回第一步安装FFmpeg
```

### ❌ 提示"直播未开始"
- 确认该抖音号正在直播
- 检查网络连接

### ❌ 提示"并发限制"
- 当前已有5个录制任务在运行
- 停止部分任务或等待完成

### ❌ 找不到录制的视频
- 检查 `./recordings` 目录
- 确认录制状态为 `COMPLETED`

---

## 下一步

查看完整的 [USER_MANUAL.md](USER_MANUAL.md) 了解：
- 详细的API文档
- 配置说明
- 高级功能
- 故障排查

---

**提示：** 首次使用建议先用一个正在直播的抖音号测试，确保系统正常工作。
