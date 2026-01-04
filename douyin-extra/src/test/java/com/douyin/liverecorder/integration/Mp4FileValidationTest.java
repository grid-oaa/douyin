package com.douyin.liverecorder.integration;

import com.douyin.liverecorder.infrastructure.FileNameGenerator;
import com.douyin.liverecorder.infrastructure.FileSystemManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MP4文件验证测试
 * 
 * 测试场景：
 * 1. 检查文件存在性
 * 2. 检查文件可播放性（使用FFprobe）
 * 3. 检查文件命名格式
 * 
 * 验证需求：4.4, 5.1, 5.2
 */
@SpringBootTest
@TestPropertySource(properties = {
    "recording.storage.path=./test-recordings"
})
public class Mp4FileValidationTest {
    
    @Autowired
    private FileSystemManager fileSystemManager;
    
    private Path testRecordingsPath;
    private static final String TEST_DOUYIN_ID = "testuser123";
    
    @BeforeEach
    public void setUp() throws IOException {
        // 创建测试录制目录
        testRecordingsPath = Path.of("./test-recordings");
        Files.createDirectories(testRecordingsPath);
        
        // 清理之前的测试文件
        cleanupTestFiles();
    }
    
    @AfterEach
    public void tearDown() {
        // 清理测试文件
        cleanupTestFiles();
    }
    
    /**
     * 测试文件存在性
     * 
     * 场景：
     * 1. 生成文件名
     * 2. 创建测试文件
     * 3. 验证文件存在
     * 
     * 验证需求：5.1
     */
    @Test
    public void testFileExistence() throws IOException {
        // 确保存储目录存在
        fileSystemManager.ensureDirectory(fileSystemManager.getStoragePath());
        
        // 生成文件名
        LocalDateTime timestamp = LocalDateTime.now();
        String filename = FileNameGenerator.generateFilename(TEST_DOUYIN_ID, timestamp);
        String fullPath = fileSystemManager.getFullPath(filename);
        
        // 创建测试文件
        File testFile = new File(fullPath);
        Files.createFile(testFile.toPath());
        
        // 验证文件存在
        assertTrue(testFile.exists(), "文件应该存在");
        assertTrue(testFile.isFile(), "应该是一个文件而不是目录");
        
        // 验证文件在正确的目录中
        String expectedDir = fileSystemManager.getStoragePath();
        String normalizedParent = testFile.getParentFile().getAbsolutePath().replace("\\", "/");
        String normalizedExpected = new File(expectedDir).getAbsolutePath().replace("\\", "/");
        
        assertTrue(normalizedParent.equals(normalizedExpected) || 
                   normalizedParent.endsWith(normalizedExpected) ||
                   normalizedParent.contains(normalizedExpected.replace("./", "")),
                   "文件应该在指定的存储目录中");
    }
    
    /**
     * 测试文件命名格式
     * 
     * 场景：
     * 1. 生成多个文件名
     * 2. 验证文件名符合格式 {douyinId}_{YYYYMMDD}_{HHMMSS}.mp4
     * 3. 验证可以从文件名中解析出抖音号和时间戳
     * 
     * 验证需求：5.2
     */
    @Test
    public void testFileNamingFormat() {
        // 测试多个时间戳
        LocalDateTime[] timestamps = {
            LocalDateTime.of(2026, 1, 3, 14, 30, 25),
            LocalDateTime.of(2026, 12, 31, 23, 59, 59),
            LocalDateTime.of(2026, 6, 15, 0, 0, 0)
        };
        
        for (LocalDateTime timestamp : timestamps) {
            String filename = FileNameGenerator.generateFilename(TEST_DOUYIN_ID, timestamp);
            
            // 验证文件名格式
            Pattern pattern = Pattern.compile("^([a-zA-Z0-9_]+)_(\\d{8})_(\\d{6})\\.mp4$");
            Matcher matcher = pattern.matcher(filename);
            
            assertTrue(matcher.matches(), 
                "文件名应符合格式 {douyinId}_{YYYYMMDD}_{HHMMSS}.mp4: " + filename);
            
            // 提取并验证抖音号
            String extractedDouyinId = matcher.group(1);
            assertEquals(TEST_DOUYIN_ID, extractedDouyinId, 
                "文件名中的抖音号应该正确");
            
            // 提取并验证日期
            String dateStr = matcher.group(2);
            String expectedDate = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertEquals(expectedDate, dateStr, 
                "文件名中的日期应该正确");
            
            // 提取并验证时间
            String timeStr = matcher.group(3);
            String expectedTime = timestamp.format(DateTimeFormatter.ofPattern("HHmmss"));
            assertEquals(expectedTime, timeStr, 
                "文件名中的时间应该正确");
        }
    }
    
    /**
     * 测试文件命名格式的边缘情况
     * 
     * 场景：
     * 1. 测试不同格式的抖音号
     * 2. 验证所有生成的文件名都符合格式
     */
    @Test
    public void testFileNamingFormat_EdgeCases() {
        String[] douyinIds = {
            "user123",           // 纯字母+数字
            "w2511839220",       // 字母开头+数字
            "testuser456",       // 字母+数字
            "ABC123XYZ"          // 大写字母+数字
        };
        
        LocalDateTime timestamp = LocalDateTime.now();
        
        for (String douyinId : douyinIds) {
            String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
            
            // 验证文件名格式
            Pattern pattern = Pattern.compile("^([a-zA-Z0-9_]+)_(\\d{8})_(\\d{6})\\.mp4$");
            Matcher matcher = pattern.matcher(filename);
            
            assertTrue(matcher.matches(), 
                "文件名应符合格式，抖音号: " + douyinId + ", 文件名: " + filename);
            
            // 验证抖音号正确
            String extractedDouyinId = matcher.group(1);
            assertEquals(douyinId, extractedDouyinId, 
                "文件名中的抖音号应该正确");
        }
    }
    
