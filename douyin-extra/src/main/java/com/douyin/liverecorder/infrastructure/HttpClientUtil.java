package com.douyin.liverecorder.infrastructure;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端工具类
 * 封装HTTP请求，支持超时和重试机制
 */
@Component
public class HttpClientUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 2;
    
    private final OkHttpClient client;
    @Value("${douyin.cookie:}")
    private String douyinCookie;
    @Value("${douyin.cookie-file:}")
    private String douyinCookieFile;
    @Value("${douyin.referer:}")
    private String douyinReferer;
    private volatile String runtimeCookie;
    
    public HttpClientUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }
    
    /**
     * 执行GET请求（带重试机制）
     * 
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public String get(String url) throws IOException {
        return executeWithRetry(url, MAX_RETRIES);
    }
    
    /**
     * 执行GET请求（不重试）
     * 
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public String getWithoutRetry(String url) throws IOException {
        return executeRequest(url);
    }
    
    /**
     * 执行带重试机制的请求
     * 
     * @param url 请求URL
     * @param maxRetries 最大重试次数
     * @return 响应内容
     * @throws IOException 如果所有重试都失败
     */
    private String executeWithRetry(String url, int maxRetries) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("尝试请求 {} (第 {}/{} 次)", url, attempt, maxRetries);
                String response = executeRequest(url);
                
                if (attempt > 1) {
                    logger.info("请求成功 (第 {} 次尝试): {}", attempt, url);
                }
                
                return response;
                
            } catch (IOException e) {
                lastException = e;
                logger.warn("请求失败 (第 {}/{} 次): {}, 错误: {}", 
                           attempt, maxRetries, url, e.getMessage());
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < maxRetries) {
                    try {
                        logger.debug("等待 {} 秒后重试...", RETRY_DELAY_SECONDS);
                        Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试等待被中断", ie);
                    }
                }
            }
        }
        
        // 所有重试都失败
        logger.error("所有重试都失败: {}", url);
        throw new IOException("请求失败，已重试 " + maxRetries + " 次", lastException);
    }
    
    /**
     * 执行单次HTTP请求
     * 
     * @param url 请求URL
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    private String executeRequest(String url) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        applyDouyinHeaders(requestBuilder);
        Request request = requestBuilder.build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP请求失败: " + response.code() + " " + response.message());
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }
            
            return body.string();
        }
    }
    
    /**
     * 执行POST请求（带重试机制）
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    public String post(String url, String jsonBody) throws IOException {
        return postWithRetry(url, jsonBody, MAX_RETRIES);
    }
    
    /**
     * 执行带重试机制的POST请求
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @param maxRetries 最大重试次数
     * @return 响应内容
     * @throws IOException 如果所有重试都失败
     */
    private String postWithRetry(String url, String jsonBody, int maxRetries) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.debug("尝试POST请求 {} (第 {}/{} 次)", url, attempt, maxRetries);
                String response = executePostRequest(url, jsonBody);
                
                if (attempt > 1) {
                    logger.info("POST请求成功 (第 {} 次尝试): {}", attempt, url);
                }
                
                return response;
                
            } catch (IOException e) {
                lastException = e;
                logger.warn("POST请求失败 (第 {}/{} 次): {}, 错误: {}", 
                           attempt, maxRetries, url, e.getMessage());
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < maxRetries) {
                    try {
                        logger.debug("等待 {} 秒后重试...", RETRY_DELAY_SECONDS);
                        Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试等待被中断", ie);
                    }
                }
            }
        }
        
        // 所有重试都失败
        logger.error("所有POST重试都失败: {}", url);
        throw new IOException("POST请求失败，已重试 " + maxRetries + " 次", lastException);
    }
    
    /**
     * 执行单次POST请求
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    private String executePostRequest(String url, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(
                jsonBody, 
                MediaType.parse("application/json; charset=utf-8")
        );
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Content-Type", "application/json");
        applyDouyinHeaders(requestBuilder);
        Request request = requestBuilder.build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP POST请求失败: " + response.code() + " " + response.message());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }
            
            return responseBody.string();
        }
    }
    
    /**
     * 获取OkHttpClient实例
     * 
     * @return OkHttpClient实例
     */
    public OkHttpClient getClient() {
        return client;
    }

    private void applyDouyinHeaders(Request.Builder requestBuilder) {
        String cookie = resolveDouyinCookie();
        if (cookie != null && !cookie.trim().isEmpty()) {
            requestBuilder.addHeader("Cookie", cookie.trim());
        }
        if (douyinReferer != null && !douyinReferer.trim().isEmpty()) {
            requestBuilder.addHeader("Referer", douyinReferer.trim());
        }
    }

    private String resolveDouyinCookie() {
        if (runtimeCookie != null && !runtimeCookie.trim().isEmpty()) {
            return runtimeCookie.trim();
        }
        String cookieFromFile = readCookieFromFile();
        if (cookieFromFile != null && !cookieFromFile.isEmpty()) {
            return cookieFromFile;
        }
        return douyinCookie;
    }

    private String readCookieFromFile() {
        if (douyinCookieFile == null || douyinCookieFile.trim().isEmpty()) {
            return null;
        }

        Path path = Paths.get(douyinCookieFile.trim());
        if (!Files.exists(path)) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (builder.length() > 0 && !builder.toString().endsWith(";")) {
                    builder.append("; ");
                }
                builder.append(trimmed);
            }
            String cookie = builder.toString().trim();
            return cookie.isEmpty() ? null : cookie;
        } catch (IOException e) {
            logger.debug("读取Cookie文件失败: {}", e.getMessage());
            return null;
        }
    }

    public void updateRuntimeCookie(String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) {
            this.runtimeCookie = null;
            return;
        }
        this.runtimeCookie = cookie.trim();
    }
}
