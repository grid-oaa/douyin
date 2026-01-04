package com.douyin.liverecorder.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.douyin.liverecorder.exception.*;
import com.douyin.liverecorder.service.RecordingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志记录测试
 * 验证错误日志包含必要信息和日志格式正确
 * 
 * 需求：7.2
 */
@SpringBootTest
public class LoggingTest {
    
    @Autowired
    private GlobalExceptionHandler exceptionHandler;
    
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    
    @BeforeEach
    public void setUp() {
        // 获取GlobalExceptionHandler的logger
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        
        // 创建并启动ListAppender来捕获日志
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }
    
    @AfterEach
    public void tearDown() {
        // 清理
        if (listAppender != null) {
            listAppender.stop();
            logger.detachAppender(listAppender);
        }
        MDC.clear();
    }
    
    /**
     * 测试录制异常日志包含任务ID
     */
    @Test
    public void testRecordingExceptionLogContainsTaskId() {
        // Given
        String taskId = "test-task-123";
        String errorMessage = "录制失败：流中断";
        RecordingException exception = new RecordingException(taskId, errorMessage);
        
        // When
        try {
            exceptionHandler.handleRecordingException(exception, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        
        ILoggingEvent logEvent = logsList.get(0);
        String logMessage = logEvent.getFormattedMessage();
        
        // 验证日志包含任务ID
        assertTrue(logMessage.contains(taskId), 
                  "日志应该包含任务ID: " + taskId);
        assertTrue(logMessage.contains("TaskID"), 
                  "日志应该包含TaskID标签");
    }
    
    /**
     * 测试录制异常日志包含错误详情
     */
    @Test
    public void testRecordingExceptionLogContainsErrorDetails() {
        // Given
        String taskId = "test-task-456";
        String errorMessage = "磁盘空间不足";
        RecordingException exception = new RecordingException(taskId, errorMessage);
        
        // When
        try {
            exceptionHandler.handleRecordingException(exception, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        
        ILoggingEvent logEvent = logsList.get(0);
        String logMessage = logEvent.getFormattedMessage();
        
        // 验证日志包含错误消息
        assertTrue(logMessage.contains(errorMessage), 
                  "日志应该包含错误消息: " + errorMessage);
    }
    
    /**
     * 测试网络异常日志记录
     */
    @Test
    public void testNetworkExceptionLogging() {
        // Given
        String errorMessage = "连接超时";
        NetworkException exception = new NetworkException(errorMessage);
        
        // When
        try {
            exceptionHandler.handleNetworkException(exception, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        
        ILoggingEvent logEvent = logsList.get(0);
        assertEquals(ch.qos.logback.classic.Level.ERROR, logEvent.getLevel(), 
                    "网络错误应该记录为ERROR级别");
    }
    
    /**
     * 测试并发限制异常日志记录
     */
    @Test
    public void testConcurrentLimitExceptionLogging() {
        // Given
        int currentCount = 5;
        int maxLimit = 5;
        ConcurrentLimitException exception = new ConcurrentLimitException(currentCount, maxLimit);
        
        // When
        try {
            exceptionHandler.handleConcurrentLimitException(exception, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        
        ILoggingEvent logEvent = logsList.get(0);
        String logMessage = logEvent.getFormattedMessage();
        
        // 验证日志包含并发数量信息
        assertTrue(logMessage.contains(String.valueOf(currentCount)), 
                  "日志应该包含当前任务数");
        assertTrue(logMessage.contains(String.valueOf(maxLimit)), 
                  "日志应该包含最大限制数");
    }
    
    /**
     * 测试日志级别正确性
     */
    @Test
    public void testLogLevels() {
        // Test ERROR level for recording exception
        RecordingException recordingException = new RecordingException("task-1", "测试错误");
        try {
            exceptionHandler.handleRecordingException(recordingException, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        assertEquals(ch.qos.logback.classic.Level.ERROR, logsList.get(0).getLevel(), 
                    "录制错误应该记录为ERROR级别");
        
        // Clear for next test
        listAppender.list.clear();
        
        // Test WARN level for concurrent limit
        ConcurrentLimitException concurrentException = new ConcurrentLimitException(5, 5);
        try {
            exceptionHandler.handleConcurrentLimitException(concurrentException, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        assertEquals(ch.qos.logback.classic.Level.WARN, logsList.get(0).getLevel(), 
                    "并发限制应该记录为WARN级别");
    }
    
    /**
     * 测试MDC上下文在日志中可用
     */
    @Test
    public void testMDCContextInLogs() {
        // Given
        String taskId = "mdc-test-task";
        String douyinId = "test-douyin-123";
        
        // 设置MDC
        MDC.put("taskId", taskId);
        MDC.put("douyinId", douyinId);
        
        // 获取RecordingManager的logger来测试MDC
        Logger recordingLogger = (Logger) LoggerFactory.getLogger(RecordingManager.class);
        ListAppender<ILoggingEvent> mdcAppender = new ListAppender<>();
        mdcAppender.start();
        recordingLogger.addAppender(mdcAppender);
        
        try {
            // When - 记录一条日志
            recordingLogger.info("测试MDC日志");
            
            // Then
            List<ILoggingEvent> logsList = mdcAppender.list;
            assertFalse(logsList.isEmpty(), "应该有日志记录");
            
            ILoggingEvent logEvent = logsList.get(0);
            Map<String, String> mdcMap = logEvent.getMDCPropertyMap();
            
            // 验证MDC包含taskId和douyinId
            assertNotNull(mdcMap, "MDC映射不应该为null");
            assertEquals(taskId, mdcMap.get("taskId"), "MDC应该包含taskId");
            assertEquals(douyinId, mdcMap.get("douyinId"), "MDC应该包含douyinId");
            
        } finally {
            mdcAppender.stop();
            recordingLogger.detachAppender(mdcAppender);
            MDC.clear();
        }
    }
    
    /**
     * 测试日志消息格式
     */
    @Test
    public void testLogMessageFormat() {
        // Given
        String taskId = "format-test-task";
        String errorMessage = "测试错误消息";
        RecordingException exception = new RecordingException(taskId, errorMessage);
        
        // When
        try {
            exceptionHandler.handleRecordingException(exception, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        
        ILoggingEvent logEvent = logsList.get(0);
        String logMessage = logEvent.getFormattedMessage();
        
        // 验证日志格式包含关键元素
        assertTrue(logMessage.contains("录制错误"), "日志应该包含操作描述");
        assertTrue(logMessage.contains("[TaskID:"), "日志应该使用方括号格式化TaskID");
        assertTrue(logMessage.contains(taskId), "日志应该包含任务ID");
        assertTrue(logMessage.contains(errorMessage), "日志应该包含错误消息");
    }
    
    /**
     * 测试异常堆栈跟踪被记录
     */
    @Test
    public void testExceptionStackTraceLogged() {
        // Given
        String taskId = "stack-trace-test";
        String errorMessage = "测试异常";
        Exception cause = new RuntimeException("根本原因");
        RecordingException exception = new RecordingException(taskId, errorMessage, cause);
        
        // When
        try {
            exceptionHandler.handleRecordingException(exception, 
                new org.springframework.mock.web.MockHttpServletRequest());
        } catch (Exception e) {
            // Ignore
        }
        
        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertFalse(logsList.isEmpty(), "应该有日志记录");
        
        ILoggingEvent logEvent = logsList.get(0);
        
        // 验证异常被附加到日志事件
        assertNotNull(logEvent.getThrowableProxy(), "日志应该包含异常信息");
    }
}
