package com.douyin.liverecorder.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 文件系统管理器
 * 负责管理录制文件的存储
 */
@Component
public class FileSystemManager {
    
    private static final Logger logger = LoggerFactory.getLogger(FileSystemManager.class);
    
    @Value("${recording.storage.path:./recordings}")
    private String storagePath;
    
    /**
     * 生成录制文件名
     * 格式：{douyinId}_{YYYYMMDD}_{HHMMSS}.mp4
     * 
     * @param douyinId 抖音号
     * @param timestamp 时间戳
     * @return 生成的文件名
     */
    public String generateFilename(String douyinId, LocalDateTime timestamp) {
        return FileNameGenerator.generateFilename(douyinId, timestamp);
    }
    
    /**
     * 确保目录存在，如果不存在则创建
     * 
     * @param path 目录路径
     * @return 如果目录存在或成功创建返回true，否则返回false
     */
    public boolean ensureDirectory(String path) {
        if (path == null || path.isEmpty()) {
            logger.error("目录路径不能为空");
            return false;
        }
        
        try {
            Path dirPath = Paths.get(path);
            
            // 如果目录已存在
            if (Files.exists(dirPath)) {
                if (!Files.isDirectory(dirPath)) {
                    logger.error("路径存在但不是目录: {}", path);
                    return false;
                }
                logger.debug("目录已存在: {}", path);
                return true;
            }
            
            // 创建目录（包括所有必要的父目录）
            Files.createDirectories(dirPath);
            logger.info("成功创建目录: {}", path);
            return true;
            
        } catch (IOException e) {
            logger.error("创建目录失败: {}", path, e);
            return false;
        } catch (SecurityException e) {
            logger.error("没有权限创建目录: {}", path, e);
            return false;
        }
    }
    
    /**
     * 获取指定路径的可用磁盘空间
     * 
     * @param path 路径
     * @return 可用空间（字节），如果获取失败返回-1
     */
    public long getAvailableSpace(String path) {
        if (path == null || path.isEmpty()) {
            logger.error("路径不能为空");
            return -1;
        }
        
        try {
            File file = new File(path);
            
            // 如果路径不存在，使用父目录
            if (!file.exists()) {
                file = file.getParentFile();
                if (file == null) {
                    file = new File(".");
                }
            }
            
            long availableSpace = file.getUsableSpace();
            logger.debug("路径 {} 的可用空间: {} 字节", path, availableSpace);
            return availableSpace;
            
        } catch (SecurityException e) {
            logger.error("没有权限访问路径: {}", path, e);
            return -1;
        }
    }
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     * @return 如果文件成功删除或不存在返回true，否则返回false
     */
    public boolean deleteFile(String path) {
        if (path == null || path.isEmpty()) {
            logger.error("文件路径不能为空");
            return false;
        }
        
        try {
            Path filePath = Paths.get(path);
            
            // 如果文件不存在，认为删除成功
            if (!Files.exists(filePath)) {
                logger.debug("文件不存在，无需删除: {}", path);
                return true;
            }
            
            // 确保是文件而不是目录
            if (Files.isDirectory(filePath)) {
                logger.error("路径是目录而不是文件: {}", path);
                return false;
            }
            
            // 删除文件
            Files.delete(filePath);
            logger.info("成功删除文件: {}", path);
            return true;
            
        } catch (IOException e) {
            logger.error("删除文件失败: {}", path, e);
            return false;
        } catch (SecurityException e) {
            logger.error("没有权限删除文件: {}", path, e);
            return false;
        }
    }
    
    /**
     * 获取完整的文件路径
     * 
     * @param filename 文件名
     * @return 完整的文件路径
     */
    public String getFullPath(String filename) {
        return Paths.get(storagePath, filename).toString();
    }
    
    /**
     * 获取存储路径
     * 
     * @return 存储路径
     */
    public String getStoragePath() {
        return storagePath;
    }
    
    /**
     * 初始化存储目录
     * 在应用启动时调用
     * 
     * @return 如果初始化成功返回true，否则返回false
     */
    public boolean initializeStorage() {
        logger.info("初始化存储目录: {}", storagePath);
        return ensureDirectory(storagePath);
    }
    
    /**
     * 获取指定路径的总磁盘空间
     * 
     * @param path 路径
     * @return 总空间（字节），如果获取失败返回-1
     */
    public long getTotalSpace(String path) {
        if (path == null || path.isEmpty()) {
            logger.error("路径不能为空");
            return -1;
        }
        
        try {
            File file = new File(path);
            
            // 如果路径不存在，使用父目录
            if (!file.exists()) {
                file = file.getParentFile();
                if (file == null) {
                    file = new File(".");
                }
            }
            
            long totalSpace = file.getTotalSpace();
            logger.debug("路径 {} 的总空间: {} 字节", path, totalSpace);
            return totalSpace;
            
        } catch (SecurityException e) {
            logger.error("没有权限访问路径: {}", path, e);
            return -1;
        }
    }
}
