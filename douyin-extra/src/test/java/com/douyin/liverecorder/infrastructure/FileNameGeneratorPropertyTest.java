package com.douyin.liverecorder.infrastructure;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.time.api.DateTimes;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件名生成器的属性测试
 * Feature: douyin-live-recorder, Property 9: 文件命名格式符合规范
 * 验证需求：5.2
 */
class FileNameGeneratorPropertyTest {

    /**
     * 属性 9: 文件命名格式符合规范
     * 对于任何生成的录制文件，文件名应该符合格式 {douyinId}_{YYYYMMDD}_{HHMMSS}.mp4，
     * 并且包含正确的抖音号和时间戳。
     */
    @Property(tries = 100)
    @Label("生成的文件名应该符合格式规范")
    void generatedFilenameShouldFollowFormat(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp) {
        
        // 生成文件名
        String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        // 验证文件名格式正确
        assertThat(FileNameGenerator.isValidFilename(filename))
                .as("生成的文件名 '%s' 应该符合格式规范", filename)
                .isTrue();
        
        // 验证文件名以.mp4结尾
        assertThat(filename)
                .as("文件名应该以.mp4结尾")
                .endsWith(".mp4");
        
        // 验证文件名包含三个下划线分隔的部分
        String nameWithoutExt = filename.substring(0, filename.length() - 4);
        String[] parts = nameWithoutExt.split("_");
        assertThat(parts)
                .as("文件名应该包含三个下划线分隔的部分")
                .hasSize(3);
    }

    @Property(tries = 100)
    @Label("从生成的文件名中应该能正确解析出抖音号")
    void shouldParseDouyinIdFromGeneratedFilename(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp) {
        
        // 生成文件名
        String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        // 解析抖音号
        String parsedDouyinId = FileNameGenerator.parseDouyinId(filename);
        
        // 验证解析出的抖音号与原始抖音号相同
        assertThat(parsedDouyinId)
                .as("从文件名 '%s' 解析出的抖音号应该与原始抖音号相同", filename)
                .isEqualTo(douyinId);
    }

    @Property(tries = 100)
    @Label("从生成的文件名中应该能正确解析出日期")
    void shouldParseDateFromGeneratedFilename(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp) {
        
        // 生成文件名
        String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        // 解析日期
        String parsedDate = FileNameGenerator.parseDate(filename);
        
        // 验证日期格式正确（8位数字）
        assertThat(parsedDate)
                .as("解析出的日期应该是8位数字")
                .matches("\\d{8}");
        
        // 验证日期值正确
        String expectedDate = String.format("%04d%02d%02d", 
                timestamp.getYear(), 
                timestamp.getMonthValue(), 
                timestamp.getDayOfMonth());
        assertThat(parsedDate)
                .as("从文件名 '%s' 解析出的日期应该与时间戳匹配", filename)
                .isEqualTo(expectedDate);
    }

    @Property(tries = 100)
    @Label("从生成的文件名中应该能正确解析出时间")
    void shouldParseTimeFromGeneratedFilename(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp) {
        
        // 生成文件名
        String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        // 解析时间
        String parsedTime = FileNameGenerator.parseTime(filename);
        
        // 验证时间格式正确（6位数字）
        assertThat(parsedTime)
                .as("解析出的时间应该是6位数字")
                .matches("\\d{6}");
        
        // 验证时间值正确
        String expectedTime = String.format("%02d%02d%02d", 
                timestamp.getHour(), 
                timestamp.getMinute(), 
                timestamp.getSecond());
        assertThat(parsedTime)
                .as("从文件名 '%s' 解析出的时间应该与时间戳匹配", filename)
                .isEqualTo(expectedTime);
    }

    @Property(tries = 100)
    @Label("不同时间戳生成的文件名应该是唯一的")
    void differentTimestampsShouldGenerateUniqueFilenames(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp1,
            @ForAll("timestamps") LocalDateTime timestamp2) {
        
        Assume.that(!timestamp1.equals(timestamp2));
        
        // 生成两个文件名
        String filename1 = FileNameGenerator.generateFilename(douyinId, timestamp1);
        String filename2 = FileNameGenerator.generateFilename(douyinId, timestamp2);
        
        // 验证文件名不同
        assertThat(filename1)
                .as("不同时间戳应该生成不同的文件名")
                .isNotEqualTo(filename2);
    }

