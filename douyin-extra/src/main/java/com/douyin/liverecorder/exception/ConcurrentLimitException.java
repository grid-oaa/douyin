package com.douyin.liverecorder.exception;

/**
 * 并发限制异常
 * 当录制任务数量达到最大并发限制时抛出
 */
public class ConcurrentLimitException extends RuntimeException {
    
    private final int currentCount;
    private final int maxLimit;
    
    public ConcurrentLimitException(int currentCount, int maxLimit) {
        super(String.format("当前录制任务已达上限（%d个），请稍后重试", maxLimit));
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
    }
    
    public ConcurrentLimitException(int currentCount, int maxLimit, String message) {
        super(message);
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
    }
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public int getMaxLimit() {
        return maxLimit;
    }
}
