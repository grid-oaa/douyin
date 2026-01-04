package com.douyin.liverecorder.exception;

import com.douyin.liverecorder.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 * 统一处理应用程序中的异常，返回适当的HTTP状态码和中文错误消息
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理无效抖音号异常
     */
    @ExceptionHandler(InvalidDouyinIdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDouyinIdException(
            InvalidDouyinIdException ex, HttpServletRequest request) {
        logger.error("无效抖音号: {}", ex.getDouyinId(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "无效抖音号",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理直播未找到异常
     */
    @ExceptionHandler(LiveStreamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLiveStreamNotFoundException(
            LiveStreamNotFoundException ex, HttpServletRequest request) {
        logger.warn("直播未找到: {}", ex.getDouyinId());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "直播未找到",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 处理网络异常
     */
    @ExceptionHandler(NetworkException.class)
    public ResponseEntity<ErrorResponse> handleNetworkException(
            NetworkException ex, HttpServletRequest request) {
        logger.error("网络错误: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "网络错误",
            "网络连接失败，请检查网络连接后重试",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * 处理录制异常
     */
    @ExceptionHandler(RecordingException.class)
    public ResponseEntity<ErrorResponse> handleRecordingException(
            RecordingException ex, HttpServletRequest request) {
        // 记录详细的错误信息，包括任务ID和错误详情
        logger.error("录制错误 [TaskID: {}]: {}, 详情: {}", 
                    ex.getTaskId(), ex.getMessage(), ex.getCause() != null ? ex.getCause().getMessage() : "无", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "录制错误",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * 处理并发限制异常
     */
    @ExceptionHandler(ConcurrentLimitException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentLimitException(
            ConcurrentLimitException ex, HttpServletRequest request) {
        logger.warn("并发限制: 当前{}个任务，最大{}个", ex.getCurrentCount(), ex.getMaxLimit());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "并发限制",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * 处理验证异常（Bean Validation）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse("输入验证失败");
        
        logger.error("输入验证失败: {}", message);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "输入验证失败",
            message,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理HTTP消息不可读异常（JSON解析错误、缺少请求体等）
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        logger.error("HTTP消息不可读: {}", ex.getMessage());
        
        String message = "请求格式错误，请检查JSON格式是否正确";
        if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            message = "缺少请求体";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "请求格式错误",
            message,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(violation -> violation.getMessage())
            .orElse("输入验证失败");
        
        logger.error("约束违反: {}", message);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "输入验证失败",
            message,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        logger.error("非法参数: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "非法参数",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 处理非法状态异常（包括并发限制）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        logger.error("非法状态: {}", ex.getMessage());
        
        // 检查是否为并发限制错误
        HttpStatus status = ex.getMessage().contains("已达上限") 
            ? HttpStatus.TOO_MANY_REQUESTS 
            : HttpStatus.BAD_REQUEST;
        
        ErrorResponse errorResponse = new ErrorResponse(
            status.value(),
            status == HttpStatus.TOO_MANY_REQUESTS ? "并发限制" : "非法状态",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        logger.error("未预期的错误: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "服务器错误",
            "服务器内部错误，请稍后重试",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
