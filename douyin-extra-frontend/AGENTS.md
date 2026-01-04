# AGENTS.md -- 前端对接规范（douyin-extra）

本文档用于指导 `douyin-extra-frontend` 对接 `douyin-extra` 后端服务。

## 1. 项目与系统边界
- 后端服务：`douyin-extra`（Spring Boot 3.2.1，Java 17）
- 主要能力：抖音直播录制任务的创建、状态查询、停止、列表、健康检查、运行时 Cookie 更新
- 前端职责：提供录制任务管理界面与状态可视化，处理错误提示与交互流程

## 2. 服务地址与环境
- 默认服务地址：`http://localhost:8080`
- API 基础路径：`/api/recordings`
- 健康检查：`/actuator/health`
- 运行时配置：`/api/config`
- 鉴权：当前无显式鉴权（如需跨域访问，需后端开启 CORS 或通过代理）

## 3. 核心数据模型

### 3.1 录制任务（RecordingResponse）
```json
{
  "taskId": "string",
  "douyinId": "string",
  "status": "PENDING|DETECTING|RECORDING|STOPPING|COMPLETED|FAILED|CANCELLED",
  "streamUrl": "string|null",
  "outputPath": "string|null",
  "startTime": "2026-01-03T14:30:25",
  "endTime": "2026-01-03T15:30:25|null",
  "fileSize": 123456,
  "error": "string|null"
}
```

### 3.2 录制状态（RecordingStatus）
```json
{
  "taskId": "string",
  "status": "PENDING|DETECTING|RECORDING|STOPPING|COMPLETED|FAILED|CANCELLED",
  "progress": {
    "duration": 120,
    "fileSize": 15728640,
    "bitrate": "1024kbps"
  },
  "error": "string|null"
}
```

### 3.3 错误响应（ErrorResponse）
```json
{
  "timestamp": "2026-01-03T14:30:25",
  "status": 400,
  "error": "错误类型",
  "message": "错误描述",
  "path": "/api/recordings/start"
}
```

## 4. API 设计

### 4.1 开始录制
- 方法：`POST /api/recordings/start`
- 请求体：
```json
{
  "douyinId": "抖音号（必填）",
  "auto": true
}
```
- 成功响应：HTTP 201，`RecordingResponse`
- 说明：`auto` 可选，缺省时使用后端配置 `recording.auto-enabled`

### 4.2 停止录制
- 方法：`POST /api/recordings/{taskId}/stop`
- 成功响应：HTTP 200，`RecordingResponse`
- 失败情况：可能返回 404（任务不存在）或 500（停止失败）

### 4.3 查询录制状态
- 方法：`GET /api/recordings/{taskId}/status`
- 成功响应：HTTP 200，`RecordingStatus`

### 4.4 列出所有录制任务
- 方法：`GET /api/recordings`
- 成功响应：HTTP 200，`RecordingResponse[]`
- 说明：当前实现返回“活跃任务”，已完成任务可能不在列表内

### 4.5 健康检查
- 方法：`GET /actuator/health`
- 成功响应：HTTP 200，包含 FFmpeg 与存储可用性

### 4.6 更新运行时 Cookie
- 方法：`POST /api/config/cookie`
- 请求体：
```json
{
  "cookie": "抖音 Cookie 字符串"
}
```
- 成功响应：HTTP 204
- 说明：用于运行时更新请求 Cookie，前端需注意脱敏与权限控制

## 5. 前端页面建议（对接视角）

### 5.1 录制任务总览页
- 输入抖音号与“自动录制”开关
- 发起录制后展示任务卡片：状态、时长、文件大小、输出路径
- 支持停止录制与刷新状态

### 5.2 任务详情弹窗/侧栏
- 展示 taskId、状态、错误信息、输出路径
- 展示进度（时长、大小、码率）

### 5.3 运行时配置页（可选）
- 提供 Cookie 更新入口
- 明确提示安全风险与权限边界

## 6. 状态与交互建议
- 状态映射建议：
  - `PENDING/DETECTING`：灰色/加载中
  - `RECORDING`：绿色/录制中
  - `STOPPING`：黄色/停止中
  - `COMPLETED`：蓝色/已完成
  - `FAILED/CANCELLED`：红色/失败或取消
- 轮询策略：前端可对单个任务 2-5 秒轮询状态；列表页可降低频率以减少压力
- 错误展示：优先展示 `ErrorResponse.message`，同时记录 `status` 与 `path`

## 7. 风险与注意事项
- 并发限制：超过 `recording.max-concurrent-tasks` 会返回 429
- 网络异常：可能返回 503（网络错误）
- 输入校验：抖音号格式错误返回 400
- 跨域：若前后端分离部署，需要后端开放 CORS 或通过同域代理

## 8. 前端对接验收清单
- 能成功发起录制并展示任务卡片
- 能正确展示状态与错误信息
- 能停止录制并更新状态
- 能展示健康检查结果（可选）
- 运行时 Cookie 更新功能可用（如启用）
