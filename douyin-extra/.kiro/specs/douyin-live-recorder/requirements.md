# 需求文档

## 简介

抖音直播视频提取器是一个服务，允许用户通过输入抖音号来自动录制和保存正在进行的直播为MP4视频文件。

## 术语表

- **System**: 抖音直播视频提取器系统
- **User**: 使用该服务的用户
- **Douyin_ID**: 抖音用户的唯一标识符（例如：w2511839220）
- **Live_Stream**: 正在进行的抖音直播
- **Recording**: 从直播流中提取并保存的视频文件
- **MP4**: 视频文件格式

## 需求

### 需求 1: 接受抖音号输入

**用户故事：** 作为用户，我想要输入抖音号，以便系统能够定位到对应的直播间。

#### 验收标准

1. WHEN 用户提供抖音号 THEN THE System SHALL 验证抖音号格式的有效性
2. WHEN 抖音号格式无效 THEN THE System SHALL 返回明确的错误信息
3. THE System SHALL 接受字母和数字组合的抖音号

### 需求 2: 检测直播状态

**用户故事：** 作为用户，我想要系统自动检测直播间是否正在直播，以便只在有直播时进行录制。

#### 验收标准

1. WHEN 抖音号被提供 THEN THE System SHALL 查询该用户的直播状态
2. WHEN 直播间未开播 THEN THE System SHALL 通知用户直播未开始
3. WHEN 直播间正在直播 THEN THE System SHALL 获取直播流地址
4. THE System SHALL 在5秒内完成直播状态检测

### 需求 3: 提取直播流

**用户故事：** 作为用户，我想要系统能够提取直播流数据，以便将其保存为视频文件。

#### 验收标准

1. WHEN 直播正在进行 THEN THE System SHALL 获取直播流的URL
2. WHEN 获取流URL失败 THEN THE System SHALL 返回详细的错误信息
3. THE System SHALL 支持抖音平台的标准直播流协议
4. WHEN 直播流中断 THEN THE System SHALL 检测到中断并停止录制

### 需求 4: 录制为MP4视频

**用户故事：** 作为用户，我想要系统将直播流录制并保存为MP4格式，以便我可以在任何设备上播放。

#### 验收标准

1. WHEN 开始录制 THEN THE System SHALL 将直播流数据写入MP4文件
2. THE System SHALL 保持视频的原始分辨率和帧率
3. THE System SHALL 保持音频的原始质量
4. WHEN 录制完成 THEN THE System SHALL 生成有效的可播放MP4文件
5. THE System SHALL 为每个录制文件生成唯一的文件名

### 需求 5: 文件存储管理

**用户故事：** 作为用户，我想要录制的视频被妥善保存，以便我可以稍后访问它们。

#### 验收标准

1. THE System SHALL 将录制的视频保存到指定的存储目录
2. THE System SHALL 使用包含抖音号和时间戳的文件命名格式
3. WHEN 存储空间不足 THEN THE System SHALL 返回错误并停止录制
4. THE System SHALL 确保文件写入完成后才关闭文件句柄

### 需求 6: 录制控制

**用户故事：** 作为用户，我想要能够控制录制过程，以便我可以按需开始或停止录制。

#### 验收标准

1. THE System SHALL 提供开始录制的接口
2. THE System SHALL 提供停止录制的接口
3. WHEN 用户请求停止录制 THEN THE System SHALL 在3秒内完成文件保存
4. THE System SHALL 在录制过程中提供状态信息

### 需求 7: 错误处理

**用户故事：** 作为用户，我想要系统能够优雅地处理错误情况，以便我了解发生了什么问题。

#### 验收标准

1. WHEN 网络连接失败 THEN THE System SHALL 返回网络错误信息
2. WHEN 抖音API返回错误 THEN THE System SHALL 记录错误详情并通知用户
3. WHEN 录制过程中发生异常 THEN THE System SHALL 保存已录制的部分并通知用户
4. THE System SHALL 为所有错误情况提供清晰的中文错误消息

### 需求 8: 并发录制支持

**用户故事：** 作为用户，我想要能够同时录制多个直播间，以便提高效率。

#### 验收标准

1. THE System SHALL 支持同时录制多个不同的直播间
2. WHEN 多个录制任务运行时 THEN THE System SHALL 为每个任务分配独立的资源
3. THE System SHALL 限制最大并发录制数量以防止资源耗尽
4. WHEN 达到并发限制 THEN THE System SHALL 拒绝新的录制请求并返回明确信息
