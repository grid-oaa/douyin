package com.douyin.liverecorder.validation;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 抖音号验证器的属性测试
 * Feature: douyin-live-recorder, Property 1: 抖音号格式验证正确性
 * 验证需求：1.1, 1.3
 */
class DouyinIdValidatorPropertyTest {

    /**
     * 属性 1: 抖音号格式验证正确性
     * 对于任何输入字符串，验证函数应该正确识别有效的抖音号格式（字母和数字组合），并拒绝无效格式。
     */
    @Property(tries = 100)
    @Label("有效的抖音号（字母和数字组合）应该通过验证")
    void validDouyinIdsShouldPass(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 1, max = 50) String douyinId) {
        // 对于任何由字母和数字组成的字符串（长度1-50），验证应该通过
        boolean result = DouyinIdValidator.isValidDouyinId(douyinId);
        assertThat(result)
                .as("抖音号 '%s' 应该是有效的", douyinId)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("包含特殊字符的字符串应该被拒绝")
    void douyinIdsWithSpecialCharactersShouldFail(
            @ForAll("stringsWithSpecialChars") String invalidDouyinId) {
        // 对于任何包含特殊字符的字符串，验证应该失败
        boolean result = DouyinIdValidator.isValidDouyinId(invalidDouyinId);
        assertThat(result)
                .as("包含特殊字符的抖音号 '%s' 应该是无效的", invalidDouyinId)
                .isFalse();
    }

    @Property(tries = 100)
    @Label("空字符串或null应该被拒绝")
    void emptyOrNullDouyinIdsShouldFail(@ForAll("emptyOrNullStrings") String invalidDouyinId) {
        // 对于空字符串或null，验证应该失败
        boolean result = DouyinIdValidator.isValidDouyinId(invalidDouyinId);
        assertThat(result)
                .as("空或null的抖音号应该是无效的")
                .isFalse();
    }

    @Property(tries = 100)
    @Label("过长的字符串应该被拒绝")
    void tooLongDouyinIdsShouldFail(
            @ForAll @AlphaChars @NumericChars @StringLength(min = 51, max = 100) String longDouyinId) {
        // 对于长度超过50的字符串，验证应该失败
        boolean result = DouyinIdValidator.isValidDouyinId(longDouyinId);
        assertThat(result)
                .as("过长的抖音号 (长度=%d) 应该是无效的", longDouyinId.length())
                .isFalse();
    }

    @Property(tries = 100)
    @Label("包含空格的字符串应该被拒绝")
    void douyinIdsWithSpacesShouldFail(@ForAll("stringsWithSpaces") String invalidDouyinId) {
        // 对于包含空格的字符串，验证应该失败
        boolean result = DouyinIdValidator.isValidDouyinId(invalidDouyinId);
        assertThat(result)
                .as("包含空格的抖音号 '%s' 应该是无效的", invalidDouyinId)
                .isFalse();
    }

    @Property(tries = 100)
    @Label("包含中文字符的字符串应该被拒绝")
    void douyinIdsWithChineseCharactersShouldFail(
            @ForAll("stringsWithChinese") String invalidDouyinId) {
        // 对于包含中文字符的字符串，验证应该失败
        boolean result = DouyinIdValidator.isValidDouyinId(invalidDouyinId);
        assertThat(result)
                .as("包含中文字符的抖音号 '%s' 应该是无效的", invalidDouyinId)
                .isFalse();
    }

    // 自定义生成器：生成包含特殊字符的字符串
    @Provide
    Arbitrary<String> stringsWithSpecialChars() {
        return Arbitraries.strings()
                .withCharRange('!', '/')  // 特殊字符
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    // 自定义生成器：生成空字符串或null
    @Provide
    Arbitrary<String> emptyOrNullStrings() {
        return Arbitraries.of("", null);
    }

    // 自定义生成器：生成包含空格的字符串
    @Provide
    Arbitrary<String> stringsWithSpaces() {
        Arbitrary<String> alphanumeric = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(10);
        
        return alphanumeric.map(s -> {
            // 在字符串中随机位置插入空格
            int pos = s.length() > 0 ? s.length() / 2 : 0;
            return s.substring(0, pos) + " " + s.substring(pos);
        });
    }

    // 自定义生成器：生成包含中文字符的字符串
    @Provide
    Arbitrary<String> stringsWithChinese() {
        Arbitrary<String> chineseChars = Arbitraries.strings()
                .withCharRange('\u4e00', '\u9fa5')  // 中文字符范围
                .ofMinLength(1)
                .ofMaxLength(10);
        
        Arbitrary<String> alphanumeric = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(0)
                .ofMaxLength(10);
        
        // 组合中文和字母数字
        return Combinators.combine(chineseChars, alphanumeric)
                .as((chinese, alphanum) -> chinese + alphanum);
    }

    // 示例测试：验证已知的有效抖音号
    @Example
    @Label("已知有效的抖音号示例应该通过验证")
    void knownValidDouyinIdsShouldPass() {
        assertThat(DouyinIdValidator.isValidDouyinId("w2511839220")).isTrue();
        assertThat(DouyinIdValidator.isValidDouyinId("user123")).isTrue();
        assertThat(DouyinIdValidator.isValidDouyinId("test456abc")).isTrue();
        assertThat(DouyinIdValidator.isValidDouyinId("ABC123")).isTrue();
        assertThat(DouyinIdValidator.isValidDouyinId("a")).isTrue();
        assertThat(DouyinIdValidator.isValidDouyinId("1")).isTrue();
    }

    // 示例测试：验证已知的无效抖音号
    @Example
    @Label("已知无效的抖音号示例应该被拒绝")
    void knownInvalidDouyinIdsShouldFail() {
        assertThat(DouyinIdValidator.isValidDouyinId("")).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId(null)).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId("@user")).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId("user name")).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId("用户123")).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId("user-123")).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId("user_123")).isFalse();
        assertThat(DouyinIdValidator.isValidDouyinId("user.123")).isFalse();
    }
}
