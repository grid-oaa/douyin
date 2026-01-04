package com.douyin.liverecorder.exception;

/**
 * 无效抖音号异常
 * 当提供的抖音号格式无效时抛出
 */
public class InvalidDouyinIdException extends RuntimeException {
    
    private final String douyinId;
    
    public InvalidDouyinIdException(String douyinId) {
        super(String.format("抖音号格式无效：%s。请输入有效的抖音号（字母和数字组合）", douyinId));
        this.douyinId = douyinId;
    }
    
    public InvalidDouyinIdException(String douyinId, String message) {
        super(message);
        this.douyinId = douyinId;
    }
    
    public String getDouyinId() {
        return douyinId;
    }
}
