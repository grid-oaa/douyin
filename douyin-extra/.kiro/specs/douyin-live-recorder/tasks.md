# 实现计划：抖音直播视频提取器

## 概述

本实现计划将抖音直播视频提取器的设计转换为可执行的Java代码任务。系统将使用Spring Boot框架构建REST API，使用FFmpeg进行视频录制，并支持并发录制多个直播间。

## 任务

- [x] 1. 设置项目结构和依赖
  - 创建Maven/Gradle项目
  - 添加Spring Boot依赖（Web、Validation）
  - 添加HTTP客户端依赖（Apache HttpClient或OkHttp）
  - 添加日志依赖（SLF4J + Logback）
  - 配置application.properties（存储路径、并发限制等）
  - _需求：所有需求的基础_

- [x] 1.1 编写属性测试：抖音号格式验证

  - **属性 1: 抖音号格式验证正确性**
  - **验证需求：1.1, 1.3**

- [x] 2. 实现数据模型和枚举
  - [x] 2.1 创建TaskStatus枚举
    - 定义所有任务状态（PENDING, DETECTING, RECORDING, STOPPING, COMPLETED, FAILED, CANCELLED）
    - _需求：6.4_
  
  - [x] 2.2 创建RecordingTask实体类
    - 实现所有字段（taskId, douyinId, status, streamUrl, outputPath, startTime, endTime, fileSize, error）
    - 添加构造函数和getter/setter
    - _需求：5.1, 5.2, 6.4_
  
  - [x] 2.3 创建LiveStatus数据类
    - 实现字段（isLive, roomId, title, startTime）
    - _需求：2.1_
  
  - [x] 2.4 创建StreamInfo数据类
    - 实现字段（url, format, quality, isValid）
    - _需求：3.1, 3.3_
  
  - [x] 2.5 创建RecordingStatus数据类
    - 实现字段（taskId, status, progress）
    - 创建Progress内部类（duration, fileSize, bitrate）
    - _需求：6.4_

- [x] 2.6 编写属性测试：文件命名格式

  - **属性 9: 文件命名格式符合规范**
  - **验证需求：5.2**

- [x] 3. 实现基础设施层
  - [x] 3.1 实现FileSystemManager类
    - 实现generateFilename方法（格式：{douyinId}_{YYYYMMDD}_{HHMMSS}.mp4）
    - 实现ensureDirectory方法
    - 实现getAvailableSpace方法
    - 实现deleteFile方法
    - _需求：5.1, 5.2, 5.3_
  
  - [x] 3.2 实现FFmpegWrapper类
    - 实现execute方法（启动FFmpeg进程）
    - 实现kill方法（终止进程）
    - 实现getOutput方法（获取进程输出）
    - 实现isRunning方法（检查进程状态）
    - 构建FFmpeg命令：`ffmpeg -i <stream_url> -c copy -bsf:a aac_adtstoasc -movflags +faststart -y <output_file>`
    - _需求：4.1, 4.2, 4.3_
  
  - [x] 3.3 创建HTTP客户端工具类
    - 配置连接超时（5秒）
    - 配置读取超时（5秒）
    - 实现重试机制（最多3次，间隔2秒）
    - _需求：2.4, 7.1_

- [x] 3.4 编写属性测试：文件名唯一性

  - **属性 7: 文件名唯一性**
  - **验证需求：4.5**

- [x] 4. 实现核心服务层
  - [x] 4.1 实现LiveStreamDetector服务
    - 实现checkLiveStatus方法
    - 实现getLiveStreamUrl方法
    - 处理网络错误和API错误
    - _需求：2.1, 2.2, 2.3, 3.1_
  
  - [x] 4.2 实现StreamExtractor服务
    - 实现extractStreamUrl方法
    - 实现validateStreamUrl方法
    - 支持FLV和M3U8格式
    - _需求：3.1, 3.2, 3.3_
  
  - [x] 4.3 实现RecordingService服务
    - 实现startRecording方法
    - 实现stopRecording方法
    - 实现isRecording方法
    - 监控FFmpeg进程状态
    - 处理流中断
    - _需求：4.1, 4.2, 4.3, 4.4, 3.4_

- [x] 4.4 编写属性测试：流协议支持

  - **属性 3: 流协议支持**
  - **验证需求：3.3**

- [x] 5. 实现业务逻辑层
  - [x] 5.1 实现RecordingManager服务
    - 实现createTask方法
    - 实现startTask方法（异步执行）
    - 实现stopTask方法
    - 实现getTaskStatus方法
    - 实现listActiveTasks方法
    - 维护任务映射表（ConcurrentHashMap）
    - 实现并发限制检查（默认最大5个）
    - _需求：6.1, 6.2, 6.3, 6.4, 8.1, 8.2, 8.3, 8.4_
  
  - [x] 5.2 实现任务执行逻辑
    - 创建异步任务执行器（使用ExecutorService）
    - 实现完整的录制流程：检测直播 → 提取流 → 开始录制 → 监控状态
    - 处理任务状态转换
    - 实现错误处理和资源清理
    - _需求：2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 4.1, 4.4_

