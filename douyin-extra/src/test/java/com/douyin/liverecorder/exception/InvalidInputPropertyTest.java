package com.douyin.liverecorder.exception;

import com.douyin.liverecorder.validation.DouyinIdValidator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 无效输入返回错误的属性测试
 * Feature: douyin-live-recorder, Property 2: 无效输入返回错误
 * 验证需求：1.2
 */
class InvalidInputPropertyTest {

    /**
     * 属性 2: 无效输入返回错误
     * 对于任何无效的抖音号输入，系统应该返回明确的错误信息，而不是崩溃或返回空值。
     */
    @Property(tries = 100)
    @Label("无效的抖音号应该被验证器拒绝并返回false")
    void invalidDouyinIdsShouldBeRejectedByValidator(
            @ForAll("invalidDouyinIds") String invalidDouyinId) {
        // 对于任何无效的抖音号，验证器应该返回false（不应该崩溃）
        boolean result = DouyinIdValidator.isValidDouyinId(invalidDouyinId);
        
        assertThat(result)
                .as("无效的抖音号 '%s' 应该被拒绝", invalidDouyinId)
                .isFalse();
    }

    @Property(tries = 100)
    @Label("InvalidDouyinIdException应该包含明确的错误信息")
    void invalidDouyinIdExceptionShouldHaveClearMessage(
            @ForAll("invalidDouyinIds") String invalidDouyinId) {
        // 对于任何无效的抖音号，异常应该包含明确的错误信息
        InvalidDouyinIdException exception = new InvalidDouyinIdException(invalidDouyinId);
        
        assertThat(exception.getMessage())
                .as("异常消息不应该为null")
                .isNotNull();
        
        assertThat(exception.getMessage())
                .as("异常消息不应该为空")
                .isNotEmpty();
        
        assertThat(exception.getMessage())
                .as("异常消息应该包含'抖音号'")
                .contains("抖音号");
        
        assertThat(exception.getDouyinId())
                .as("异常应该保存抖音号")
                .isEqualTo(invalidDouyinId);
    }

    @Property(tries = 100)
    @Label("包含特殊字符的输入应该返回错误而不是崩溃")
    void specialCharacterInputsShouldReturnErrorNotCrash(
            @ForAll("stringsWithSpecialChars") String specialInput) {
        // 对于包含特殊字符的输入，系统应该正常处理（不崩溃）
        try {
            boolean result = DouyinIdValidator.isValidDouyinId(specialInput);
            assertThat(result)
                    .as("包含特殊字符的输入应该被拒绝")
                    .isFalse();
            
            // 创建异常也不应该崩溃
            InvalidDouyinIdException exception = new InvalidDouyinIdException(specialInput);
            assertThat(exception.getMessage())
                    .as("异常消息应该存在")
                    .isNotNull()
                    .isNotEmpty();
        } catch (Exception e) {
            // 不应该抛出未预期的异常
            throw new AssertionError("处理特殊字符输入时不应该崩溃: " + specialInput, e);
        }
    }

    @Property(tries = 100)
    @Label("null或空字符串应该返回错误而不是崩溃")
    void nullOrEmptyInputsShouldReturnErrorNotCrash(
            @ForAll("nullOrEmptyStrings") String emptyInput) {
        // 对于null或空字符串，系统应该正常处理（不崩溃）
        try {
            boolean result = DouyinIdValidator.isValidDouyinId(emptyInput);
            assertThat(result)
                    .as("null或空字符串应该被拒绝")
                    .isFalse();
        } catch (NullPointerException e) {
            // 不应该抛出NullPointerException
            throw new AssertionError("处理null输入时不应该抛出NullPointerException", e);
        } catch (Exception e) {
            // 不应该抛出其他未预期的异常
            throw new AssertionError("处理空输入时不应该崩溃", e);
        }
    }

    @Property(tries = 100)
    @Label("超长字符串应该返回错误而不是崩溃")
    void veryLongInputsShouldReturnErrorNotCrash(
            @ForAll @StringLength(min = 51, max = 1000) String longInput) {
        // 对于超长字符串，系统应该正常处理（不崩溃）
        try {
            boolean result = DouyinIdValidator.isValidDouyinId(longInput);
            assertThat(result)
                    .as("超长字符串应该被拒绝")
                    .isFalse();
            
            // 创建异常也不应该崩溃
            InvalidDouyinIdException exception = new InvalidDouyinIdException(longInput);
            assertThat(exception.getMessage())
                    .as("异常消息应该存在")
                    .isNotNull()
                    .isNotEmpty();
        } catch (Exception e) {
            // 不应该抛出未预期的异常
            throw new AssertionError("处理超长输入时不应该崩溃: 长度=" + longInput.length(), e);
        }
    }

