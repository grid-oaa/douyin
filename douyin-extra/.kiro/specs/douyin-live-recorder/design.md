# 设计文档

## 概述

抖音直播视频提取器是一个基于服务的应用程序，能够自动检测、提取和录制抖音直播流。系统接受抖音号作为输入，查询直播状态，获取直播流URL，并使用FFmpeg将直播内容录制为MP4格式的视频文件。

核心技术栈：
- FFmpeg：用于直播流的捕获和转码
- HTTP客户端：用于与抖音API交互
- 异步任务管理：支持并发录制多个直播间

## 架构

系统采用分层架构，包含以下主要层次：

```
┌─────────────────────────────────────┐
│      API/服务接口层                  │
│  (接收用户请求，返回状态)            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      业务逻辑层                      │
│  - 录制管理器                        │
│  - 任务调度器                        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      核心服务层                      │
│  - 直播检测服务                      │
│  - 流提取服务                        │
│  - 录制服务                          │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      基础设施层                      │
│  - HTTP客户端                        │
│  - FFmpeg包装器                      │
│  - 文件系统管理                      │
└─────────────────────────────────────┘
```

## 组件和接口

### 1. API接口层

**RecordingAPI**
- 职责：提供HTTP/REST接口供用户交互
- 主要方法：
  - `startRecording(douyinId: string) -> RecordingTask`
  - `stopRecording(taskId: string) -> boolean`
  - `getRecordingStatus(taskId: string) -> RecordingStatus`
  - `listRecordings() -> List<RecordingTask>`

### 2. 录制管理器 (RecordingManager)

**职责**：管理所有录制任务的生命周期

**接口**：
```
class RecordingManager:
    createTask(douyinId: string) -> RecordingTask
    startTask(taskId: string) -> boolean
    stopTask(taskId: string) -> boolean
    getTaskStatus(taskId: string) -> RecordingStatus
    listActiveTasks() -> List<RecordingTask>
    getMaxConcurrentTasks() -> int
```

**状态管理**：
- 维护活动任务映射表 (taskId -> RecordingTask)
- 限制最大并发任务数（默认：5）
- 处理任务的创建、启动、停止和清理

### 3. 直播检测服务 (LiveStreamDetector)

**职责**：检测抖音用户的直播状态

**接口**：
```
class LiveStreamDetector:
    checkLiveStatus(douyinId: string) -> LiveStatus
    getLiveStreamUrl(douyinId: string) -> string
```

**实现细节**：
- 通过HTTP请求抖音的Web接口获取直播间信息
- 解析响应数据提取直播状态和流URL
- 支持重试机制（最多3次，间隔2秒）
- 超时设置：5秒

### 4. 流提取服务 (StreamExtractor)

**职责**：从抖音直播间提取可用的流URL

**接口**：
```
class StreamExtractor:
    extractStreamUrl(douyinId: string) -> StreamInfo
    validateStreamUrl(url: string) -> boolean
```

**StreamInfo结构**：
```
{
    url: string,           // 直播流URL
    format: string,        // 流格式 (flv, m3u8等)
    quality: string,       // 画质 (origin, high, medium)
    isValid: boolean       // URL是否有效
}
```

### 5. 录制服务 (RecordingService)

**职责**：使用FFmpeg录制直播流

**接口**：
```
class RecordingService:
    startRecording(streamUrl: string, outputPath: string) -> Process
    stopRecording(process: Process) -> boolean
    isRecording(process: Process) -> boolean
```

**FFmpeg命令模板**：
```bash
ffmpeg -i <stream_url> \
       -c copy \
       -bsf:a aac_adtstoasc \
       -movflags +faststart \
       -y <output_file>
```

参数说明：
- `-i`: 输入流URL
- `-c copy`: 直接复制流，不重新编码（保持原始质量）
- `-bsf:a aac_adtstoasc`: 音频比特流过滤器
- `-movflags +faststart`: 优化MP4文件结构，支持快速播放
- `-y`: 覆盖已存在的文件

