package com.douyin.liverecorder.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg包装器
 * 封装FFmpeg命令行调用
 */
@Component
public class FFmpegWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(FFmpegWrapper.class);
    
    // 存储进程和其输出的映射
    private final ConcurrentHashMap<Process, StringBuilder> processOutputMap = new ConcurrentHashMap<>();
    
    /**
     * 执行FFmpeg命令
     * 
     * @param command FFmpeg命令列表
     * @return 启动的进程
     * @throws IOException 如果启动进程失败
     */
    public Process execute(List<String> command) throws IOException {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("命令不能为空");
        }
        
        logger.info("执行FFmpeg命令: {}", String.join(" ", command));
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
            
            Process process = processBuilder.start();
            
            // 初始化输出缓冲区
            StringBuilder outputBuffer = new StringBuilder();
            processOutputMap.put(process, outputBuffer);
            
            // 启动线程读取进程输出
            startOutputReader(process, outputBuffer);
            
            logger.info("FFmpeg进程已启动，PID: {}", process.pid());
            return process;
            
        } catch (IOException e) {
            logger.error("启动FFmpeg进程失败", e);
            throw e;
        }
    }
    
    /**
     * 构建录制命令
     * 命令格式（MP4）：ffmpeg -i <stream_url> -c copy -bsf:a aac_adtstoasc -movflags +faststart -y <output_file>
     * 命令格式（FLV）：ffmpeg -i <stream_url> -c copy -f flv -y <output_file>
     * 
     * @param streamUrl 直播流URL
     * @param outputFile 输出文件路径
     * @return FFmpeg命令列表
     */
    public List<String> buildRecordingCommand(String streamUrl, String outputFile) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(streamUrl);
        command.add("-c");
        command.add("copy");
        if (outputFile != null && outputFile.toLowerCase().endsWith(".flv")) {
            command.add("-f");
            command.add("flv");
        } else {
            command.add("-bsf:a");
            command.add("aac_adtstoasc");
            command.add("-movflags");
            command.add("+faststart");
        }
        command.add("-y");
        command.add(outputFile);
        
        return command;
    }

    /**
     * 构建封装转换命令（FLV -> MP4）
     *
     * @param inputFile 输入文件
     * @param outputFile 输出文件
     * @return FFmpeg命令列表
     */
    public List<String> buildRemuxCommand(String inputFile, String outputFile) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFile);
        command.add("-c");
        command.add("copy");
        command.add("-movflags");
        command.add("+faststart");
        command.add("-y");
        command.add(outputFile);
        return command;
    }
    
    /**
     * 终止进程
     * 
     * @param process 要终止的进程
     * @return 如果成功终止返回true，否则返回false
     */
    public boolean kill(Process process) {
        if (process == null) {
            logger.warn("进程为null，无法终止");
            return false;
        }
        
        if (!process.isAlive()) {
            logger.debug("进程已经不在运行");
            cleanupProcess(process);
            return true;
        }
        
        try {
            logger.info("正在终止FFmpeg进程，PID: {}", process.pid());
            
            // 首先尝试发送 q 进行优雅退出
            try {
                process.getOutputStream().write("q\n".getBytes());
                process.getOutputStream().flush();
            } catch (IOException e) {
                logger.debug("无法写入FFmpeg标准输入，改用destroy: {}", e.getMessage());
            }

            process.destroy();
            
            // 等待最多3秒让进程优雅退出
            boolean exited = process.waitFor(3, TimeUnit.SECONDS);
            
            if (!exited) {
                // 如果进程没有退出，强制终止
                logger.warn("进程未能优雅退出，强制终止");
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
            
            cleanupProcess(process);
            logger.info("FFmpeg进程已终止");
            return true;
            
        } catch (InterruptedException e) {
            logger.error("等待进程终止时被中断", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 获取进程输出
     * 
     * @param process 进程
     * @return 进程的输出内容
     */
    public String getOutput(Process process) {
        if (process == null) {
            return "";
        }
        
        StringBuilder output = processOutputMap.get(process);
        if (output == null) {
            return "";
        }
        
        return output.toString();
    }
    
    /**
     * 检查进程是否正在运行
     * 
     * @param process 进程
     * @return 如果进程正在运行返回true，否则返回false
     */
    public boolean isRunning(Process process) {
        if (process == null) {
            return false;
        }
        
        return process.isAlive();
    }
    
    /**
     * 获取进程的退出码
     * 
     * @param process 进程
     * @return 退出码，如果进程还在运行返回null
     */
    public Integer getExitCode(Process process) {
        if (process == null || process.isAlive()) {
            return null;
        }
        
        return process.exitValue();
    }
    
    /**
     * 启动输出读取线程
     * 
     * @param process 进程
     * @param outputBuffer 输出缓冲区
     */
    private void startOutputReader(Process process, StringBuilder outputBuffer) {
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuffer.append(line).append("\n");
                    
                    // 记录重要的FFmpeg输出
                    if (line.contains("error") || line.contains("Error") || 
                        line.contains("failed") || line.contains("Failed")) {
                        logger.warn("FFmpeg输出: {}", line);
                    } else {
                        logger.debug("FFmpeg输出: {}", line);
                    }
                }
                
            } catch (IOException e) {
                logger.error("读取进程输出时发生错误", e);
            }
        });
        
        readerThread.setDaemon(true);
        readerThread.setName("FFmpeg-Output-Reader-" + process.pid());
        readerThread.start();
    }
    
    /**
     * 清理进程相关资源
     * 
     * @param process 进程
     */
    private void cleanupProcess(Process process) {
        if (process != null) {
            processOutputMap.remove(process);
        }
    }
    
    /**
     * 检查FFmpeg是否可用
     * 
     * @return 如果FFmpeg可用返回true，否则返回false
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            
            if (exited && process.exitValue() == 0) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.debug("检查FFmpeg可用性失败", e);
            return false;
        }
    }
    
    /**
     * 获取FFmpeg版本信息
     * 
     * @return FFmpeg版本字符串
     */
    public String getFFmpegVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null) {
                    return firstLine;
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            return "未知版本";
            
        } catch (Exception e) {
            logger.debug("获取FFmpeg版本失败", e);
            return "未知版本";
        }
    }
}
