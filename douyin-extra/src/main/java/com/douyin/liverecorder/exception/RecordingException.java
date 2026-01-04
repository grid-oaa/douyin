package com.douyin.liverecorder.exception;

/**
 * 录制异常
 * 当录制过程中发生错误时抛出（如FFmpeg错误、流中断、磁盘空间不足等）
 */
public class RecordingException extends RuntimeException {
    
    private final String taskId;
    
    public RecordingException(String message) {
        super(message);
        this.taskId = null;
    }
    
    public RecordingException(String taskId, String message) {
        super(message);
        this.taskId = taskId;
    }
    
    public RecordingException(String message, Throwable cause) {
        super(message, cause);
        this.taskId = null;
    }
    
    public RecordingException(String taskId, String message, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
    }
    
    public String getTaskId() {
        return taskId;
    }
}