### 6. FFmpeg包装器 (FFmpegWrapper)

**职责**：封装FFmpeg命令行调用

**接口**：
```
class FFmpegWrapper:
    execute(command: List<string>) -> Process
    kill(process: Process) -> boolean
    getOutput(process: Process) -> string
    isRunning(process: Process) -> boolean
```

### 7. 文件系统管理器 (FileSystemManager)

**职责**：管理录制文件的存储

**接口**：
```
class FileSystemManager:
    generateFilename(douyinId: string, timestamp: DateTime) -> string
    ensureDirectory(path: string) -> boolean
    getAvailableSpace(path: string) -> long
    deleteFile(path: string) -> boolean
```

**文件命名格式**：
```
{douyinId}_{YYYYMMDD}_{HHMMSS}.mp4
例如: w2511839220_20260103_143025.mp4
```

## 数据模型

### RecordingTask
```
{
    taskId: string,              // 唯一任务ID (UUID)
    douyinId: string,            // 抖音号
    status: TaskStatus,          // 任务状态
    streamUrl: string,           // 直播流URL
    outputPath: string,          // 输出文件路径
    startTime: DateTime,         // 开始时间
    endTime: DateTime?,          // 结束时间（可选）
    fileSize: long,              // 文件大小（字节）
    error: string?               // 错误信息（可选）
}
```

### TaskStatus (枚举)
```
enum TaskStatus {
    PENDING,      // 等待中
    DETECTING,    // 检测直播状态中
    RECORDING,    // 录制中
    STOPPING,     // 停止中
    COMPLETED,    // 已完成
    FAILED,       // 失败
    CANCELLED     // 已取消
}
```

### LiveStatus
```
{
    isLive: boolean,             // 是否正在直播
    roomId: string,              // 直播间ID
    title: string,               // 直播标题
    startTime: DateTime?         // 开播时间（可选）
}
```

### RecordingStatus
```
{
    taskId: string,
    status: TaskStatus,
    progress: {
        duration: int,           // 已录制时长（秒）
        fileSize: long,          // 当前文件大小（字节）
        bitrate: string          // 比特率
    },
    error: string?
}
```

## 正确性属性

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的形式化陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性 1: 抖音号格式验证正确性

*对于任何*输入字符串，验证函数应该正确识别有效的抖音号格式（字母和数字组合），并拒绝无效格式。

**验证需求：1.1, 1.3**

### 属性 2: 无效输入返回错误

*对于任何*无效的抖音号输入，系统应该返回明确的错误信息，而不是崩溃或返回空值。

**验证需求：1.2**

### 属性 3: 流协议支持

*对于任何*标准的抖音直播流协议（FLV、M3U8），系统应该能够正确识别和处理。

**验证需求：3.3**

### 属性 4: 错误信息完整性

*对于任何*获取流URL失败的情况，系统应该返回包含失败原因的详细错误信息。

**验证需求：3.2**

### 属性 5: MP4文件生成

*对于任何*成功完成的录制任务，系统应该生成一个有效的、可播放的MP4文件。

**验证需求：4.1, 4.4**

### 属性 6: 媒体质量保持不变

*对于任何*录制的视频，输出文件应该保持输入流的原始分辨率、帧率和音频质量。

**验证需求：4.2, 4.3**

### 属性 7: 文件名唯一性

*对于任何*两个不同的录制任务，生成的文件名应该是唯一的，即使它们录制相同的抖音号。

**验证需求：4.5**

### 属性 8: 文件存储位置正确性

*对于任何*完成的录制任务，生成的MP4文件应该存在于指定的存储目录中。

**验证需求：5.1**

### 属性 9: 文件命名格式符合规范

*对于任何*生成的录制文件，文件名应该符合格式 `{douyinId}_{YYYYMMDD}_{HHMMSS}.mp4`，并且包含正确的抖音号和时间戳。

