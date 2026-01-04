package com.douyin.liverecorder.exception;

/**
 * 网络异常
 * 当网络连接失败或请求超时时抛出
 */
public class NetworkException extends RuntimeException {
    
    private final String url;
    
    public NetworkException(String message) {
        super(message);
        this.url = null;
    }
    
    public NetworkException(String url, String message) {
        super(message);
        this.url = url;
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
        this.url = null;
    }
    
    public NetworkException(String url, String message, Throwable cause) {
        super(message, cause);
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }
}
