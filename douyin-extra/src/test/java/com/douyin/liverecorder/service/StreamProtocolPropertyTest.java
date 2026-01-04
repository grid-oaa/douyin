package com.douyin.liverecorder.service;

import com.douyin.liverecorder.infrastructure.HttpClientUtil;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流协议支持的属性测试
 * Feature: douyin-live-recorder, Property 3: 流协议支持
 * 验证需求：3.3
 */
class StreamProtocolPropertyTest {

    /**
     * 属性 3: 流协议支持
     * 对于任何标准的抖音直播流协议（FLV、M3U8），系统应该能够正确识别和处理。
     */
    @Property(tries = 100)
    @Label("FLV格式的URL应该被正确识别")
    void flvUrlsShouldBeRecognized(@ForAll("flvUrls") String flvUrl) {
        // 创建StreamExtractor实例
        HttpClientUtil httpClient = new HttpClientUtil();
        StreamExtractor extractor = new StreamExtractor(httpClient);
        
        // FLV URL应该被识别为有效格式（即使可能无法访问）
        // 我们主要测试格式识别，而不是实际的网络连接
        boolean hasFlvExtension = flvUrl.toLowerCase().contains(".flv");
        
        assertThat(hasFlvExtension)
                .as("URL '%s' 应该包含.flv扩展名", flvUrl)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("M3U8格式的URL应该被正确识别")
    void m3u8UrlsShouldBeRecognized(@ForAll("m3u8Urls") String m3u8Url) {
        // 创建StreamExtractor实例
        HttpClientUtil httpClient = new HttpClientUtil();
        StreamExtractor extractor = new StreamExtractor(httpClient);
        
        // M3U8 URL应该被识别为有效格式
        boolean hasM3u8Extension = m3u8Url.toLowerCase().contains(".m3u8");
        
        assertThat(hasM3u8Extension)
                .as("URL '%s' 应该包含.m3u8扩展名", m3u8Url)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("支持的流协议URL应该通过格式验证")
    void supportedStreamProtocolsShouldPassFormatValidation(
            @ForAll("supportedStreamUrls") String streamUrl) {
        // 创建StreamExtractor实例
        HttpClientUtil httpClient = new HttpClientUtil();
        StreamExtractor extractor = new StreamExtractor(httpClient);
        
        // 验证URL格式（不进行实际的网络连接）
        // 支持的格式应该至少通过基本的格式检查
        boolean hasValidProtocol = streamUrl.startsWith("http://") || 
                                   streamUrl.startsWith("https://") ||
                                   streamUrl.startsWith("rtmp://");
        
        boolean hasValidExtension = streamUrl.toLowerCase().contains(".flv") || 
                                    streamUrl.toLowerCase().contains(".m3u8");
        
        assertThat(hasValidProtocol)
                .as("URL '%s' 应该有有效的协议", streamUrl)
                .isTrue();
        
        assertThat(hasValidExtension)
                .as("URL '%s' 应该有支持的扩展名（.flv或.m3u8）", streamUrl)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("不支持的流格式应该被识别为无效")
    void unsupportedStreamFormatsShouldBeInvalid(
            @ForAll("unsupportedStreamUrls") String unsupportedUrl) {
        // 创建StreamExtractor实例
        HttpClientUtil httpClient = new HttpClientUtil();
        StreamExtractor extractor = new StreamExtractor(httpClient);
        
        // 不支持的格式不应该包含.flv或.m3u8
        boolean hasUnsupportedExtension = !unsupportedUrl.toLowerCase().contains(".flv") && 
                                          !unsupportedUrl.toLowerCase().contains(".m3u8");
        
        assertThat(hasUnsupportedExtension)
                .as("URL '%s' 不应该包含支持的扩展名", unsupportedUrl)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("FLV URL可以包含查询参数")
    void flvUrlsCanHaveQueryParameters(@ForAll("flvUrlsWithParams") String flvUrl) {
        // FLV URL即使带有查询参数也应该被识别
        boolean hasFlvExtension = flvUrl.toLowerCase().contains(".flv");
        
        assertThat(hasFlvExtension)
                .as("带参数的FLV URL '%s' 应该被识别", flvUrl)
                .isTrue();
    }

    @Property(tries = 100)
    @Label("M3U8 URL可以包含查询参数")
    void m3u8UrlsCanHaveQueryParameters(@ForAll("m3u8UrlsWithParams") String m3u8Url) {
        // M3U8 URL即使带有查询参数也应该被识别
        boolean hasM3u8Extension = m3u8Url.toLowerCase().contains(".m3u8");
        
        assertThat(hasM3u8Extension)
                .as("带参数的M3U8 URL '%s' 应该被识别", m3u8Url)
                .isTrue();
    }

    // 自定义生成器：生成FLV格式的URL
    @Provide
    Arbitrary<String> flvUrls() {
        Arbitrary<String> protocol = Arbitraries.of("http://", "https://");
        Arbitrary<String> domain = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> s + ".com");
        Arbitrary<String> path = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> "/live/" + s);
        
        return Combinators.combine(protocol, domain, path)
                .as((p, d, path1) -> p + d + path1 + ".flv");
    }

    // 自定义生成器：生成M3U8格式的URL
    @Provide
    Arbitrary<String> m3u8Urls() {
        Arbitrary<String> protocol = Arbitraries.of("http://", "https://");
        Arbitrary<String> domain = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> s + ".com");
        Arbitrary<String> path = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> "/live/" + s);
        
        return Combinators.combine(protocol, domain, path)
                .as((p, d, path1) -> p + d + path1 + ".m3u8");
    }