**验证需求：5.2**

### 属性 10: 录制状态可查询

*对于任何*正在进行的录制任务，查询其状态应该返回有效的状态信息（包括任务ID、状态、进度等）。

**验证需求：6.4**

### 属性 11: 错误消息本地化

*对于任何*错误情况，系统返回的错误消息应该是清晰的中文文本，而不是英文或错误代码。

**验证需求：7.4**

### 属性 12: API错误处理

*对于任何*抖音API返回的错误响应，系统应该记录错误详情并向用户返回通知。

**验证需求：7.2**

### 属性 13: 并发录制支持

*对于任何*小于最大并发限制的录制任务集合，系统应该能够同时处理所有任务而不互相干扰。

**验证需求：8.1, 8.2**

### 属性 14: 并发限制强制执行

*对于任何*超过最大并发限制的录制请求，系统应该拒绝该请求并返回明确的限制信息。

**验证需求：8.3**



## 错误处理

### 错误类型和处理策略

#### 1. 输入验证错误
- **场景**：无效的抖音号格式
- **处理**：立即返回400错误，包含详细的验证失败原因
- **用户消息**："抖音号格式无效，请输入有效的抖音号（字母和数字组合）"

#### 2. 网络错误
- **场景**：无法连接到抖音服务器、请求超时
- **处理**：
  - 自动重试（最多3次，间隔2秒）
  - 重试失败后返回503错误
- **用户消息**："网络连接失败，请检查网络连接后重试"

#### 3. API错误
- **场景**：抖音API返回错误响应（404、403等）
- **处理**：
  - 记录完整的错误响应到日志
  - 根据错误码返回相应的用户友好消息
- **用户消息示例**：
  - 404: "未找到该抖音用户或直播间"
  - 403: "访问被拒绝，可能是频率限制"

#### 4. 直播状态错误
- **场景**：用户未开播
- **处理**：返回200状态码，但标记为"未开播"
- **用户消息**："该用户当前未在直播"

#### 5. 录制错误
- **场景**：FFmpeg进程崩溃、流中断、磁盘空间不足
- **处理**：
  - 捕获FFmpeg错误输出
  - 保存已录制的部分（如果可能）
  - 更新任务状态为FAILED
  - 记录详细错误到日志
- **用户消息示例**：
  - "录制过程中直播流中断"
  - "磁盘空间不足，录制已停止"
  - "录制失败：{具体错误原因}"

#### 6. 并发限制错误
- **场景**：达到最大并发录制数量
- **处理**：返回429错误（Too Many Requests）
- **用户消息**："当前录制任务已达上限（{max}个），请稍后重试"

#### 7. 文件系统错误
- **场景**：无法创建目录、无写入权限
- **处理**：
  - 在启动时验证目录权限
  - 运行时错误立即停止录制
- **用户消息**："文件系统错误：无法写入录制文件"

### 错误日志记录

所有错误都应该记录以下信息：
- 时间戳
- 错误类型
- 错误详情
- 相关的任务ID和抖音号
- 堆栈跟踪（如果适用）

日志格式示例：
```
[2026-01-03 14:30:25] ERROR [TaskID: abc-123] [DouyinID: w2511839220]
录制失败: FFmpeg进程异常退出
FFmpeg输出: [详细错误信息]
```

## 测试策略

### 单元测试

单元测试用于验证特定示例、边缘情况和错误条件：

#### 1. 输入验证测试
- 测试有效的抖音号格式
- 测试无效的抖音号格式（空字符串、特殊字符、过长等）
- 测试边缘情况（单字符、纯数字、纯字母）

#### 2. 文件命名测试
- 测试文件名生成的正确性
- 测试时间戳格式
- 测试特殊字符处理

#### 3. 状态转换测试
- 测试任务状态的有效转换
- 测试无效的状态转换被拒绝

#### 4. 错误处理测试
- 测试各种错误场景的处理
- 测试错误消息的格式和内容
- 测试资源清理（文件句柄、进程等）