    @Property(tries = 100)
    @Label("包含Unicode字符的输入应该返回错误而不是崩溃")
    void unicodeInputsShouldReturnErrorNotCrash(
            @ForAll("stringsWithUnicode") String unicodeInput) {
        // 对于包含Unicode字符的输入，系统应该正常处理（不崩溃）
        try {
            boolean result = DouyinIdValidator.isValidDouyinId(unicodeInput);
            assertThat(result)
                    .as("包含Unicode字符的输入应该被拒绝")
                    .isFalse();
            
            // 创建异常也不应该崩溃
            InvalidDouyinIdException exception = new InvalidDouyinIdException(unicodeInput);
            assertThat(exception.getMessage())
                    .as("异常消息应该存在")
                    .isNotNull()
                    .isNotEmpty();
        } catch (Exception e) {
            // 不应该抛出未预期的异常
            throw new AssertionError("处理Unicode输入时不应该崩溃: " + unicodeInput, e);
        }
    }

    // 自定义生成器：生成各种无效的抖音号
    @Provide
    Arbitrary<String> invalidDouyinIds() {
        return Arbitraries.oneOf(
            // 空字符串和null
            Arbitraries.of("", null),
            // 包含特殊字符
            Arbitraries.strings().withCharRange('!', '/').ofMinLength(1).ofMaxLength(20),
            // 包含空格
            Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(10)
                .map(s -> s + " " + s),
            // 包含中文
            Arbitraries.strings().withCharRange('\u4e00', '\u9fa5').ofMinLength(1).ofMaxLength(10),
            // 超长字符串
            Arbitraries.strings().alpha().numeric().ofMinLength(51).ofMaxLength(100),
            // 包含下划线、连字符等
            Arbitraries.of("user_123", "user-123", "user.123", "user@123", "user#123")
        );
    }

    // 自定义生成器：生成包含特殊字符的字符串
    @Provide
    Arbitrary<String> stringsWithSpecialChars() {
        return Arbitraries.strings()
            .withChars('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '=', '+', '[', ']', '{', '}', '|', '\\', ';', ':', '\'', '"', ',', '.', '<', '>', '/', '?')
            .ofMinLength(1)
            .ofMaxLength(20);
    }

    // 自定义生成器：生成null或空字符串
    @Provide
    Arbitrary<String> nullOrEmptyStrings() {
        return Arbitraries.of("", null, "   ", "\t", "\n");
    }

    // 自定义生成器：生成包含Unicode字符的字符串
    @Provide
    Arbitrary<String> stringsWithUnicode() {
        return Arbitraries.oneOf(
            // 中文字符
            Arbitraries.strings().withCharRange('\u4e00', '\u9fa5').ofMinLength(1).ofMaxLength(10),
            // 日文字符
            Arbitraries.strings().withCharRange('\u3040', '\u309f').ofMinLength(1).ofMaxLength(10),
            // 韩文字符
            Arbitraries.strings().withCharRange('\uac00', '\ud7af').ofMinLength(1).ofMaxLength(10),
            // Emoji
            Arbitraries.of("😀", "🎉", "❤️", "🚀", "⭐")
        );
    }

    // 示例测试：验证已知的无效输入
    @Example
    @Label("已知的无效输入应该返回明确的错误")
    void knownInvalidInputsShouldReturnClearErrors() {
        String[] invalidInputs = {
            "", null, "@user", "user name", "用户123", 
            "user-123", "user_123", "user.123", "😀", "   "
        };
        
        for (String input : invalidInputs) {
            // 验证器应该拒绝
            boolean result = DouyinIdValidator.isValidDouyinId(input);
            assertThat(result)
                    .as("输入 '%s' 应该被拒绝", input)
                    .isFalse();
            
            // 异常应该包含明确的错误信息（除了null）
            if (input != null) {
                InvalidDouyinIdException exception = new InvalidDouyinIdException(input);
                assertThat(exception.getMessage())
                        .as("异常消息应该不为空")
                        .isNotNull()
                        .isNotEmpty();
            }
        }
    }
}
