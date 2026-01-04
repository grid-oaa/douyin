package com.douyin.liverecorder.model;

import java.time.LocalDateTime;

/**
 * 直播状态数据类
 * 表示抖音直播间的当前状态信息
 */
public class LiveStatus {
    /**
     * 是否正在直播
     */
    private boolean isLive;
    
    /**
     * 直播间ID
     */
    private String roomId;
    
    /**
     * 直播标题
     */
    private String title;
    
    /**
     * 开播时间（可选）
     */
    private LocalDateTime startTime;
    
    /**
     * 默认构造函数
     */
    public LiveStatus() {
    }
    
    /**
     * 完整构造函数
     */
    public LiveStatus(boolean isLive, String roomId, String title, LocalDateTime startTime) {
        this.isLive = isLive;
        this.roomId = roomId;
        this.title = title;
        this.startTime = startTime;
    }
    
    // Getters and Setters
    
    public boolean isLive() {
        return isLive;
    }
    
    public void setLive(boolean live) {
        isLive = live;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