### 属性测试

属性测试用于验证跨所有输入的通用属性，使用属性测试框架（如Hypothesis for Python、fast-check for TypeScript）：

#### 配置
- 每个属性测试最少运行100次迭代
- 使用随机生成的测试数据
- 每个测试必须引用设计文档中的属性

#### 测试标签格式
```
Feature: douyin-live-recorder, Property {number}: {property_text}
```

#### 属性测试列表

1. **属性 1: 抖音号格式验证正确性**
   - 生成随机字符串（有效和无效格式）
   - 验证验证函数的返回值正确性

2. **属性 2: 无效输入返回错误**
   - 生成各种无效输入
   - 验证总是返回错误而不是崩溃

3. **属性 7: 文件名唯一性**
   - 生成多个录制任务（相同或不同的抖音号）
   - 验证所有生成的文件名都是唯一的

4. **属性 9: 文件命名格式符合规范**
   - 生成随机的抖音号和时间戳
   - 验证生成的文件名符合指定格式
   - 验证可以从文件名中正确解析出抖音号和时间戳

5. **属性 11: 错误消息本地化**
   - 触发各种错误场景
   - 验证所有错误消息都是中文
   - 验证错误消息不包含英文错误代码

6. **属性 13: 并发录制支持**
   - 创建多个并发录制任务（数量小于限制）
   - 验证所有任务都能成功启动
   - 验证任务之间不互相干扰

7. **属性 14: 并发限制强制执行**
   - 尝试创建超过限制的任务
   - 验证超出限制的任务被拒绝
   - 验证返回正确的错误信息

### 集成测试

集成测试验证组件之间的交互：

1. **端到端录制流程**
   - 模拟完整的录制流程（从输入抖音号到生成MP4文件）
   - 使用测试直播流URL
   - 验证生成的文件可以播放

2. **FFmpeg集成**
   - 测试FFmpeg命令的正确构建
   - 测试进程管理（启动、停止、监控）
   - 测试输出文件的有效性

3. **并发场景**
   - 测试多个任务同时运行
   - 测试任务的启动和停止
   - 测试资源清理

### 测试工具和框架

根据实现语言选择合适的测试框架：

- **Python**: pytest + Hypothesis (属性测试)
- **TypeScript/JavaScript**: Jest + fast-check (属性测试)
- **Java**: JUnit + jqwik (属性测试)
- **Go**: testing + gopter (属性测试)

### 测试数据

#### 模拟数据
- 有效的抖音号示例：`w2511839220`, `user123`, `test456abc`
- 无效的抖音号示例：``, `@user`, `user name`, `用户123`
- 测试直播流URL（使用公开的测试流或本地模拟服务器）

#### 测试环境
- 独立的测试目录用于文件输出
- 模拟的HTTP服务器用于API测试
- 测试后自动清理生成的文件

## 实现注意事项

### 性能考虑

1. **异步处理**：录制任务应该在后台异步执行，不阻塞API响应
2. **资源限制**：限制最大并发任务数，防止系统资源耗尽
3. **流式处理**：使用流式写入，避免将整个视频加载到内存

### 安全考虑

1. **输入验证**：严格验证所有用户输入，防止注入攻击
2. **路径遍历防护**：确保文件只能写入指定目录
3. **资源限制**：限制单个文件的最大大小和录制时长
4. **进程隔离**：确保FFmpeg进程不能访问系统敏感资源

### 可扩展性

1. **插件化设计**：支持未来添加其他平台（B站、快手等）
2. **配置化**：将关键参数（并发限制、超时时间等）配置化
3. **存储抽象**：支持本地存储和云存储（S3、OSS等）

### 监控和日志

1. **结构化日志**：使用JSON格式记录日志，便于分析
2. **指标收集**：记录录制成功率、平均录制时长等指标
3. **健康检查**：提供健康检查端点，监控系统状态
