package com.douyin.liverecorder.dto;

import com.douyin.liverecorder.model.RecordingTask;
import com.douyin.liverecorder.model.TaskStatus;

import java.time.LocalDateTime;

/**
 * 录制响应DTO
 * 用于返回录制任务信息
 */
public class RecordingResponse {
    
    private String taskId;
    private String douyinId;
    private TaskStatus status;
    private String streamUrl;
    private String outputPath;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long fileSize;
    private String error;
    
    public RecordingResponse() {
    }
    
    public RecordingResponse(RecordingTask task) {
        this.taskId = task.getTaskId();
        this.douyinId = task.getDouyinId();
        this.status = task.getStatus();
        this.streamUrl = task.getStreamUrl();
        this.outputPath = task.getOutputPath();
        this.startTime = task.getStartTime();
        this.endTime = task.getEndTime();
        this.fileSize = task.getFileSize();
        this.error = task.getError();
    }
    
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
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