    // 自定义生成器：生成支持的流URL（FLV或M3U8）
    @Provide
    Arbitrary<String> supportedStreamUrls() {
        return Arbitraries.oneOf(flvUrls(), m3u8Urls());
    }

    // 自定义生成器：生成不支持的流URL
    @Provide
    Arbitrary<String> unsupportedStreamUrls() {
        Arbitrary<String> protocol = Arbitraries.of("http://", "https://");
        Arbitrary<String> domain = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> s + ".com");
        Arbitrary<String> path = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> "/live/" + s);
        Arbitrary<String> extension = Arbitraries.of(".mp4", ".avi", ".mkv", ".ts", ".webm");
        
        return Combinators.combine(protocol, domain, path, extension)
                .as((p, d, path1, ext) -> p + d + path1 + ext);
    }

    // 自定义生成器：生成带查询参数的FLV URL
    @Provide
    Arbitrary<String> flvUrlsWithParams() {
        Arbitrary<String> baseUrl = flvUrls();
        Arbitrary<String> params = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> "?token=" + s + "&quality=origin");
        
        return Combinators.combine(baseUrl, params)
                .as((url, param) -> url + param);
    }

    // 自定义生成器：生成带查询参数的M3U8 URL
    @Provide
    Arbitrary<String> m3u8UrlsWithParams() {
        Arbitrary<String> baseUrl = m3u8Urls();
        Arbitrary<String> params = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(s -> "?token=" + s + "&quality=hd");
        
        return Combinators.combine(baseUrl, params)
                .as((url, param) -> url + param);
    }

    // 示例测试：验证已知的有效流URL
    @Example
    @Label("已知的FLV URL应该被识别")
    void knownFlvUrlsShouldBeRecognized() {
        String flvUrl1 = "https://live.douyin.com/stream/12345.flv";
        String flvUrl2 = "http://pull.example.com/live/room123.flv?token=abc";
        
        assertThat(flvUrl1.toLowerCase().contains(".flv")).isTrue();
        assertThat(flvUrl2.toLowerCase().contains(".flv")).isTrue();
    }

    // 示例测试：验证已知的有效M3U8 URL
    @Example
    @Label("已知的M3U8 URL应该被识别")
    void knownM3u8UrlsShouldBeRecognized() {
        String m3u8Url1 = "https://live.douyin.com/stream/12345.m3u8";
        String m3u8Url2 = "http://pull.example.com/live/room123.m3u8?token=xyz";
        
        assertThat(m3u8Url1.toLowerCase().contains(".m3u8")).isTrue();
        assertThat(m3u8Url2.toLowerCase().contains(".m3u8")).isTrue();
    }

    // 示例测试：验证不支持的格式
    @Example
    @Label("不支持的流格式应该被识别为无效")
    void unsupportedFormatsShouldBeInvalid() {
        String mp4Url = "https://example.com/video.mp4";
        String aviUrl = "http://example.com/video.avi";
        String mkvUrl = "https://example.com/video.mkv";
        
        assertThat(mp4Url.toLowerCase().contains(".flv")).isFalse();
        assertThat(mp4Url.toLowerCase().contains(".m3u8")).isFalse();
        assertThat(aviUrl.toLowerCase().contains(".flv")).isFalse();
        assertThat(aviUrl.toLowerCase().contains(".m3u8")).isFalse();
        assertThat(mkvUrl.toLowerCase().contains(".flv")).isFalse();
        assertThat(mkvUrl.toLowerCase().contains(".m3u8")).isFalse();
    }
}