- [x] 5.3 编写属性测试：并发录制支持

  - **属性 13: 并发录制支持**
  - **验证需求：8.1, 8.2**

- [x] 5.4 编写属性测试：并发限制强制执行

  - **属性 14: 并发限制强制执行**
  - **验证需求：8.3**

- [x] 6. 检查点 - 确保核心功能测试通过
  - 确保所有测试通过，如有问题请询问用户。

- [x] 7. 实现错误处理
  - [x] 7.1 创建自定义异常类
    - InvalidDouyinIdException（无效抖音号）
    - LiveStreamNotFoundException（直播未开始）
    - NetworkException（网络错误）
    - RecordingException（录制错误）
    - ConcurrentLimitException（并发限制）
    - _需求：1.2, 2.2, 7.1, 7.2, 7.3, 8.4_
  
  - [x] 7.2 实现全局异常处理器
    - 使用@ControllerAdvice
    - 为每种异常返回适当的HTTP状态码和中文错误消息
    - 记录错误日志
    - _需求：7.1, 7.2, 7.3, 7.4_

- [x] 7.3 编写属性测试：错误消息本地化

  - **属性 11: 错误消息本地化**
  - **验证需求：7.4**

- [x] 7.4 编写属性测试：无效输入返回错误

  - **属性 2: 无效输入返回错误**
  - **验证需求：1.2**

- [x] 8. 实现API接口层
  - [x] 8.1 创建RecordingController
    - 实现POST /api/recordings/start端点（开始录制）
    - 实现POST /api/recordings/{taskId}/stop端点（停止录制）
    - 实现GET /api/recordings/{taskId}/status端点（查询状态）
    - 实现GET /api/recordings端点（列出所有任务）
    - 添加输入验证注解（@Valid, @NotBlank等）
    - _需求：1.1, 6.1, 6.2, 6.4_
  
  - [x] 8.2 创建请求和响应DTO
    - StartRecordingRequest（包含douyinId）
    - RecordingResponse（包含任务信息）
    - ErrorResponse（包含错误信息）
    - _需求：1.1, 6.4, 7.4_

- [x] 8.3 编写单元测试：API端点

  - 测试所有端点的正常流程
  - 测试输入验证
  - 测试错误响应
  - _需求：1.1, 1.2, 6.1, 6.2, 6.4_

- [x] 9. 实现输入验证
  - [x] 9.1 创建DouyinIdValidator
    - 实现自定义验证注解@ValidDouyinId
    - 验证抖音号格式（字母和数字组合，长度限制）
    - _需求：1.1, 1.3_
  
  - [x] 9.2 添加验证到DTO
    - 在StartRecordingRequest中使用@ValidDouyinId
    - 添加其他必要的验证注解
    - _需求：1.1, 1.2_

- [x] 10. 实现日志和监控
  - [x] 10.1 配置结构化日志
    - 配置Logback使用JSON格式
    - 为每个操作添加日志记录
    - 记录任务ID、抖音号、错误详情等
    - _需求：7.2_
  
  - [x] 10.2 添加健康检查端点
    - 实现GET /actuator/health端点
    - 检查FFmpeg可用性
    - 检查存储空间
    - _需求：5.3_

- [x] 10.3 编写单元测试：日志记录

  - 测试错误日志包含必要信息
  - 测试日志格式正确
  - _需求：7.2_

- [x] 11. 集成和端到端测试
  - [x] 11.1 创建集成测试
    - 测试完整的录制流程（使用测试流URL）
    - 测试并发录制场景
    - 测试错误恢复
    - _需求：所有需求_
  
  - [x] 11.2 验证生成的MP4文件
    - 检查文件存在性
    - 检查文件可播放性（使用FFprobe）
    - 检查文件命名格式
    - _需求：4.4, 5.1, 5.2_

- [x] 12. 最终检查点 - 确保所有测试通过
  - 运行所有单元测试
  - 运行所有属性测试
  - 运行所有集成测试
  - 确保所有测试通过，如有问题请询问用户。

## 注意事项

- 标记为`*`的任务是可选的，可以跳过以加快MVP开发
- 每个任务都引用了具体的需求以便追溯
- 检查点确保增量验证
- 属性测试验证通用正确性属性
- 单元测试验证特定示例和边缘情况
- 使用jqwik库进行属性测试（每个测试至少100次迭代）
