package com.douyin.liverecorder.exception;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误消息本地化的属性测试
 * Feature: douyin-live-recorder, Property 11: 错误消息本地化
 * 验证需求：7.4
 */
class ErrorMessageLocalizationPropertyTest {

    /**
     * 属性 11: 错误消息本地化
     * 对于任何错误情况，系统返回的错误消息应该是清晰的中文文本，而不是英文或错误代码。
     */
    @Property(tries = 100)
    @Label("InvalidDouyinIdException的错误消息应该是中文")
    void invalidDouyinIdExceptionShouldHaveChineseMessage(
            @ForAll @StringLength(min = 1, max = 50) String douyinId) {
        // 对于任何抖音号，InvalidDouyinIdException的消息应该包含中文
        InvalidDouyinIdException exception = new InvalidDouyinIdException(douyinId);
        String message = exception.getMessage();
        
        assertThat(message)
                .as("错误消息应该不为空")
                .isNotNull()
                .isNotEmpty();
        
        assertThat(containsChinese(message))
                .as("错误消息 '%s' 应该包含中文字符", message)
                .isTrue();
        
        assertThat(message)
                .as("错误消息应该包含'抖音号'")
                .contains("抖音号");
    }

    @Property(tries = 100)
    @Label("LiveStreamNotFoundException的错误消息应该是中文")
    void liveStreamNotFoundExceptionShouldHaveChineseMessage(
            @ForAll @StringLength(min = 1, max = 50) String douyinId) {
        // 对于任何抖音号，LiveStreamNotFoundException的消息应该包含中文
        LiveStreamNotFoundException exception = new LiveStreamNotFoundException(douyinId);
        String message = exception.getMessage();
        
        assertThat(message)
                .as("错误消息应该不为空")
                .isNotNull()
                .isNotEmpty();
        
        assertThat(containsChinese(message))
                .as("错误消息 '%s' 应该包含中文字符", message)
                .isTrue();
        
        assertThat(message)
                .as("错误消息应该包含'直播'")
                .contains("直播");
    }

    @Property(tries = 100)
    @Label("NetworkException的错误消息应该是中文")
    void networkExceptionShouldHaveChineseMessage(
            @ForAll("chineseErrorMessages") String errorMessage) {
        // 对于任何中文错误消息，NetworkException应该保持中文
        NetworkException exception = new NetworkException(errorMessage);
        String message = exception.getMessage();
        
        assertThat(message)
                .as("错误消息应该不为空")
                .isNotNull()
                .isNotEmpty();
        
        assertThat(containsChinese(message))
                .as("错误消息 '%s' 应该包含中文字符", message)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("RecordingException的错误消息应该是中文")
    void recordingExceptionShouldHaveChineseMessage(
            @ForAll("chineseErrorMessages") String errorMessage) {
        // 对于任何中文错误消息，RecordingException应该保持中文
        RecordingException exception = new RecordingException(errorMessage);
        String message = exception.getMessage();
        
        assertThat(message)
                .as("错误消息应该不为空")
                .isNotNull()
                .isNotEmpty();
        
        assertThat(containsChinese(message))
                .as("错误消息 '%s' 应该包含中文字符", message)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("ConcurrentLimitException的错误消息应该是中文")
    void concurrentLimitExceptionShouldHaveChineseMessage(
            @ForAll @IntRange(min = 1, max = 100) int currentCount,
            @ForAll @IntRange(min = 1, max = 100) int maxLimit) {
        // 对于任何并发数量，ConcurrentLimitException的消息应该包含中文
        ConcurrentLimitException exception = new ConcurrentLimitException(currentCount, maxLimit);
        String message = exception.getMessage();
        
        assertThat(message)
                .as("错误消息应该不为空")
                .isNotNull()
                .isNotEmpty();
        
        assertThat(containsChinese(message))
                .as("错误消息 '%s' 应该包含中文字符", message)
                .isTrue();
        
        assertThat(message)
                .as("错误消息应该包含'录制'或'任务'")
                .containsAnyOf("录制", "任务");
    }

    @Property(tries = 100)
    @Label("所有异常的错误消息都不应该包含纯英文错误代码")
    void exceptionMessagesShouldNotContainEnglishErrorCodes(
            @ForAll @StringLength(min = 1, max = 50) String douyinId) {
        // 测试各种异常，确保不包含常见的英文错误代码模式
        InvalidDouyinIdException ex1 = new InvalidDouyinIdException(douyinId);
        LiveStreamNotFoundException ex2 = new LiveStreamNotFoundException(douyinId);
        
        String[] messages = {ex1.getMessage(), ex2.getMessage()};
        
        for (String message : messages) {
            // 错误消息不应该以常见的英文错误代码开头
            assertThat(message)
                    .as("错误消息不应该以ERROR开头")
                    .doesNotStartWith("ERROR");
            
            assertThat(message)
                    .as("错误消息不应该以Exception开头")
                    .doesNotStartWith("Exception");
            
            // 错误消息应该包含中文
            assertThat(containsChinese(message))
                    .as("错误消息应该包含中文字符")
                    .isTrue();
        }
    }

    // 自定义生成器：生成中文错误消息
    @Provide
    Arbitrary<String> chineseErrorMessages() {
        return Arbitraries.of(
            "网络连接失败",
            "录制过程中发生错误",
            "磁盘空间不足",
            "直播流中断",
            "无法访问服务器",
            "请求超时",
            "文件写入失败",
            "系统资源不足"
        );
    }

    /**
     * 检查字符串是否包含中文字符
     */
    private boolean containsChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        for (char c : str.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    // 示例测试：验证已知的错误消息格式
    @Example
    @Label("已知的异常应该有正确的中文错误消息")
    void knownExceptionsShouldHaveCorrectChineseMessages() {
        InvalidDouyinIdException ex1 = new InvalidDouyinIdException("test123");
        assertThat(ex1.getMessage()).contains("抖音号");
        assertThat(containsChinese(ex1.getMessage())).isTrue();
        
        LiveStreamNotFoundException ex2 = new LiveStreamNotFoundException("test123");
        assertThat(ex2.getMessage()).contains("直播");
        assertThat(containsChinese(ex2.getMessage())).isTrue();
        
        ConcurrentLimitException ex3 = new ConcurrentLimitException(5, 5);
        assertThat(ex3.getMessage()).containsAnyOf("录制", "任务", "上限");
        assertThat(containsChinese(ex3.getMessage())).isTrue();
        
        NetworkException ex4 = new NetworkException("网络连接失败");
        assertThat(containsChinese(ex4.getMessage())).isTrue();
        
        RecordingException ex5 = new RecordingException("录制失败");
        assertThat(containsChinese(ex5.getMessage())).isTrue();
    }
}
