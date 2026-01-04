package com.douyin.liverecorder.infrastructure;

import net.jqwik.api.*;
import net.jqwik.time.api.DateTimes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件名唯一性属性测试
 * Feature: douyin-live-recorder, Property 7: 文件名唯一性
 * 验证需求：4.5
 */
class FileNameUniquenessPropertyTest {
    
    /**
     * 属性 7: 文件名唯一性
     * 对于任何两个不同的录制任务，生成的文件名应该是唯一的，即使它们录制相同的抖音号
     */
    @Property(tries = 100)
    @Label("属性 7: 文件名唯一性 - 不同时间戳生成不同文件名")
    void differentTimestampsGenerateDifferentFilenames(
            @ForAll("validDouyinId") String douyinId,
            @ForAll("dateTime") LocalDateTime timestamp1,
            @ForAll("dateTime") LocalDateTime timestamp2) {
        
        // 如果时间戳相同，跳过此测试
        Assume.that(!timestamp1.equals(timestamp2));
        
        // 生成文件名
        String filename1 = FileNameGenerator.generateFilename(douyinId, timestamp1);
        String filename2 = FileNameGenerator.generateFilename(douyinId, timestamp2);
        
        // 验证文件名不同
        assertNotEquals(filename1, filename2, 
            "相同抖音号但不同时间戳应该生成不同的文件名");
    }
    
    /**
     * 属性 7: 文件名唯一性
     * 对于任何一组录制任务，所有生成的文件名都应该是唯一的
     */
    @Property(tries = 100)
    @Label("属性 7: 文件名唯一性 - 多个任务生成唯一文件名")
    void multipleTasksGenerateUniqueFilenames(
            @ForAll("validDouyinId") String douyinId,
            @ForAll("dateTimeList") java.util.List<LocalDateTime> timestamps) {
        
        // 至少需要2个时间戳
        Assume.that(timestamps.size() >= 2);
        
        Set<String> filenames = new HashSet<>();
        Set<LocalDateTime> uniqueTimestamps = new HashSet<>();
        
        // 为每个时间戳生成文件名
        for (LocalDateTime timestamp : timestamps) {
            // 只处理唯一的时间戳
            if (uniqueTimestamps.add(timestamp)) {
                String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
                
                // 验证文件名是唯一的
                assertTrue(filenames.add(filename), 
                    "生成的文件名应该是唯一的: " + filename);
            }
        }
        
        // 验证生成的文件名数量等于唯一时间戳数量
        assertEquals(uniqueTimestamps.size(), filenames.size(),
            "唯一时间戳数量应该等于唯一文件名数量");
    }
    
    /**
     * 属性 7: 文件名唯一性
     * 即使是相同的抖音号，在不同时刻录制也应该生成不同的文件名
     */
    @Property(tries = 100)
    @Label("属性 7: 文件名唯一性 - 相同抖音号不同时间生成唯一文件名")
    void sameDouyinIdDifferentTimesGenerateUniqueFilenames(
            @ForAll("validDouyinId") String douyinId,
            @ForAll("dateTime") LocalDateTime baseTime) {
        
        Set<String> filenames = new HashSet<>();
        
        // 生成10个不同时间点的文件名（每秒递增）
        for (int i = 0; i < 10; i++) {
            LocalDateTime timestamp = baseTime.plusSeconds(i);
            String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
            
            // 验证每个文件名都是唯一的
            assertTrue(filenames.add(filename), 
                "第 " + i + " 个文件名应该是唯一的: " + filename);
        }
        
        // 验证生成了10个不同的文件名
        assertEquals(10, filenames.size(), "应该生成10个唯一的文件名");
    }
    
    /**
     * 属性 7: 文件名唯一性
     * 不同的抖音号在相同时间也应该生成不同的文件名
     */
    @Property(tries = 100)
    @Label("属性 7: 文件名唯一性 - 不同抖音号相同时间生成不同文件名")
    void differentDouyinIdsSameTimeGenerateDifferentFilenames(
            @ForAll("validDouyinId") String douyinId1,
            @ForAll("validDouyinId") String douyinId2,
            @ForAll("dateTime") LocalDateTime timestamp) {
        
        // 如果抖音号相同，跳过此测试
        Assume.that(!douyinId1.equals(douyinId2));
        
        // 生成文件名
        String filename1 = FileNameGenerator.generateFilename(douyinId1, timestamp);
        String filename2 = FileNameGenerator.generateFilename(douyinId2, timestamp);
        
        // 验证文件名不同
        assertNotEquals(filename1, filename2, 
            "不同抖音号即使在相同时间也应该生成不同的文件名");
    }
    
    // ========== 数据生成器 ==========
    
    @Provide
    Arbitrary<String> validDouyinId() {
        // 生成有效的抖音号：字母和数字组合，长度3-20
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(10)
                .flatMap(letters -> 
                    Arbitraries.strings()
                        .numeric()
                        .ofMinLength(1)
                        .ofMaxLength(10)
                        .map(numbers -> letters + numbers)
                );
    }
    
    @Provide
    Arbitrary<LocalDateTime> dateTime() {
        // 生成2020-2030年之间的日期时间
        return DateTimes.dateTimes()
                .between(
                    LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                    LocalDateTime.of(2030, 12, 31, 23, 59, 59)
                );
    }
    
    @Provide
    Arbitrary<java.util.List<LocalDateTime>> dateTimeList() {
        // 生成2-20个日期时间的列表
        return dateTime().list().ofMinSize(2).ofMaxSize(20);
    }
}
