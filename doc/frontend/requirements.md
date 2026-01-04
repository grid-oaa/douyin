# 需求文档

## 简介

该系统是抖音直播录制任务的前端管理界面，提供任务创建、状态可视化、详情查看与运行时配置入口，面向需要管理录制任务的运营或技术人员使用。

## 术语表

- **System**: douyin-extra-frontend 前端管理系统
- **User**: 录制任务的管理人员
- **Recording**: 录制任务
- **Status**: 录制任务状态
- **Task List**: 录制任务列表
- **Detail Panel**: 任务详情弹窗/侧栏
- **Cookie**: 抖音请求 Cookie 配置

## 需求

### 需求1: 录制任务创建与总览

**用户故事：** 作为用户，我想输入抖音房间号并选择是否自动录制，以便发起录制任务并在列表中查看任务信息。

#### 验收标准

1. WHEN 用户填写抖音房间号并提交 THEN THE System SHALL 调用 `POST /api/recordings/start` 创建录制任务。
2. WHEN 创建成功返回 THEN THE System SHALL 在任务列表中展示新任务卡片。
3. WHEN 页面首次进入 THEN THE System SHALL 调用 `GET /api/recordings` 拉取并展示任务列表。
4. WHEN 用户点击刷新列表 THEN THE System SHALL 重新拉取任务列表并更新展示。

### 需求2: 任务状态轮询与刷新

**用户故事：** 作为用户，我想看到任务状态持续更新，以便了解录制进度与结果。

#### 验收标准

1. WHEN 任务状态为 `PENDING/DETECTING/RECORDING/STOPPING` THEN THE System SHALL 以 2-5 秒间隔轮询 `GET /api/recordings/{taskId}/status` 更新该任务状态。
2. WHEN 列表页处于可见状态 THEN THE System SHALL 以 10-30 秒间隔轮询 `GET /api/recordings` 更新任务列表。
3. WHEN 任务状态变为 `COMPLETED/FAILED/CANCELLED` THEN THE System SHALL 停止对该任务的状态轮询。
4. WHEN 轮询失败 THEN THE System SHALL 提示网络异常并保持上一次有效状态展示。

### 需求3: 任务详情弹窗/侧栏

**用户故事：** 作为用户，我想查看任务详情与进度，以便掌握录制状态、错误与输出信息。

#### 验收标准

1. WHEN 用户点击某个任务 THEN THE System SHALL 打开详情弹窗/侧栏并展示 `taskId`、`status`、`outputPath`、`error`。
2. WHEN 状态接口包含进度信息 THEN THE System SHALL 展示时长、文件大小与码率。
3. WHEN 用户关闭详情弹窗/侧栏 THEN THE System SHALL 恢复列表视图且不丢失已加载数据。

### 需求4: 停止录制任务

**用户故事：** 作为用户，我想停止正在录制的任务，以便及时结束录制。

#### 验收标准

1. WHEN 用户点击停止按钮 THEN THE System SHALL 调用 `POST /api/recordings/{taskId}/stop`。
2. WHEN 停止请求成功 THEN THE System SHALL 更新任务状态并在列表与详情中同步展示。
3. WHEN 任务处于不可停止状态 THEN THE System SHALL 禁用停止按钮并提示原因。

### 需求5: 运行时 Cookie 更新

**用户故事：** 作为用户，我想更新 Cookie 配置，以便修复鉴权或访问问题。

#### 验收标准

1. WHEN 用户输入 Cookie 并提交 THEN THE System SHALL 调用 `POST /api/config/cookie` 更新配置。
2. WHEN 接口返回 204 THEN THE System SHALL 提示更新成功并清空输入框。
3. WHEN 用户进入 Cookie 配置区域 THEN THE System SHALL 展示安全提示与权限边界说明。

### 需求6: 状态映射与错误提示

**用户故事：** 作为用户，我想看到一致的状态颜色与错误信息，以便快速判断任务状况。

#### 验收标准

1. WHEN 任务状态为 `PENDING/DETECTING` THEN THE System SHALL 以灰色状态样式展示。
2. WHEN 任务状态为 `RECORDING` THEN THE System SHALL 以绿色状态样式展示。
3. WHEN 任务状态为 `STOPPING` THEN THE System SHALL 以黄色状态样式展示。
4. WHEN 任务状态为 `COMPLETED` THEN THE System SHALL 以蓝色状态样式展示。
5. WHEN 任务状态为 `FAILED/CANCELLED` THEN THE System SHALL 以红色状态样式展示。
6. WHEN 接口返回错误响应 THEN THE System SHALL 优先展示 `message`，并附带 `status` 与 `path`。
7. WHEN 接口返回 429 或 503 或 400 THEN THE System SHALL 给出清晰的失败原因提示。

### 需求7: 健康检查展示（可选）

**用户故事：** 作为用户，我想查看后端健康状态，以便确认系统可用性。

#### 验收标准

1. WHEN 用户打开健康检查区域 THEN THE System SHALL 调用 `GET /actuator/health` 并展示结果摘要。
