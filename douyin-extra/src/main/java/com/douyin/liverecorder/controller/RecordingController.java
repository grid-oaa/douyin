package com.douyin.liverecorder.controller;

import com.douyin.liverecorder.dto.RecordingResponse;
import com.douyin.liverecorder.dto.StartRecordingRequest;
import com.douyin.liverecorder.model.RecordingStatus;
import com.douyin.liverecorder.model.RecordingTask;
import com.douyin.liverecorder.service.RecordingManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 录制控制器
 * 提供HTTP/REST接口供用户交互
 */
@RestController
@RequestMapping("/api/recordings")
public class RecordingController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecordingController.class);
    
    private final RecordingManager recordingManager;
    @Value("${recording.auto-enabled:true}")
    private boolean defaultAutoEnabled;
    
    public RecordingController(RecordingManager recordingManager) {
        this.recordingManager = recordingManager;
    }
    
    /**
     * 开始录制
     * POST /api/recordings/start
     * 
     * @param request 开始录制请求
     * @return 录制任务信息
     */
    @PostMapping("/start")
    public ResponseEntity<RecordingResponse> startRecording(
            @Valid @RequestBody StartRecordingRequest request) {
        
        boolean autoEnabled = request.getAuto() != null ? request.getAuto() : defaultAutoEnabled;

        logger.info("收到开始录制请求: douyinId={}, auto={}", request.getDouyinId(), autoEnabled);
        
        // 创建任务
        RecordingTask task = recordingManager.createTask(request.getDouyinId(), autoEnabled);
        
        // 启动任务
        recordingManager.startTask(task.getTaskId());
        
        // 返回任务信息
        RecordingResponse response = new RecordingResponse(task);
        
        logger.info("录制任务已创建并启动: taskId={}, douyinId={}", 
                   task.getTaskId(), request.getDouyinId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 停止录制
     * POST /api/recordings/{taskId}/stop
     * 
     * @param taskId 任务ID
     * @return 录制任务信息
     */
    @PostMapping("/{taskId}/stop")
    public ResponseEntity<RecordingResponse> stopRecording(
            @PathVariable String taskId) {
        
        logger.info("收到停止录制请求: taskId={}", taskId);
        
        // 停止任务
        boolean stopped = recordingManager.stopTask(taskId);
        
        if (!stopped) {
            logger.error("停止录制失败: taskId={}", taskId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        // 获取任务状态
        RecordingStatus status = recordingManager.getTaskStatus(taskId);
        
        // 构造响应（需要从任务映射中获取完整任务信息）
        RecordingTask task = recordingManager.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        RecordingResponse response = new RecordingResponse(task);
        
        logger.info("录制任务已停止: taskId={}", taskId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询录制状态
     * GET /api/recordings/{taskId}/status
     * 
     * @param taskId 任务ID
     * @return 录制状态信息
     */
    @GetMapping("/{taskId}/status")
    public ResponseEntity<RecordingStatus> getRecordingStatus(
            @PathVariable String taskId) {
        
        logger.debug("查询录制状态: taskId={}", taskId);
        
        // 获取任务状态
        RecordingStatus status = recordingManager.getTaskStatus(taskId);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 列出所有录制任务
     * GET /api/recordings
     * 
     * @return 录制任务列表
     */
    @GetMapping
    public ResponseEntity<List<RecordingResponse>> listRecordings() {
        
        logger.debug("查询所有录制任务");
        
        // 获取所有活动任务
        List<RecordingTask> tasks = recordingManager.listActiveTasks();
        
        // 转换为响应DTO
        List<RecordingResponse> responses = tasks.stream()
                .map(RecordingResponse::new)
                .collect(Collectors.toList());
        
        logger.debug("返回录制任务列表: 数量={}", responses.size());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 根据任务ID查找任务
     * 这是一个辅助方法，用于从RecordingManager获取完整的任务信息
     * 
     * @param taskId 任务ID
     * @return 录制任务，如果不存在返回null
     */
    private RecordingTask findTaskById(String taskId) {
        return recordingManager.getTask(taskId);
    }
}
