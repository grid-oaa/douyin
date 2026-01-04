package com.douyin.liverecorder.model;

/**
 * 录制状态数据类
 * 表示录制任务的当前状态和进度信息
 */
public class RecordingStatus {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 进度信息
     */
    private Progress progress;
    
    /**
     * 错误信息（可选）
     */
    private String error;
    
    /**
     * 默认构造函数
     */
    public RecordingStatus() {
    }
    
    /**
     * 带参数的构造函数
     */
    public RecordingStatus(String taskId, TaskStatus status, Progress progress) {
        this.taskId = taskId;
        this.status = status;
        this.progress = progress;
    }
    
    // Getters and Setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Progress getProgress() {
        return progress;
    }
    
    public void setProgress(Progress progress) {
        this.progress = progress;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    /**
     * 进度内部类
     * 表示录制的详细进度信息
     */
    public static class Progress {
        /**
         * 已录制时长（秒）
         */
        private int duration;
        
        /**
         * 当前文件大小（字节）
         */
        private long fileSize;
        
        /**
         * 比特率
         */
        private String bitrate;
        
        /**
         * 默认构造函数
         */
        public Progress() {
        }
        
        /**
         * 完整构造函数
         */
        public Progress(int duration, long fileSize, String bitrate) {
            this.duration = duration;
            this.fileSize = fileSize;
            this.bitrate = bitrate;
        }
        
        // Getters and Setters
        
        public int getDuration() {
            return duration;
        }
        
        public void setDuration(int duration) {
            this.duration = duration;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
        
        public String getBitrate() {
            return bitrate;
        }
        
        public void setBitrate(String bitrate) {
            this.bitrate = bitrate;
        }
    }
}