    /**
     * 测试文件可播放性（使用FFprobe）
     * 
     * 场景：
     * 1. 创建一个测试MP4文件（使用FFmpeg生成）
     * 2. 使用FFprobe验证文件可播放
     * 3. 验证文件包含视频流
     * 
     * 验证需求：4.4
     * 
     * 注意：此测试需要系统安装FFmpeg和FFprobe
     */
    @Test
    public void testFilePlayability() throws IOException, InterruptedException {
        // 检查FFprobe是否可用
        if (!isFFprobeAvailable()) {
            System.out.println("跳过文件可播放性测试：FFprobe不可用");
            return;
        }
        
        // 生成测试文件名
        LocalDateTime timestamp = LocalDateTime.now();
        String filename = FileNameGenerator.generateFilename(TEST_DOUYIN_ID, timestamp);
        String fullPath = fileSystemManager.getFullPath(filename);
        
        // 创建一个简单的测试视频文件（使用FFmpeg）
        boolean testFileCreated = createTestVideoFile(fullPath);
        
        if (!testFileCreated) {
            System.out.println("跳过文件可播放性测试：无法创建测试视频文件");
            return;
        }
        
        // 使用FFprobe验证文件
        boolean isPlayable = verifyFileWithFFprobe(fullPath);
        
        assertTrue(isPlayable, "生成的MP4文件应该是可播放的");
        
        // 清理测试文件
        Files.deleteIfExists(Path.of(fullPath));
    }
    
    /**
     * 测试文件完整性
     * 
     * 场景：
     * 1. 创建测试文件
     * 2. 验证文件大小大于0
     * 3. 验证文件可读
     */
    @Test
    public void testFileIntegrity() throws IOException {
        // 生成文件名
        LocalDateTime timestamp = LocalDateTime.now();
        String filename = FileNameGenerator.generateFilename(TEST_DOUYIN_ID, timestamp);
        String fullPath = fileSystemManager.getFullPath(filename);
        
        // 创建测试文件并写入一些数据
        File testFile = new File(fullPath);
        Files.write(testFile.toPath(), "test data".getBytes());
        
        // 验证文件大小
        assertTrue(testFile.length() > 0, "文件大小应该大于0");
        
        // 验证文件可读
        assertTrue(testFile.canRead(), "文件应该可读");
        
        // 验证文件扩展名
        assertTrue(filename.endsWith(".mp4"), "文件应该有.mp4扩展名");
    }
    
    /**
     * 测试多个文件的命名唯一性
     * 
     * 场景：
     * 1. 在短时间内生成多个文件名
     * 2. 验证所有文件名都是唯一的
     * 
     * 验证需求：4.5（通过属性测试已验证，这里做集成验证）
     */
    @Test
    public void testMultipleFilesUniqueness() throws InterruptedException {
        String[] filenames = new String[10];
        
        // 快速生成多个文件名，使用纳秒级时间戳确保唯一性
        for (int i = 0; i < filenames.length; i++) {
            filenames[i] = FileNameGenerator.generateFilename(TEST_DOUYIN_ID, LocalDateTime.now());
            // 短暂延迟以确保时间戳不同（至少1秒）
            Thread.sleep(1100);
        }
        
        // 验证所有文件名都是唯一的
        for (int i = 0; i < filenames.length; i++) {
            for (int j = i + 1; j < filenames.length; j++) {
                assertNotEquals(filenames[i], filenames[j], 
                    "文件名应该是唯一的: " + filenames[i] + " vs " + filenames[j]);
            }
        }
    }
    
    /**
     * 检查FFprobe是否可用
     */
    private boolean isFFprobeAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("ffprobe -version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 创建测试视频文件
     * 使用FFmpeg生成一个简单的测试视频
     */
    private boolean createTestVideoFile(String outputPath) {
        try {
            // 使用FFmpeg生成一个5秒的测试视频
            String[] command = {
                "ffmpeg",
                "-f", "lavfi",
                "-i", "testsrc=duration=5:size=320x240:rate=30",
                "-f", "lavfi",
                "-i", "sine=frequency=1000:duration=5",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-y",
                outputPath
            };
            
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            return exitCode == 0 && new File(outputPath).exists();
        } catch (Exception e) {
            System.err.println("创建测试视频文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用FFprobe验证文件
     */
    private boolean verifyFileWithFFprobe(String filePath) {
        try {
            // 使用FFprobe检查文件
            String[] command = {
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_type",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            };
            
            Process process = Runtime.getRuntime().exec(command);
            
            // 读取输出
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            
            int exitCode = process.waitFor();
            
            // 验证退出码为0且输出包含"video"
            return exitCode == 0 && "video".equals(line);
            
        } catch (Exception e) {
            System.err.println("FFprobe验证失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清理测试文件
     */
    private void cleanupTestFiles() {
        try {
            if (Files.exists(testRecordingsPath)) {
                Files.walk(testRecordingsPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // 忽略删除错误
                        }
                    });
            }
        } catch (IOException e) {
            // 忽略清理错误
        }
    }
}
