package com.douyin.liverecorder.service;

import com.douyin.liverecorder.infrastructure.FFmpegWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 录制服务
 * 负责使用FFmpeg录制直播流
 */
@Service
public class RecordingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingService.class);
    
    private final FFmpegWrapper ffmpegWrapper;
    
    // 存储录制进程的映射（输出路径 -> 进程）
    private final ConcurrentHashMap<String, Process> recordingProcesses = new ConcurrentHashMap<>();
    
    // 进程监控器
    private final ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(1);
    
    public RecordingService(FFmpegWrapper ffmpegWrapper) {
        this.ffmpegWrapper = ffmpegWrapper;
        
        // 启动进程监控任务
        startProcessMonitor();
    }
    
    /**
     * 开始录制
     * 
     * @param streamUrl 直播流URL
     * @param outputPath 输出文件路径
     * @return 录制进程
     * @throws IOException 如果启动录制失败
     */
    public Process startRecording(String streamUrl, String outputPath) throws IOException {
        if (streamUrl == null || streamUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("流URL不能为空");
        }
        
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("输出路径不能为空");
        }
        
        // 检查是否已经在录制
        if (recordingProcesses.containsKey(outputPath)) {
            Process existingProcess = recordingProcesses.get(outputPath);
            if (ffmpegWrapper.isRunning(existingProcess)) {
                throw new IllegalStateException("该输出路径已经在录制中: " + outputPath);
            } else {
                // 清理已结束的进程
                recordingProcesses.remove(outputPath);
            }
        }
        
        logger.info("开始录制: streamUrl={}, outputPath={}", maskUrl(streamUrl), outputPath);
        
        try {
            // 确保输出目录存在
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    throw new IOException("无法创建输出目录: " + parentDir.getAbsolutePath());
                }
            }
            
            // 构建FFmpeg命令
            List<String> command = ffmpegWrapper.buildRecordingCommand(streamUrl, outputPath);
            
            // 启动FFmpeg进程
            Process process = ffmpegWrapper.execute(command);
            
            // 存储进程引用
            recordingProcesses.put(outputPath, process);
            
            logger.info("录制已启动: outputPath={}, PID={}", outputPath, process.pid());
            
            return process;
            
        } catch (IOException e) {
            logger.error("启动录制失败: {}", outputPath, e);
            throw new IOException("启动录制失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将录制的临时文件封装为MP4
     *
     * @param inputPath 录制临时文件路径（例如 .flv）
     * @param outputPath 目标MP4路径
     * @return 封装成功返回true，否则返回false
     */
    public boolean remuxToMp4(String inputPath, String outputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            return false;
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            return false;
        }

        logger.info("开始封装为MP4: input={}, output={}", inputPath, outputPath);
        try {
            List<String> command = ffmpegWrapper.buildRemuxCommand(inputPath, outputPath);
            Process process = ffmpegWrapper.execute(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("封装成功: {}", outputPath);
                return true;
            }
            String output = ffmpegWrapper.getOutput(process);
            if (output != null && !output.isEmpty()) {
                logger.warn("封装失败输出: {}", output);
            }
            logger.warn("封装失败: exitCode={}", exitCode);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("封装被中断: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("封装失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 停止录制
     * 
     * @param process 录制进程
     * @return 如果成功停止返回true，否则返回false
     */
    public boolean stopRecording(Process process) {
        if (process == null) {
            logger.warn("进程为null，无法停止录制");
            return false;
        }
        
        logger.info("停止录制: PID={}", process.pid());
        
        try {
            // 使用FFmpegWrapper终止进程
            boolean stopped = ffmpegWrapper.kill(process);
            
            if (stopped) {
                // 从映射中移除进程
                recordingProcesses.values().remove(process);
                logger.info("录制已停止: PID={}", process.pid());
            } else {
                logger.warn("停止录制失败: PID={}", process.pid());
            }
            
            return stopped;
            
        } catch (Exception e) {
            logger.error("停止录制时发生错误: PID={}", process.pid(), e);
            return false;
        }
    }
    
    /**
     * 通过输出路径停止录制
     * 
     * @param outputPath 输出文件路径
     * @return 如果成功停止返回true，否则返回false
     */
    public boolean stopRecordingByPath(String outputPath) {
        if (outputPath == null) {
            return false;
        }
        
        Process process = recordingProcesses.get(outputPath);
        if (process == null) {
            logger.debug("未找到录制进程: {}", outputPath);
            return false;
        }
        
        return stopRecording(process);
    }
    
    /**
     * 检查是否正在录制
     * 
     * @param process 录制进程
     * @return 如果正在录制返回true，否则返回false
     */
    public boolean isRecording(Process process) {
        if (process == null) {
            return false;
        }
        
        return ffmpegWrapper.isRunning(process);
    }
    
    /**
     * 通过输出路径检查是否正在录制
     * 
     * @param outputPath 输出文件路径
     * @return 如果正在录制返回true，否则返回false
     */
    public boolean isRecordingByPath(String outputPath) {
        if (outputPath == null) {
            return false;
        }
        
        Process process = recordingProcesses.get(outputPath);
        if (process == null) {
            return false;
        }
        
        return isRecording(process);
    }
    
    /**
     * 获取录制进程的输出
     * 
     * @param process 录制进程
     * @return 进程输出内容
     */
    public String getRecordingOutput(Process process) {
        if (process == null) {
            return "";
        }
        
        return ffmpegWrapper.getOutput(process);
    }
    
    /**
     * 获取录制进程的退出码
     * 
     * @param process 录制进程
     * @return 退出码，如果进程还在运行返回null
     */
    public Integer getExitCode(Process process) {
        if (process == null) {
            return null;
        }
        
        return ffmpegWrapper.getExitCode(process);
    }
    
    /**
     * 获取所有活动的录制进程数量
     * 
     * @return 活动录制进程数量
     */
    public int getActiveRecordingCount() {
        // 清理已结束的进程
        recordingProcesses.entrySet().removeIf(entry -> !ffmpegWrapper.isRunning(entry.getValue()));
        
        return recordingProcesses.size();
    }
    
    /**
     * 检查进程是否因流中断而失败
     * 
     * @param process 录制进程
     * @return 如果是流中断返回true，否则返回false
     */
    public boolean isStreamInterrupted(Process process) {
        if (process == null || ffmpegWrapper.isRunning(process)) {
            return false;
        }
        
        // 检查进程输出中是否包含流中断的错误信息
        String output = ffmpegWrapper.getOutput(process);
        if (output == null) {
            return false;
        }
        
        // 常见的流中断错误关键词
        String[] interruptionKeywords = {
            "Connection refused",
            "Connection reset",
            "Connection timed out",
            "Server returned 404",
            "Server returned 403",
            "End of file",
            "I/O error",
            "Invalid data found"
        };
        
        String lowerOutput = output.toLowerCase();
        for (String keyword : interruptionKeywords) {
            if (lowerOutput.contains(keyword.toLowerCase())) {
                logger.debug("检测到流中断: {}", keyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 启动进程监控任务
     * 定期检查录制进程状态，清理已结束的进程
     */
    private void startProcessMonitor() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                // 清理已结束的进程
                recordingProcesses.entrySet().removeIf(entry -> {
                    Process process = entry.getValue();
                    if (!ffmpegWrapper.isRunning(process)) {
                        String outputPath = entry.getKey();
                        Integer exitCode = ffmpegWrapper.getExitCode(process);
                        
                        if (exitCode != null && exitCode != 0) {
                            logger.warn("录制进程异常退出: outputPath={}, exitCode={}", 
                                       outputPath, exitCode);
                            
                            // 检查是否为流中断
                            if (isStreamInterrupted(process)) {
                                logger.warn("检测到流中断: {}", outputPath);
                            }
                        } else {
                            logger.debug("录制进程正常结束: {}", outputPath);
                        }
                        
                        return true; // 移除已结束的进程
                    }
                    return false;
                });
                
            } catch (Exception e) {
                logger.error("进程监控任务发生错误", e);
            }
        }, 10, 10, TimeUnit.SECONDS); // 每10秒检查一次
        
        logger.info("进程监控任务已启动");
    }
    
    /**
     * 掩码URL用于日志输出（隐藏敏感参数）
     * 
     * @param url 原始URL
     * @return 掩码后的URL
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }
        
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex) + "?...";
        }
        
        return url;
    }
    
    /**
     * 关闭服务，清理所有资源
     */
    public void shutdown() {
        logger.info("关闭录制服务...");
        
        // 停止所有录制进程
        recordingProcesses.values().forEach(this::stopRecording);
        
        // 关闭监控器
        monitorExecutor.shutdown();
        try {
            if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("录制服务已关闭");
    }
}
