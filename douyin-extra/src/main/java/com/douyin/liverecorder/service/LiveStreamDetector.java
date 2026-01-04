package com.douyin.liverecorder.service;

import com.douyin.liverecorder.infrastructure.HttpClientUtil;
import com.douyin.liverecorder.model.LiveStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 直播检测服务
 * 负责检测抖音用户的直播状态
 */
@Service
public class LiveStreamDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveStreamDetector.class);
    
    private final HttpClientUtil httpClient;
    private final ObjectMapper objectMapper;
    
    // 抖音Web接口URL模板（注意：实际URL可能需要根据抖音API变化调整）
    private static final String DOUYIN_LIVE_API_TEMPLATE = "https://live.douyin.com/webcast/room/web/enter/?aid=6383&web_rid=%s";
    
    public LiveStreamDetector(HttpClientUtil httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 检查直播状态
     * 
     * @param douyinId 抖音号
     * @return 直播状态信息
     * @throws IOException 如果网络请求失败
     * @throws IllegalArgumentException 如果抖音号无效
     */
    public LiveStatus checkLiveStatus(String douyinId) throws IOException {
        if (douyinId == null || douyinId.trim().isEmpty()) {
            throw new IllegalArgumentException("抖音号不能为空");
        }
        
        logger.info("检查直播状态: {}", douyinId);
        
        try {
            // 构建API URL
            String apiUrl = String.format(DOUYIN_LIVE_API_TEMPLATE, douyinId);
            
            // 发送HTTP请求（带重试机制）
            String response = httpClient.get(apiUrl);
            
            // 解析响应
            LiveStatus liveStatus = parseLiveStatusResponse(response, douyinId);
            
            if (liveStatus.isLive()) {
                logger.info("用户 {} 正在直播，直播间ID: {}, 标题: {}", 
                           douyinId, liveStatus.getRoomId(), liveStatus.getTitle());
            } else {
                logger.info("用户 {} 当前未在直播", douyinId);
            }
            
            return liveStatus;
            
        } catch (IOException e) {
            logger.error("检查直播状态失败: {}, 错误: {}", douyinId, e.getMessage());
            throw new IOException("网络连接失败，请检查网络连接后重试", e);
        } catch (Exception e) {
            logger.error("解析直播状态响应失败: {}", douyinId, e);
            throw new IOException("解析直播状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取直播流URL
     * 
     * @param douyinId 抖音号
     * @return 直播流URL
     * @throws IOException 如果网络请求失败或直播未开始
     */
    public String getLiveStreamUrl(String douyinId) throws IOException {
        if (douyinId == null || douyinId.trim().isEmpty()) {
            throw new IllegalArgumentException("抖音号不能为空");
        }
        
        logger.info("获取直播流URL: {}", douyinId);
        
        try {
            // 构建API URL
            String apiUrl = String.format(DOUYIN_LIVE_API_TEMPLATE, douyinId);
            
            // 发送HTTP请求（带重试机制）
            String response = httpClient.get(apiUrl);
            
            // 解析响应获取流URL
            String streamUrl = parseStreamUrlFromResponse(response, douyinId);
            
            logger.info("成功获取直播流URL: {}", maskUrl(streamUrl));
            
            return streamUrl;
            
        } catch (IOException e) {
            logger.error("获取直播流URL失败: {}, 错误: {}", douyinId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("解析直播流URL响应失败: {}", douyinId, e);
            throw new IOException("解析直播流URL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析直播状态响应
     * 
     * @param response API响应内容
     * @param douyinId 抖音号
     * @return 直播状态对象
     * @throws IOException 如果解析失败或API返回错误
     */
    private LiveStatus parseLiveStatusResponse(String response, String douyinId) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(response);
            logger.debug("web/enter raw response: {}", truncateResponse(response));
            
            // 检查响应状态码
            int statusCode = root.path("status_code").asInt(-1);
            if (statusCode != 0) {
                String statusMsg = root.path("status_msg").asText("未知错误");
                logger.error("抖音API返回错误: statusCode={}, statusMsg={}, douyinId={}", 
                            statusCode, statusMsg, douyinId);
                
                // 根据状态码返回具体的错误信息
                String errorMessage;
                switch (statusCode) {
                    case 404:
                    case 4003:
                        errorMessage = "未找到该抖音用户或直播间";
                        break;
                    case 403:
                    case 4001:
                        errorMessage = "访问被拒绝，可能是频率限制";
                        break;
                    case 500:
                    case 5000:
                        errorMessage = "抖音服务器错误，请稍后重试";
                        break;
                    default:
                        errorMessage = "抖音API错误: " + statusMsg;
                }
                
                throw new IOException(errorMessage);
            }
            
            // 解析数据
            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                logger.warn("响应中缺少data字段");
                return new LiveStatus(false, null, null, null);
            }
            
            // 获取直播间信息（兼容 data.room 与 data.data[0] 两种结构）
            JsonNode room = resolveRoomNode(data);
            if (room == null || room.isMissingNode()) {
                logger.warn("Response missing room node (data.room or data.data[0])");
                return new LiveStatus(false, null, null, null);
            }
            
            // 检查直播状态
            int status = room.path("status").asInt(0);
            boolean isLive = (status == 2); // 2表示正在直播
            
            if (!isLive) {
                return new LiveStatus(false, null, null, null);
            }
            
            // 提取直播间信息
            String roomId = room.path("id_str").asText(null);
            String title = room.path("title").asText("未知标题");
            
            // 解析开播时间
            LocalDateTime startTime = null;
            long createTime = room.path("create_time").asLong(0);
            if (createTime > 0) {
                startTime = LocalDateTime.ofEpochSecond(createTime, 0, 
                    java.time.ZoneOffset.ofHours(8)); // 使用东八区时间
            }
            
            return new LiveStatus(isLive, roomId, title, startTime);
            
        } catch (IOException e) {
            // 重新抛出IOException（包括API错误）
            throw e;
        } catch (Exception e) {
            logger.error("解析JSON响应失败", e);
            throw new IOException("解析直播状态响应失败", e);
        }
    }
    
    /**
     * 从响应中解析直播流URL
     * 
     * @param response API响应内容
     * @param douyinId 抖音号
     * @return 直播流URL
     * @throws IOException 如果解析失败或直播未开始
     */
    private String parseStreamUrlFromResponse(String response, String douyinId) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // 检查响应状态码
            int statusCode = root.path("status_code").asInt(-1);
            if (statusCode != 0) {
                String statusMsg = root.path("status_msg").asText("未知错误");
                logger.error("抖音API返回错误: statusCode={}, statusMsg={}, douyinId={}", 
                            statusCode, statusMsg, douyinId);
                
                // 根据状态码返回具体的错误信息
                String errorMessage;
                switch (statusCode) {
                    case 404:
                    case 4003:
                        errorMessage = "未找到该抖音用户或直播间";
                        break;
                    case 403:
                    case 4001:
                        errorMessage = "访问被拒绝，可能是频率限制";
                        break;
                    case 500:
                    case 5000:
                        errorMessage = "抖音服务器错误，请稍后重试";
                        break;
                    default:
                        errorMessage = "抖音API错误: " + statusMsg;
                }
                
                throw new IOException(errorMessage);
            }
            
            // 解析数据
            JsonNode data = root.path("data");
            if (data.isMissingNode()) {
                throw new IOException("响应中缺少data字段");
            }
            
            // 获取直播间信息
            JsonNode room = resolveRoomNode(data);
            if (room == null || room.isMissingNode()) {
                throw new IOException("响应中缺少room字段");
            }
            
            // 检查直播状态
            int status = room.path("status").asInt(0);
            if (status != 2) {
                throw new IOException("该用户当前未在直播");
            }
            
            // 获取流数据
            JsonNode streamData = resolveStreamUrlNode(data, room);
            if (streamData == null || streamData.isMissingNode()) {
                throw new IOException("响应中缺少stream_url字段");
            }
            
            // 尝试获取FLV流（优先）
            JsonNode flvPullUrl = streamData.path("flv_pull_url");
            if (!flvPullUrl.isMissingNode() && flvPullUrl.isObject()) {
                String flvUrl = extractBestQualityUrl(flvPullUrl);
                if (flvUrl != null && !flvUrl.isEmpty()) {
                    logger.debug("提取到FLV流URL");
                    return flvUrl;
                }
            }
            
            // 尝试获取HLS流（M3U8）
            JsonNode hlsPullUrl = streamData.path("hls_pull_url");
            if (!hlsPullUrl.isMissingNode() && hlsPullUrl.isTextual()) {
                String m3u8Url = hlsPullUrl.asText();
                if (m3u8Url != null && !m3u8Url.isEmpty()) {
                    logger.debug("提取到M3U8流URL");
                    return m3u8Url;
                }
            }
            
            // 如果都没有找到，抛出异常
            throw new IOException("未能从响应中提取流URL");
            
        } catch (IOException e) {
            // 重新抛出IOException
            throw e;
        } catch (Exception e) {
            logger.error("解析流URL响应失败", e);
            throw new IOException("解析流URL失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从质量映射中提取最佳质量的URL
     * 
     * @param qualityNode 质量节点（包含不同质量的URL）
     * @return 最佳质量的URL
     */
    private String extractBestQualityUrl(JsonNode qualityNode) {
        // 优先级：FULL_HD1 > ORIGIN > HD1 > SD1 > SD2 > LD1
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

    private String truncateResponse(String response) {
        if (response == null) {
            return "null";
        }
        int maxLen = 800;
        if (response.length() <= maxLen) {
            return response;
        }
        return response.substring(0, maxLen) + "...";
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
