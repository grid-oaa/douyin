package com.douyin.liverecorder.model;

/**
 * 录制任务状态枚举
 * 定义了录制任务在其生命周期中可能处于的所有状态
 */
public enum TaskStatus {
    /**
     * 等待中 - 任务已创建但尚未开始
     */
    PENDING,

    WAITING,
    
    /**
     * 检测直播状态中 - 正在检查直播间是否在线
     */
    DETECTING,
    
    /**
     * 录制中 - 正在录制直播流
     */
    RECORDING,
    
    /**
     * 停止中 - 正在停止录制过程
     */
    STOPPING,
    
    /**
     * 已完成 - 录制成功完成
     */
    COMPLETED,
    
    /**
     * 失败 - 录制过程中发生错误
     */
    FAILED,
    
    /**
     * 已取消 - 用户取消了录制任务
     */
    CANCELLED
}