    @Property(tries = 100)
    @Label("相同输入应该生成相同的文件名（幂等性）")
    void sameInputShouldGenerateSameFilename(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp) {
        
        // 多次生成文件名
        String filename1 = FileNameGenerator.generateFilename(douyinId, timestamp);
        String filename2 = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        // 验证文件名相同
        assertThat(filename1)
                .as("相同输入应该生成相同的文件名")
                .isEqualTo(filename2);
    }

    @Property(tries = 100)
    @Label("生成的文件名不应该包含路径分隔符")
    void generatedFilenameShouldNotContainPathSeparators(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId,
            @ForAll("timestamps") LocalDateTime timestamp) {
        
        // 生成文件名
        String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        // 验证不包含路径分隔符
        assertThat(filename)
                .as("文件名不应该包含路径分隔符")
                .doesNotContain("/")
                .doesNotContain("\\");
    }

    @Property(tries = 100)
    @Label("无效格式的文件名应该被识别为无效")
    void invalidFormatShouldBeRejected(@ForAll("invalidFilenames") String invalidFilename) {
        // 验证无效文件名被正确识别
        boolean isValid = FileNameGenerator.isValidFilename(invalidFilename);
        assertThat(isValid)
                .as("无效格式的文件名 '%s' 应该被识别为无效", invalidFilename)
                .isFalse();
    }

    // 自定义生成器：生成时间戳
    @Provide
    Arbitrary<LocalDateTime> timestamps() {
        return DateTimes.dateTimes()
                .between(
                        LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                        LocalDateTime.of(2030, 12, 31, 23, 59, 59)
                );
    }

    // 自定义生成器：生成无效的文件名
    @Provide
    Arbitrary<String> invalidFilenames() {
        return Arbitraries.oneOf(
                // 没有扩展名
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(20),
                // 错误的扩展名
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(20)
                        .map(s -> s + ".txt"),
                // 缺少下划线
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(20)
                        .map(s -> s + ".mp4"),
                // 只有一个下划线
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "_20260103.mp4"),
                // 日期格式错误
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "_2026010_143025.mp4"),
                // 时间格式错误
                Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "_20260103_14302.mp4"),
                // 空字符串
                Arbitraries.just(""),
                // 只有扩展名
                Arbitraries.just(".mp4")
        );
    }

    // 示例测试：验证已知的有效文件名
    @Example
    @Label("已知有效的文件名示例应该通过验证")
    void knownValidFilenamesShouldPass() {
        assertThat(FileNameGenerator.isValidFilename("w2511839220_20260103_143025.mp4")).isTrue();
        assertThat(FileNameGenerator.isValidFilename("user123_20250101_000000.mp4")).isTrue();
        assertThat(FileNameGenerator.isValidFilename("test456abc_20291231_235959.mp4")).isTrue();
    }

    // 示例测试：验证已知的无效文件名
    @Example
    @Label("已知无效的文件名示例应该被拒绝")
    void knownInvalidFilenamesShouldFail() {
        assertThat(FileNameGenerator.isValidFilename("")).isFalse();
        assertThat(FileNameGenerator.isValidFilename(null)).isFalse();
        assertThat(FileNameGenerator.isValidFilename("test.mp4")).isFalse();
        assertThat(FileNameGenerator.isValidFilename("test_20260103.mp4")).isFalse();
        assertThat(FileNameGenerator.isValidFilename("test_2026010_143025.mp4")).isFalse();
        assertThat(FileNameGenerator.isValidFilename("test_20260103_14302.mp4")).isFalse();
        assertThat(FileNameGenerator.isValidFilename("test_20260103_143025.txt")).isFalse();
    }

    // 示例测试：验证文件名生成和解析的往返一致性
    @Example
    @Label("文件名生成和解析应该保持一致性")
    void filenameGenerationAndParsingRoundTrip() {
        String douyinId = "w2511839220";
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 3, 14, 30, 25);
        
        String filename = FileNameGenerator.generateFilename(douyinId, timestamp);
        
        assertThat(filename).isEqualTo("w2511839220_20260103_143025.mp4");
        assertThat(FileNameGenerator.parseDouyinId(filename)).isEqualTo("w2511839220");
        assertThat(FileNameGenerator.parseDate(filename)).isEqualTo("20260103");
        assertThat(FileNameGenerator.parseTime(filename)).isEqualTo("143025");
    }
}
