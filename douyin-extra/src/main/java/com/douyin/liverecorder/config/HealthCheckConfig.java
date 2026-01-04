package com.douyin.liverecorder.config;

import com.douyin.liverecorder.infrastructure.FFmpegWrapper;
import com.douyin.liverecorder.infrastructure.FileSystemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 健康检查配置
 * 提供FFmpeg和存储空间的健康检查
 */
@Configuration
public class HealthCheckConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckConfig.class);
    
    /**
     * FFmpeg健康检查指示器
     */
    @Bean
    public HealthIndicator ffmpegHealthIndicator(FFmpegWrapper ffmpegWrapper) {
        return () -> {
            try {
                boolean isAvailable = ffmpegWrapper.isFFmpegAvailable();
                
                if (isAvailable) {
                    String version = ffmpegWrapper.getFFmpegVersion();
                    logger.debug("FFmpeg健康检查: 可用, 版本={}", version);
                    return Health.up()
                            .withDetail("available", true)
                            .withDetail("version", version)
                            .build();
                } else {
                    logger.warn("FFmpeg健康检查: 不可用");
                    return Health.down()
                            .withDetail("available", false)
                            .withDetail("reason", "FFmpeg未安装或不在PATH中")
                            .build();
                }
            } catch (Exception e) {
                logger.error("FFmpeg健康检查失败", e);
                return Health.down()
                        .withDetail("available", false)
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
    
    /**
     * 存储空间健康检查指示器
     */
    @Bean
    public HealthIndicator storageHealthIndicator(FileSystemManager fileSystemManager) {
        return () -> {
            try {
                String storagePath = fileSystemManager.getStoragePath();
                long availableSpace = fileSystemManager.getAvailableSpace(storagePath);
                long totalSpace = fileSystemManager.getTotalSpace(storagePath);
                
                // 计算可用空间百分比
                double availablePercent = (double) availableSpace / totalSpace * 100;
                
                // 如果可用空间小于10%或小于1GB，标记为DOWN
                boolean isHealthy = availableSpace > 1024 * 1024 * 1024 && availablePercent > 10;
                
                logger.debug("存储空间健康检查: 可用={}MB, 总计={}MB, 百分比={:.2f}%", 
                           availableSpace / 1024 / 1024, 
                           totalSpace / 1024 / 1024, 
                           availablePercent);
                
                if (isHealthy) {
                    return Health.up()
                            .withDetail("path", storagePath)
                            .withDetail("availableSpaceMB", availableSpace / 1024 / 1024)
                            .withDetail("totalSpaceMB", totalSpace / 1024 / 1024)
                            .withDetail("availablePercent", String.format("%.2f%%", availablePercent))
                            .build();
                } else {
                    logger.warn("存储空间不足: 可用={}MB, 百分比={:.2f}%", 
                              availableSpace / 1024 / 1024, availablePercent);
                    return Health.down()
                            .withDetail("path", storagePath)
                            .withDetail("availableSpaceMB", availableSpace / 1024 / 1024)
                            .withDetail("totalSpaceMB", totalSpace / 1024 / 1024)
                            .withDetail("availablePercent", String.format("%.2f%%", availablePercent))
                            .withDetail("reason", "存储空间不足")
                            .build();
                }
            } catch (Exception e) {
                logger.error("存储空间健康检查失败", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
