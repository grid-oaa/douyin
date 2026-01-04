package com.douyin.liverecorder.infrastructure;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件名生成器
 * 负责生成符合规范的录制文件名
 */
public class FileNameGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final String FILE_EXTENSION = ".mp4";
    
    /**
     * 生成录制文件名
     * 格式：{douyinId}_{YYYYMMDD}_{HHMMSS}.mp4
     * 
     * @param douyinId 抖音号
     * @param timestamp 时间戳
     * @return 生成的文件名
     */
    public static String generateFilename(String douyinId, LocalDateTime timestamp) {
        if (douyinId == null || douyinId.isEmpty()) {
            throw new IllegalArgumentException("抖音号不能为空");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("时间戳不能为空");
        }
        
        String datePart = timestamp.format(DATE_FORMATTER);
        String timePart = timestamp.format(TIME_FORMATTER);
        
        return douyinId + "_" + datePart + "_" + timePart + FILE_EXTENSION;
    }
    
    /**
     * 从文件名中解析抖音号
     * 
     * @param filename 文件名
     * @return 抖音号，如果解析失败返回null
     */
    public static String parseDouyinId(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        // 移除.mp4扩展名
        if (filename.endsWith(FILE_EXTENSION)) {
            filename = filename.substring(0, filename.length() - FILE_EXTENSION.length());
        }
        
        // 分割文件名
        String[] parts = filename.split("_");
        if (parts.length != 3) {
            return null;
        }
        
        return parts[0];
    }
    
    /**
     * 从文件名中解析日期部分
     * 
     * @param filename 文件名
     * @return 日期字符串（YYYYMMDD格式），如果解析失败返回null
     */
    public static String parseDate(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        // 移除.mp4扩展名
        if (filename.endsWith(FILE_EXTENSION)) {
            filename = filename.substring(0, filename.length() - FILE_EXTENSION.length());
        }
        
        // 分割文件名
        String[] parts = filename.split("_");
        if (parts.length != 3) {
            return null;
        }
        
        return parts[1];
    }
    
    /**
     * 从文件名中解析时间部分
     * 
     * @param filename 文件名
     * @return 时间字符串（HHMMSS格式），如果解析失败返回null
     */
    public static String parseTime(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        // 移除.mp4扩展名
        if (filename.endsWith(FILE_EXTENSION)) {
            filename = filename.substring(0, filename.length() - FILE_EXTENSION.length());
        }
        
        // 分割文件名
        String[] parts = filename.split("_");
        if (parts.length != 3) {
            return null;
        }
        
        return parts[2];
    }
    
    /**
     * 验证文件名格式是否正确
     * 
     * @param filename 文件名
     * @return 如果格式正确返回true，否则返回false
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        // 检查扩展名
        if (!filename.endsWith(FILE_EXTENSION)) {
            return false;
        }
        
        // 移除扩展名
        String nameWithoutExt = filename.substring(0, filename.length() - FILE_EXTENSION.length());
        
        // 分割并验证格式
        String[] parts = nameWithoutExt.split("_");
        if (parts.length != 3) {
            return false;
        }
        
        // 验证日期部分（8位数字）
        String datePart = parts[1];
        if (datePart.length() != 8 || !datePart.matches("\\d{8}")) {
            return false;
        }
        
        // 验证时间部分（6位数字）
        String timePart = parts[2];
        if (timePart.length() != 6 || !timePart.matches("\\d{6}")) {
            return false;
        }
        
        // 验证抖音号部分（非空）
        String douyinIdPart = parts[0];
        if (douyinIdPart.isEmpty()) {
            return false;
        }
        
        return true;
    }
}
