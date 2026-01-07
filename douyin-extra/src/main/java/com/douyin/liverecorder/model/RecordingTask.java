package com.douyin.liverecorder.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 录制任务实体类
 * 表示一个直播录制任务的完整信息
 */
public class RecordingTask {
    /**
     * 唯一任务ID
     */
    private String taskId;
    
    /**
     * 抖音号
     */
    private String douyinId;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 直播流URL
     */
    private String streamUrl;
    
    /**
     * 输出文件路径
     */
    private String outputPath;
    
    /**
     * 保存目录
     */
    private String outputDir;

    /**
     * 录制临时文件路径（例如 .flv）
     */
    private String tempOutputPath;

    /**
     * 是否启用自动录制（等待开播/自动停播）
     */
    private boolean autoEnabled;

    /**
     * 停止请求是否由用户触发
     */
    private boolean stopRequestedByUser;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间（可选）
     */
    private LocalDateTime endTime;
    
    /**
     * 文件大小（字节）
     */
    private long fileSize;
    
    /**
     * 错误信息（可选）
     */
    private String error;
    
    /**
     * 默认构造函数
     */
    public RecordingTask() {
        this.taskId = UUID.randomUUID().toString();
        this.status = TaskStatus.PENDING;
        this.fileSize = 0L;
    }
    
    /**
     * 带抖音号的构造函数
     */
    public RecordingTask(String douyinId) {
        this();
        this.douyinId = douyinId;
    }
    
    // Getters and Setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getDouyinId() {
        return douyinId;
    }
    
    public void setDouyinId(String douyinId) {
        this.douyinId = douyinId;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getStreamUrl() {
        return streamUrl;
    }
    
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
    
    public String getOutputPath() {
        return outputPath;
    }
    
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
    
    public String getOutputDir() {
        return outputDir;
    }
    
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getTempOutputPath() {
        return tempOutputPath;
    }

    public void setTempOutputPath(String tempOutputPath) {
        this.tempOutputPath = tempOutputPath;
    }

    public boolean isAutoEnabled() {
        return autoEnabled;
    }

    public void setAutoEnabled(boolean autoEnabled) {
        this.autoEnabled = autoEnabled;
    }

    public boolean isStopRequestedByUser() {
        return stopRequestedByUser;
    }

    public void setStopRequestedByUser(boolean stopRequestedByUser) {
        this.stopRequestedByUser = stopRequestedByUser;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
