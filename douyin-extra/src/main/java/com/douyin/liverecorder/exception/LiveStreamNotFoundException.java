package com.douyin.liverecorder.exception;

/**
 * 直播未找到异常
 * 当用户未开播或直播间不存在时抛出
 */
public class LiveStreamNotFoundException extends RuntimeException {
    
    private final String douyinId;
    
    public LiveStreamNotFoundException(String douyinId) {
        super(String.format("该用户当前未在直播：%s", douyinId));
        this.douyinId = douyinId;
    }
    
    public LiveStreamNotFoundException(String douyinId, String message) {
        super(message);
        this.douyinId = douyinId;
    }
    
    public String getDouyinId() {
        return douyinId;
    }
}
