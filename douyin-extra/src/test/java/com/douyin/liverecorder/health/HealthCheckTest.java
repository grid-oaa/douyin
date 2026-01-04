package com.douyin.liverecorder.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 健康检查端点测试
 * 验证健康检查端点正常工作
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HealthCheckTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    /**
     * 测试健康检查端点可访问
     */
    @Test
    public void testHealthEndpointAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").exists());
    }
    
    /**
     * 测试FFmpeg健康检查存在
     */
    @Test
    public void testFFmpegHealthIndicatorExists() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components.ffmpeg").exists())
                .andExpect(jsonPath("$.components.ffmpeg.status").exists())
                .andExpect(jsonPath("$.components.ffmpeg.details.available").exists());
    }
    
    /**
     * 测试存储健康检查存在
     */
    @Test
    public void testStorageHealthIndicatorExists() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components.storage").exists())
                .andExpect(jsonPath("$.components.storage.status").value("UP"));
    }
    
    /**
     * 测试存储健康检查包含详细信息
     */
    @Test
    public void testStorageHealthIndicatorDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components.storage.details.path").exists())
                .andExpect(jsonPath("$.components.storage.details.availableSpaceMB").exists())
                .andExpect(jsonPath("$.components.storage.details.totalSpaceMB").exists())
                .andExpect(jsonPath("$.components.storage.details.availablePercent").exists());
    }
}
