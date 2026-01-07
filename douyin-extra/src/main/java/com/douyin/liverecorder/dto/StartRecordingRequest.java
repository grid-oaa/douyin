package com.douyin.liverecorder.dto;

import com.douyin.liverecorder.validation.ValidDouyinId;
import jakarta.validation.constraints.NotBlank;

/**
 * 开始录制请求DTO
 * 用于接收用户的录制请求
 */
public class StartRecordingRequest {
    
    @NotBlank(message = "抖音号不能为空")
    @ValidDouyinId
    private String douyinId;
    private Boolean auto;
    private String outputDir;
    
    public StartRecordingRequest() {
    }
    
    public StartRecordingRequest(String douyinId) {
        this.douyinId = douyinId;
    }

    public StartRecordingRequest(String douyinId, Boolean auto) {
        this.douyinId = douyinId;
        this.auto = auto;
    }
    
    public String getDouyinId() {
        return douyinId;
    }
    
    public void setDouyinId(String douyinId) {
        this.douyinId = douyinId;
    }

    public Boolean getAuto() {
        return auto;
    }

    public void setAuto(Boolean auto) {
        this.auto = auto;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}
