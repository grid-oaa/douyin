package com.douyin.liverecorder.service;

import com.douyin.liverecorder.infrastructure.FileSystemManager;
import com.douyin.liverecorder.model.LiveStatus;
import com.douyin.liverecorder.model.RecordingStatus;
import com.douyin.liverecorder.model.RecordingTask;
import com.douyin.liverecorder.model.StreamInfo;
import com.douyin.liverecorder.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录制管理器
 * 管理所有录制任务的生命周期
 */
@Service
public class RecordingManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingManager.class);
    
    private final LiveStreamDetector liveStreamDetector;
    private final StreamExtractor streamExtractor;
    private final RecordingService recordingService;
    private final FileSystemManager fileSystemManager;
    
    // 任务映射表（taskId -> RecordingTask）
    private final ConcurrentHashMap<String, RecordingTask> taskMap = new ConcurrentHashMap<>();
    
    // 任务进程映射表（taskId -> Process）
    private final ConcurrentHashMap<String, Process> taskProcessMap = new ConcurrentHashMap<>();
    
    // 异步任务执行器
    private final ExecutorService executorService;
    
    // 最大并发任务数
    @Value("${recording.max-concurrent-tasks:5}")
    private int maxConcurrentTasks;
    @Value("${recording.poll-interval-ms:120000}")
    private long pollIntervalMs;
    @Value("${recording.max-wait-ms:3600000}")
    private long maxWaitMs;
    @Value("${recording.end-detect-grace-ms:15000}")
    private long endDetectGraceMs;
    @Value("${recording.task-log-path:./task.txt}")
    private String taskLogPath;
    
    public RecordingManager(
            LiveStreamDetector liveStreamDetector,
            StreamExtractor streamExtractor,
            RecordingService recordingService,
            FileSystemManager fileSystemManager) {
        this.liveStreamDetector = liveStreamDetector;
        this.streamExtractor = streamExtractor;
        this.recordingService = recordingService;
        this.fileSystemManager = fileSystemManager;
        
        // 创建固定大小的线程池
        this.executorService = Executors.newCachedThreadPool();
        
        logger.info("录制管理器已初始化，最大并发任务数: {}", maxConcurrentTasks);
    }
    
    /**
     * 创建录制任务
     * 
     * @param douyinId 抖音号
     * @return 创建的录制任务
     * @throws IllegalArgumentException 如果抖音号无效
     * @throws IllegalStateException 如果达到并发限制
     */
    public RecordingTask createTask(String douyinId, boolean autoEnabled, String outputDir) {
        if (douyinId == null || douyinId.trim().isEmpty()) {
            throw new IllegalArgumentException("抖音号不能为空");
        }
        
        // 检查并发限制
        int activeTaskCount = getActiveTaskCount();
        if (activeTaskCount >= maxConcurrentTasks) {
            throw new IllegalStateException(
                String.format("当前录制任务已达上限（%d个），请稍后重试", maxConcurrentTasks));
        }
        
        // 创建新任务
        RecordingTask task = new RecordingTask(douyinId);
        task.setAutoEnabled(autoEnabled);
        task.setOutputDir(outputDir);
        task.setStatus(TaskStatus.PENDING);
        
        // 存储任务
        taskMap.put(task.getTaskId(), task);
        
        logger.info("创建录制任务: taskId={}, douyinId={}, outputDir={}", task.getTaskId(), douyinId, outputDir);
        appendTaskLog(task, "CREATED");
        
        return task;
    }
    
    /**
     * 启动录制任务（异步执行）
     * 
     * @param taskId 任务ID
     * @return 如果任务成功提交返回true，否则返回false
     * @throws IllegalArgumentException 如果任务ID无效
     * @throws IllegalStateException 如果任务状态不允许启动
     */
    public boolean startTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        
        RecordingTask task = taskMap.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 检查任务状态
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException(
                String.format("任务状态不允许启动: %s (当前状态: %s)", 
                    taskId, task.getStatus()));
        }
        
        // 检查并发限制
        int activeTaskCount = getActiveTaskCount();
        if (activeTaskCount >= maxConcurrentTasks) {
            throw new IllegalStateException(
                String.format("当前录制任务已达上限（%d个），请稍后重试", maxConcurrentTasks));
        }
        
        logger.info("启动录制任务: taskId={}, douyinId={}", taskId, task.getDouyinId());
        
        // 异步执行录制任务
        executorService.submit(() -> executeTask(task));
        
        return true;
    }
    
    /**
     * 停止录制任务
     * 
     * @param taskId 任务ID
     * @return 如果任务成功停止返回true，否则返回false
     * @throws IllegalArgumentException 如果任务ID无效
     */
    public boolean stopTask(String taskId) {
        return stopTaskInternal(taskId, true);
    }

    private boolean stopTaskInternal(String taskId, boolean userRequested) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        RecordingTask task = taskMap.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        logger.info("停止录制任务: taskId={}, douyinId={}, 当前状态={}, userRequested={}",
                taskId, task.getDouyinId(), task.getStatus(), userRequested);

        task.setStopRequestedByUser(userRequested);
        task.setStatus(TaskStatus.STOPPING);

        Process process = taskProcessMap.get(taskId);
        if (process != null) {
            boolean stopped = recordingService.stopRecording(process);

            if (stopped) {
                task.setEndTime(LocalDateTime.now());
                updateFileSize(task);
                taskProcessMap.remove(taskId);
                logger.info("录制任务已停止: taskId={}", taskId);
                return true;
            }

            logger.error("停止录制任务失败: taskId={}", taskId);
            return false;
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        logger.info("录制任务已取消（未开始录制）: taskId={}", taskId);
        return true;
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 录制状态信息
     * @throws IllegalArgumentException 如果任务ID无效
     */
    public RecordingStatus getTaskStatus(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        
        RecordingTask task = taskMap.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 创建录制状态对象
        RecordingStatus status = new RecordingStatus();
        status.setTaskId(taskId);
        status.setStatus(task.getStatus());
        
        // 如果任务正在录制，计算进度信息
        if (task.getStatus() == TaskStatus.RECORDING) {
            RecordingStatus.Progress progress = calculateProgress(task);
            status.setProgress(progress);
        }
        
        // 如果有错误信息，设置错误
        if (task.getError() != null) {
            status.setError(task.getError());
        }
        
        return status;
    }
    
    /**
     * 列出所有活动任务
     * 
     * @return 活动任务列表
     */
    public List<RecordingTask> listActiveTasks() {
        List<RecordingTask> activeTasks = new ArrayList<>();
        
        for (RecordingTask task : taskMap.values()) {
            // 只返回活动状态的任务
            if (task.getStatus() == TaskStatus.PENDING ||
                task.getStatus() == TaskStatus.WAITING ||
                task.getStatus() == TaskStatus.DETECTING ||
                task.getStatus() == TaskStatus.RECORDING ||
                task.getStatus() == TaskStatus.STOPPING) {
                activeTasks.add(task);
            }
        }
        
        logger.debug("活动任务数量: {}", activeTasks.size());
        return activeTasks;
    }

    public RecordingTask getTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return null;
        }
        return taskMap.get(taskId);
    }
    
    /**
     * 获取最大并发任务数
     * 
     * @return 最大并发任务数
     */
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
    
    /**
     * 获取活动任务数量
     * 
     * @return 活动任务数量
     */
    private int getActiveTaskCount() {
        return (int) taskMap.values().stream()
            .filter(task -> task.getStatus() == TaskStatus.DETECTING ||
                           task.getStatus() == TaskStatus.RECORDING)
            .count();
    }
    
    /**
     * 执行录制任务
     * 完整流程：检测直播 → 提取流 → 开始录制 → 监控状态
     * 
     * @param task 录制任务
     */
    private void executeTask(RecordingTask task) {
        String taskId = task.getTaskId();
        String douyinId = task.getDouyinId();
        
        // 设置MDC上下文，用于结构化日志
        MDC.put("taskId", taskId);
        MDC.put("douyinId", douyinId);
        
        try {
            logger.info("开始执行录制任务: taskId={}, douyinId={}", taskId, douyinId);
            
            // 步骤1: 检测直播状态
            LiveStatus liveStatus;
            if (task.isAutoEnabled()) {
                liveStatus = waitForLive(task);
                if (liveStatus == null) {
                    return;
                }
            } else {
                task.setStatus(TaskStatus.DETECTING);
                logger.info("检测直播状态: taskId={}, douyinId={}", taskId, douyinId);
                liveStatus = liveStreamDetector.checkLiveStatus(douyinId);

                if (!liveStatus.isLive()) {
                    task.setStatus(TaskStatus.FAILED);
                    task.setError("该用户当前未在直播");
                    logger.warn("直播未开始: taskId={}, douyinId={}", taskId, douyinId);
                    return;
                }
            }
            
            logger.info("直播已开始: taskId={}, douyinId={}, 直播间ID={}, 标题={}", 
                       taskId, douyinId, liveStatus.getRoomId(), liveStatus.getTitle());
            
            // 步骤2: 提取流URL
            logger.info("提取流URL: taskId={}, douyinId={}", taskId, douyinId);
            
            StreamInfo streamInfo = streamExtractor.extractStreamUrl(douyinId);
            
            if (!streamInfo.isValid() || streamInfo.getUrl() == null) {
                // 流URL无效
                task.setStatus(TaskStatus.FAILED);
                task.setError("未能获取有效的流URL");
                logger.error("流URL无效: taskId={}, douyinId={}", taskId, douyinId);
                return;
            }
            
            task.setStreamUrl(streamInfo.getUrl());
            logger.info("成功提取流URL: taskId={}, format={}, quality={}", 
                       taskId, streamInfo.getFormat(), streamInfo.getQuality());
            
            // 步骤3: 生成输出文件路径
            String filename = fileSystemManager.generateFilename(douyinId, LocalDateTime.now());
            String outputDir = task.getOutputDir();
            String outputPath = fileSystemManager.getFullPath(outputDir, filename);
            String tempOutputPath = replaceExtension(outputPath, ".flv");
            task.setOutputPath(outputPath);
            task.setTempOutputPath(tempOutputPath);
            
            // 确保存储目录存在
            if (!fileSystemManager.ensureDirectory(outputDir)) {
                task.setStatus(TaskStatus.FAILED);
                task.setError("无法创建存储目录");
                logger.error("无法创建存储目录: taskId={}", taskId);
                return;
            }
            
            // 检查磁盘空间
            long availableSpace = fileSystemManager.getAvailableSpace(outputPath);
            if (availableSpace < 100 * 1024 * 1024) { // 至少需要100MB
                task.setStatus(TaskStatus.FAILED);
                task.setError("磁盘空间不足，录制已停止");
                logger.error("磁盘空间不足: taskId={}, 可用空间={}MB", 
                           taskId, availableSpace / 1024 / 1024);
                return;
            }
            
            // 步骤4: 开始录制
            int activeTaskCount = getActiveTaskCount();
            if (activeTaskCount >= maxConcurrentTasks) {
                task.setStatus(TaskStatus.FAILED);
                task.setError(String.format("当前录制任务已达上限（%d个）", maxConcurrentTasks));
                logger.error("并发限制: taskId={}, 当前活动任务数={}", taskId, activeTaskCount);
                return;
            }

            task.setStatus(TaskStatus.RECORDING);
            task.setStartTime(LocalDateTime.now());
            logger.info("开始录制: taskId={}, outputPath={}", taskId, outputPath);
            appendTaskLog(task, "RECORDING_STARTED");
            
            Process process = recordingService.startRecording(streamInfo.getUrl(), tempOutputPath);
            taskProcessMap.put(taskId, process);

            startLiveEndMonitor(task, process);
            
            // 步骤5: 监控录制状态
            monitorRecording(task, process);
            
        } catch (IOException e) {
            // 网络或IO错误
            task.setStatus(TaskStatus.FAILED);
            task.setError("录制失败: " + e.getMessage());
            task.setEndTime(LocalDateTime.now());
            logger.error("录制任务失败: taskId={}, douyinId={}, error={}", taskId, douyinId, e.getMessage(), e);
            
        } catch (Exception e) {
            // 其他错误
            task.setStatus(TaskStatus.FAILED);
            task.setError("录制失败: " + e.getMessage());
            task.setEndTime(LocalDateTime.now());
            logger.error("录制任务发生未知错误: taskId={}, douyinId={}, error={}", taskId, douyinId, e.getMessage(), e);
            
        } finally {
            // 清理资源
            taskProcessMap.remove(taskId);
            // 清理MDC上下文
            MDC.clear();
        }
    }
    
    /**
     * 监控录制状态
     * 
     * @param task 录制任务
     * @param process 录制进程
     */
    private void monitorRecording(RecordingTask task, Process process) {
        String taskId = task.getTaskId();
        
        try {
            // 等待进程结束
            int exitCode = process.waitFor();
            
            task.setEndTime(LocalDateTime.now());

            boolean remuxed = finalizeRecording(task);
            updateFileSize(task);

            if (task.getStatus() == TaskStatus.STOPPING || task.getStatus() == TaskStatus.CANCELLED) {
                if (task.isStopRequestedByUser()) {
                    task.setStatus(TaskStatus.CANCELLED);
                } else if (remuxed) {
                    task.setStatus(TaskStatus.COMPLETED);
                } else {
                    task.setStatus(TaskStatus.FAILED);
                    task.setError("封装MP4失败");
                }
                appendTaskLog(task, "FINALIZED");
                logger.info("录制任务已停止: taskId={}, exitCode={}, status={}", taskId, exitCode, task.getStatus());
                return;
            }

            if (exitCode == 0) {
                // 录制成功完成
                task.setStatus(TaskStatus.COMPLETED);
                appendTaskLog(task, "COMPLETED");
                logger.info("录制任务完成: taskId={}, fileSize={}MB", 
                           taskId, task.getFileSize() / 1024 / 1024);
            } else {
                // 录制失败
                task.setStatus(TaskStatus.FAILED);
                
                // 检查是否为流中断
                if (recordingService.isStreamInterrupted(process)) {
                    task.setError("录制过程中直播流中断");
                    logger.warn("录制过程中直播流中断: taskId={}", taskId);
                } else {
                    task.setError("录制失败，退出码: " + exitCode);
                    logger.error("录制失败: taskId={}, exitCode={}", taskId, exitCode);
                }
                appendTaskLog(task, "FAILED");
                
                // 记录FFmpeg输出
                String output = recordingService.getRecordingOutput(process);
                if (output != null && !output.isEmpty()) {
                    logger.debug("FFmpeg输出: {}", output);
                }
            }
            
        } catch (InterruptedException e) {
            // 被中断
            task.setStatus(TaskStatus.CANCELLED);
            task.setEndTime(LocalDateTime.now());
            task.setError("录制被中断");
            logger.warn("录制任务被中断: taskId={}", taskId);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 更新任务的文件大小
     * 
     * @param task 录制任务
     */
    private void updateFileSize(RecordingTask task) {
        String outputPath = task.getOutputPath();
        if (outputPath != null) {
            File file = new File(outputPath);
            if (file.exists()) {
                task.setFileSize(file.length());
                return;
            }
        }
        String tempPath = task.getTempOutputPath();
        if (tempPath != null) {
            File tempFile = new File(tempPath);
            if (tempFile.exists()) {
                task.setFileSize(tempFile.length());
            }
        }
    }
    
    /**
     * 计算录制进度
     * 
     * @param task 录制任务
     * @return 进度信息
     */
    private RecordingStatus.Progress calculateProgress(RecordingTask task) {
        RecordingStatus.Progress progress = new RecordingStatus.Progress();
        
        // 计算录制时长（秒）
        if (task.getStartTime() != null) {
            long duration = java.time.Duration.between(
                task.getStartTime(), LocalDateTime.now()).getSeconds();
            progress.setDuration((int) duration);
        }
        
        // 获取当前文件大小
        File sizeFile = null;
        if (task.getOutputPath() != null) {
            File file = new File(task.getOutputPath());
            if (file.exists()) {
                sizeFile = file;
            }
        }
        if (sizeFile == null && task.getTempOutputPath() != null) {
            File tempFile = new File(task.getTempOutputPath());
            if (tempFile.exists()) {
                sizeFile = tempFile;
            }
        }
        if (sizeFile != null) {
            progress.setFileSize(sizeFile.length());
            if (progress.getDuration() > 0) {
                long bitrate = (sizeFile.length() * 8) / progress.getDuration(); // bits per second
                progress.setBitrate(String.format("%.2f Mbps", bitrate / 1_000_000.0));
            }
        }
        
        return progress;
    }

    private boolean finalizeRecording(RecordingTask task) {
        String tempPath = task.getTempOutputPath();
        String outputPath = task.getOutputPath();
        if (tempPath == null || outputPath == null) {
            return false;
        }
        File tempFile = new File(tempPath);
        if (!tempFile.exists() || tempFile.length() == 0) {
            return false;
        }
        boolean remuxed = recordingService.remuxToMp4(tempPath, outputPath);
        if (remuxed) {
            fileSystemManager.deleteFile(tempPath);
            return true;
        } else {
            logger.warn("封装失败，保留临时文件: {}", tempPath);
            return false;
        }
    }

    private String replaceExtension(String path, String newExtension) {
        if (path == null) {
            return null;
        }
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex <= 0) {
            return path + newExtension;
        }
        return path.substring(0, dotIndex) + newExtension;
    }

    private LiveStatus waitForLive(RecordingTask task) {
        long startTime = System.currentTimeMillis();
        String taskId = task.getTaskId();
        String douyinId = task.getDouyinId();

        task.setStatus(TaskStatus.WAITING);
        logger.info("等待开播: taskId={}, douyinId={}", taskId, douyinId);

        while (true) {
            if (task.getStatus() == TaskStatus.STOPPING || task.getStatus() == TaskStatus.CANCELLED) {
                task.setStatus(TaskStatus.CANCELLED);
                task.setEndTime(LocalDateTime.now());
                logger.info("等待开播被取消: taskId={}", taskId);
                appendTaskLog(task, "CANCELLED");
                return null;
            }

            try {
                logger.debug("等待开播轮询: taskId={}, douyinId={}", taskId, douyinId);
                LiveStatus liveStatus = liveStreamDetector.checkLiveStatus(douyinId);
                if (liveStatus.isLive()) {
                    logger.info("检测到开播: taskId={}, douyinId={}", taskId, douyinId);
                    return liveStatus;
                }
            } catch (IOException e) {
                logger.debug("等待开播检测失败: taskId={}, error={}", taskId, e.getMessage());
            }

            if (System.currentTimeMillis() - startTime > maxWaitMs) {
                task.setStatus(TaskStatus.FAILED);
                task.setError("等待开播超时");
                logger.warn("等待开播超时: taskId={}, maxWaitMs={}", taskId, maxWaitMs);
                appendTaskLog(task, "FAILED_WAIT_TIMEOUT");
                return null;
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                task.setStatus(TaskStatus.CANCELLED);
                task.setEndTime(LocalDateTime.now());
                logger.warn("等待开播被中断: taskId={}", taskId);
                appendTaskLog(task, "CANCELLED");
                return null;
            }
        }
    }

    private void startLiveEndMonitor(RecordingTask task, Process process) {
        if (!task.isAutoEnabled()) {
            return;
        }

        executorService.submit(() -> {
            long notLiveSince = -1L;
            String taskId = task.getTaskId();
            String douyinId = task.getDouyinId();

            while (process.isAlive() && task.getStatus() == TaskStatus.RECORDING) {
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (task.getStatus() != TaskStatus.RECORDING) {
                    return;
                }

                try {
                    logger.debug("录制中轮询直播状态: taskId={}, douyinId={}", taskId, douyinId);
                    LiveStatus liveStatus = liveStreamDetector.checkLiveStatus(douyinId);
                    if (liveStatus.isLive()) {
                        notLiveSince = -1L;
                        continue;
                    }

                    if (notLiveSince < 0) {
                        notLiveSince = System.currentTimeMillis();
                        logger.info("检测到直播结束，等待确认: taskId={}", taskId);
                    }

                    if (System.currentTimeMillis() - notLiveSince >= endDetectGraceMs) {
                        logger.info("直播已结束，自动停止录制: taskId={}", taskId);
                        stopTaskInternal(taskId, false);
                        return;
                    }
                } catch (IOException e) {
                    logger.debug("检测直播结束失败: taskId={}, error={}", taskId, e.getMessage());
                }
            }
        });
    }

    private synchronized void appendTaskLog(RecordingTask task, String event) {
        if (taskLogPath == null || taskLogPath.trim().isEmpty()) {
            return;
        }
        String line = String.format(
                "%s event=%s taskId=%s douyinId=%s auto=%s status=%s output=%s temp=%s%n",
                LocalDateTime.now(),
                event,
                task.getTaskId(),
                task.getDouyinId(),
                task.isAutoEnabled(),
                task.getStatus(),
                task.getOutputPath(),
                task.getTempOutputPath()
        );
        try {
            Files.writeString(
                    Path.of(taskLogPath),
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            logger.debug("写入任务日志失败: {}", e.getMessage());
        }
    }
    
    /**
     * 关闭管理器，清理所有资源
     */
    public void shutdown() {
        logger.info("关闭录制管理器...");
        
        // 停止所有活动任务
        for (RecordingTask task : listActiveTasks()) {
            try {
                stopTask(task.getTaskId());
            } catch (Exception e) {
                logger.error("停止任务失败: taskId={}", task.getTaskId(), e);
            }
        }
        
        // 关闭执行器
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("录制管理器已关闭");
    }
}
