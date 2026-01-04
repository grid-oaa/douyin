package com.douyin.liverecorder.model;

/**
 * 流信息数据类
 * 表示直播流的详细信息
 */
public class StreamInfo {
    /**
     * 直播流URL
     */
    private String url;
    
    /**
     * 流格式 (flv, m3u8等)
     */
    private String format;
    
    /**
     * 画质 (origin, high, medium)
     */
    private String quality;
    
    /**
     * URL是否有效
     */
    private boolean isValid;
    
    /**
     * 默认构造函数
     */
    public StreamInfo() {
    }
    
    /**
     * 完整构造函数
     */
    public StreamInfo(String url, String format, String quality, boolean isValid) {
        this.url = url;
        this.format = format;
        this.quality = quality;
        this.isValid = isValid;
    }
    
    // Getters and Setters
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public String getQuality() {
        return quality;
    }
    
    public void setQuality(String quality) {
        this.quality = quality;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
}
