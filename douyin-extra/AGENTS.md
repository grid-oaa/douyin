# AGENTS.md — 后端项目结构与业务流程说明（douyin-extra）

本文档用于分析后端项目的结构、业务流程与功能项，便于快速理解与协作开发。

## 1. 项目概览
- 项目名称：douyin-extra（抖音直播录制服务）
- 技术栈：Spring Boot 3.2.1、Java 17、Maven、FFmpeg、OkHttp
- 对外形式：REST API 服务
- 目标能力：检测直播状态、提取直播流、录制为 MP4、任务状态查询与停止

## 2. 目录结构（核心分层）
```
src/main/java/com/douyin/liverecorder/
  controller/        # API 控制层
  service/           # 业务逻辑层
  infrastructure/    # 基础设施层（HTTP/FFmpeg/文件）
  model/             # 领域模型与状态
  dto/               # 请求/响应 DTO
  exception/         # 业务异常与全局异常处理
  validation/        # 自定义校验
  config/            # 配置类
src/main/resources/
  application.properties
  logback-spring.xml
```

## 3. 核心模块与职责
- controller
  - `RecordingController`：录制任务 API（start/stop/status/list）
  - `ConfigController`：运行时配置（cookie）
- service
  - `RecordingManager`：任务生命周期管理（创建、等待、录制、停止、完成）
  - `LiveStreamDetector`：检测是否开播
  - `StreamExtractor`：提取直播流 URL
  - `RecordingService`：启动/停止 FFmpeg 录制、封装 MP4
- infrastructure
  - `HttpClientUtil`：HTTP 客户端封装（调用外部）
  - `FFmpegWrapper`：FFmpeg 调用封装
  - `FileSystemManager` / `FileNameGenerator`：文件路径与命名
- model
  - `RecordingTask` / `TaskStatus`：任务与状态
  - `RecordingStatus` / `StreamInfo` / `LiveStatus`：进度与流信息
- exception
  - `GlobalExceptionHandler`：统一异常响应
  - 业务异常：并发限制、输入校验、网络错误等
- validation
  - `ValidDouyinId` / `DouyinIdValidator`：抖音号校验

## 4. 关键业务流程

### 4.1 创建任务（POST /api/recordings/start）
1) 接收 `douyinId` 与 `auto`  
2) `RecordingManager.createTask` 创建任务并进入 `PENDING`  
3) 若 `auto=true`，任务进入等待开播流程（WAITING）

### 4.2 自动等待开播（auto=true）
- 轮询直播状态（`recording.poll-interval-ms`，默认 120000ms）
- 超过 `recording.max-wait-ms`（默认 3600000ms）失败
- 发现开播后继续提取流并开始录制

### 4.3 手动检测开播（auto=false）
- 直接检测一次直播状态  
  - 未开播：任务失败  
  - 已开播：继续提取流并录制

### 4.4 录制流程
1) 提取直播流 URL  
2) 生成输出路径与临时路径  
3) 启动 FFmpeg 录制  
4) 监控进程完成后进行封装/收尾

### 4.5 自动停止（直播结束）
- 录制中定时检查是否结束  
- 连续结束超过 `recording.end-detect-grace-ms`（默认 15000ms）自动停止

### 4.6 停止任务（POST /api/recordings/{taskId}/stop）
- 标记 STOPPING 并尝试停止 FFmpeg  
- 成功后整理输出文件与状态

## 5. 对外 API 概览
- `POST /api/recordings/start` 创建任务
- `POST /api/recordings/{taskId}/stop` 停止任务
- `GET /api/recordings/{taskId}/status` 获取任务状态
- `GET /api/recordings` 列出活跃任务
- `GET /actuator/health` 健康检查
- `POST /api/config/cookie` 运行时更新 Cookie

## 6. 关键配置项（application.properties）
- `server.port=8080`
- `recording.storage.path=./recordings`
- `recording.max-concurrent-tasks=5`
- `recording.poll-interval-ms=120000`
- `recording.max-wait-ms=3600000`
- `recording.end-detect-grace-ms=15000`
- `ffmpeg.path=ffmpeg`
- `recording.task-log-path=./task.txt`

## 7. 高风险点与注意事项
- Cookie 使用：`POST /api/config/cookie` 仅更新运行时配置，需确认安全存储策略
- 并发限制：超出 `recording.max-concurrent-tasks` 会返回错误
- FFmpeg 与存储空间：依赖本机 FFmpeg 与磁盘可用空间
- 轮询频率：过低可能导致状态更新延迟，过高可能造成压力

## 8. 可扩展方向
- 增加实时预览（HLS/FLV/WebSocket 转发）
- 增强任务历史记录与持久化
- 细化权限控制与审计日志
- 任务状态事件推送（SSE/WebSocket）
