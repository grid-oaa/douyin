package com.douyin.liverecorder.service;

import com.douyin.liverecorder.infrastructure.HttpClientUtil;
import com.douyin.liverecorder.model.StreamInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 流提取服务
 * 负责从抖音直播间提取可用的流URL
 */
@Service
public class StreamExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamExtractor.class);
    
    private final HttpClientUtil httpClient;
    private final ObjectMapper objectMapper;
    
    // 支持的流格式
    private static final Pattern FLV_PATTERN = Pattern.compile(".*\\.flv(\\?.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern M3U8_PATTERN = Pattern.compile(".*\\.m3u8(\\?.*)?$", Pattern.CASE_INSENSITIVE);
    
    // 抖音流信息API模板
    private static final String STREAM_API_TEMPLATE = "https://live.douyin.com/webcast/room/web/enter/?aid=6383&web_rid=%s";
    
    public StreamExtractor(HttpClientUtil httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 提取直播流URL
     * 
     * @param douyinId 抖音号或直播间ID
     * @return 流信息对象
     * @throws IOException 如果提取失败
     */
    public StreamInfo extractStreamUrl(String douyinId) throws IOException {
        if (douyinId == null || douyinId.trim().isEmpty()) {
            throw new IllegalArgumentException("抖音号不能为空");
        }
        
        logger.info("提取直播流URL: {}", douyinId);
        
        try {
            // 构建API URL
            String apiUrl = String.format(STREAM_API_TEMPLATE, douyinId);
            
            // 发送HTTP请求
            String response = httpClient.get(apiUrl);
            
            // 解析响应获取流URL
            StreamInfo streamInfo = parseStreamInfoResponse(response);
            
            if (streamInfo.isValid()) {
                logger.info("成功提取流URL: format={}, quality={}, url={}", 
                           streamInfo.getFormat(), streamInfo.getQuality(), 
                           maskUrl(streamInfo.getUrl()));
            } else {
                logger.warn("未能提取有效的流URL");
            }
            
            return streamInfo;
            
        } catch (IOException e) {
            logger.error("提取流URL失败: {}, 错误: {}", douyinId, e.getMessage());
            throw new IOException("获取流URL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证流URL是否有效
     * 
     * @param url 流URL
     * @return 如果URL有效返回true，否则返回false
     */
    public boolean validateStreamUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            logger.debug("URL为空");
            return false;
        }
        
        // 检查URL格式
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("rtmp://")) {
            logger.debug("URL协议不支持: {}", url);
            return false;
        }
        
        // 检查是否为支持的流格式
        boolean isFLV = FLV_PATTERN.matcher(url).matches();
        boolean isM3U8 = M3U8_PATTERN.matcher(url).matches();
        
        if (!isFLV && !isM3U8) {
            logger.debug("URL格式不支持（仅支持FLV和M3U8）: {}", url);
            return false;
        }
        
        // 尝试连接验证URL可访问性
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            boolean isValid = (responseCode >= 200 && responseCode < 400);
            
            if (isValid) {
                logger.debug("URL验证成功: {}", maskUrl(url));
            } else {
                logger.debug("URL验证失败，响应码: {}", responseCode);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.debug("URL验证失败: {}, 错误: {}", maskUrl(url), e.getMessage());
            return false;
        }
    }
    
    /**
     * 解析流信息响应
     * 
     * @param response API响应内容
     * @return 流信息对象
     * @throws IOException 如果解析失败
     */
    private StreamInfo parseStreamInfoResponse(String response) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // 检查响应状态
            int statusCode = root.path("status_code").asInt(-1);
            if (statusCode != 0) {
                String statusMsg = root.path("status_msg").asText("未知错误");
                throw new IOException("API返回错误: " + statusMsg);
            }
            
            // 解析数据
            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                throw new IOException("响应中缺少data字段");
            }
            
            // 获取流数据（兼容 data.stream_url 与 data.data[0].stream_url）
            JsonNode room = resolveRoomNode(data);
            JsonNode streamData = resolveStreamUrlNode(data, room);
            if (streamData == null || streamData.isMissingNode()) {
                throw new IOException("响应中缺少stream_url字段");
            }
            
            // 尝试获取FLV流
            JsonNode flvPullUrl = streamData.path("flv_pull_url");
            if (!flvPullUrl.isMissingNode() && flvPullUrl.isObject()) {
                String flvUrl = extractBestQualityUrl(flvPullUrl);
                if (flvUrl != null && !flvUrl.isEmpty()) {
                    return new StreamInfo(flvUrl, "flv", "origin", true);
                }
            }
            
            // 尝试获取HLS流（M3U8）
            JsonNode hlsPullUrl = streamData.path("hls_pull_url");
            if (!hlsPullUrl.isMissingNode() && hlsPullUrl.isTextual()) {
                String m3u8Url = hlsPullUrl.asText();
                if (m3u8Url != null && !m3u8Url.isEmpty()) {
                    return new StreamInfo(m3u8Url, "m3u8", "origin", true);
                }
            }
            
            // 如果都没有找到，返回无效的流信息
            logger.warn("未能从响应中提取流URL");
            return new StreamInfo(null, null, null, false);
            
        } catch (Exception e) {
            logger.error("解析流信息响应失败", e);
            throw new IOException("解析流信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从质量映射中提取最佳质量的URL
     * 
     * @param qualityNode 质量节点（包含不同质量的URL）
     * @return 最佳质量的URL
     */
    private String extractBestQualityUrl(JsonNode qualityNode) {
        // 优先级：origin > uhd > hd > sd > ld
        String[] qualities = {"FULL_HD1", "ORIGIN", "HD1", "SD1", "SD2", "LD1"};
        
        for (String quality : qualities) {
            JsonNode urlNode = qualityNode.path(quality);
            if (!urlNode.isMissingNode() && urlNode.isTextual()) {
                String url = urlNode.asText();
                if (url != null && !url.isEmpty()) {
                    logger.debug("选择质量: {}", quality);
                    return url;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检测流格式
     * 
     * @param url 流URL
     * @return 流格式（flv, m3u8, 或 unknown）
     */
    private String detectStreamFormat(String url) {
        if (url == null) {
            return "unknown";
        }
        
        if (FLV_PATTERN.matcher(url).matches()) {
            return "flv";
        } else if (M3U8_PATTERN.matcher(url).matches()) {
            return "m3u8";
        } else {
            return "unknown";
        }
    }
    
    /**
     * 掩码URL用于日志输出（隐藏敏感参数）
     * 
     * @param url 原始URL
     * @return 掩码后的URL
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }
        
        // 只显示协议和域名部分
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            return url.substring(0, queryIndex) + "?...";
        }
        
        return url;
    }

    private JsonNode resolveRoomNode(JsonNode data) {
        if (data == null || data.isMissingNode()) {
            return null;
        }
        JsonNode room = data.path("room");
        if (!room.isMissingNode()) {
            return room;
        }
        JsonNode dataArray = data.path("data");
        if (dataArray.isArray() && dataArray.size() > 0) {
            return dataArray.get(0);
        }
        return null;
    }

    private JsonNode resolveStreamUrlNode(JsonNode data, JsonNode room) {
        if (data != null && !data.isMissingNode()) {
            JsonNode streamData = data.path("stream_url");
            if (!streamData.isMissingNode()) {
                return streamData;
            }
        }
        if (room != null && !room.isMissingNode()) {
            JsonNode streamData = room.path("stream_url");
            if (!streamData.isMissingNode()) {
                return streamData;
            }
        }
        return null;
    }
}
