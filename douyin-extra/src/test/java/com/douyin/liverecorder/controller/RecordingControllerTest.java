package com.douyin.liverecorder.controller;

import com.douyin.liverecorder.dto.RecordingResponse;
import com.douyin.liverecorder.dto.StartRecordingRequest;
import com.douyin.liverecorder.exception.ConcurrentLimitException;
import com.douyin.liverecorder.exception.InvalidDouyinIdException;
import com.douyin.liverecorder.model.RecordingStatus;
import com.douyin.liverecorder.model.RecordingTask;
import com.douyin.liverecorder.model.TaskStatus;
import com.douyin.liverecorder.service.RecordingManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RecordingController??????
 * ???????PI??????????????????????????
 */
@WebMvcTest(RecordingController.class)
class RecordingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private RecordingManager recordingManager;
    
    private RecordingTask testTask;
    
    @BeforeEach
    void setUp() {
        // ?????????
        testTask = new RecordingTask("test123");
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setStartTime(LocalDateTime.now());
    }
    
    /**
     * ??????????- ??????
     */
    @Test
    void testStartRecording_Success() throws Exception {
        // ?????????
        StartRecordingRequest request = new StartRecordingRequest("test123");
        
        // Mock RecordingManager???
        when(recordingManager.createTask(anyString(), anyBoolean())).thenReturn(testTask);
        when(recordingManager.startTask(anyString())).thenReturn(true);
        
        // ??????????????
        mockMvc.perform(post("/api/recordings/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(testTask.getTaskId()))
                .andExpect(jsonPath("$.douyinId").value("test123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
    
    /**
     * ??????????- ??????
     */
    @Test
    void testStartRecording_EmptyDouyinId() throws Exception {
        // ????????? - ??????
        StartRecordingRequest request = new StartRecordingRequest("");
        
        // ??????????????
        mockMvc.perform(post("/api/recordings/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * ??????????- ???????????
     */
    @Test
    void testStartRecording_InvalidDouyinIdFormat() throws Exception {
        // ????????? - ???????????????
        StartRecordingRequest request = new StartRecordingRequest("@invalid!");
        
        // ??????????????
        mockMvc.perform(post("/api/recordings/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * ??????????- ?????????
     */
    @Test
    void testStartRecording_ConcurrentLimitReached() throws Exception {
        // ?????????
        StartRecordingRequest request = new StartRecordingRequest("test123");
        
        // Mock RecordingManager??? - ????????????
        when(recordingManager.createTask(anyString(), anyBoolean()))
                .thenThrow(new IllegalStateException("?????????????????????????????"));
        
        // ??????????????
        mockMvc.perform(post("/api/recordings/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("?????????????????????????????"));
    }
    
    /**
     * ????????? - ??????
     */
    @Test
    void testStopRecording_Success() throws Exception {
        // ?????????
        String taskId = testTask.getTaskId();
        testTask.setStatus(TaskStatus.CANCELLED);
        testTask.setEndTime(LocalDateTime.now());
        
        // Mock RecordingManager???
        when(recordingManager.stopTask(taskId)).thenReturn(true);
        when(recordingManager.getTaskStatus(taskId)).thenReturn(createRecordingStatus(testTask));
        when(recordingManager.getTask(taskId)).thenReturn(testTask);
        
        // ??????????????
        mockMvc.perform(post("/api/recordings/{taskId}/stop", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
    
    /**
     * ????????? - ????????
     */
    @Test
    void testStopRecording_TaskNotFound() throws Exception {
        // ?????????
        String taskId = "nonexistent";
        
        // Mock RecordingManager??? - ??????????????
        when(recordingManager.stopTask(taskId))
                .thenThrow(new IllegalArgumentException("???????? " + taskId));
        
        // ??????????????
        mockMvc.perform(post("/api/recordings/{taskId}/stop", taskId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("???????? " + taskId));
    }
    
    /**
     * ?????????????- ??????
     */
    @Test
    void testGetRecordingStatus_Success() throws Exception {
        // ?????????
        String taskId = testTask.getTaskId();
        testTask.setStatus(TaskStatus.RECORDING);
        RecordingStatus status = createRecordingStatus(testTask);
        
        // Mock RecordingManager???
        when(recordingManager.getTaskStatus(taskId)).thenReturn(status);
        
        // ??????????????
        mockMvc.perform(get("/api/recordings/{taskId}/status", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.status").value("RECORDING"));
    }
    
    /**
     * ?????????????- ????????
     */
    @Test
    void testGetRecordingStatus_TaskNotFound() throws Exception {
        // ?????????
        String taskId = "nonexistent";
        
        // Mock RecordingManager??? - ??????????????
        when(recordingManager.getTaskStatus(taskId))
                .thenThrow(new IllegalArgumentException("???????? " + taskId));
        
        // ??????????????
        mockMvc.perform(get("/api/recordings/{taskId}/status", taskId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("???????? " + taskId));
    }
    
    /**
     * ????????????????- ??????
     */
    @Test
    void testListRecordings_Success() throws Exception {
        // ?????????
        RecordingTask task1 = new RecordingTask("test123");
        task1.setStatus(TaskStatus.RECORDING);
        
        RecordingTask task2 = new RecordingTask("test456");
        task2.setStatus(TaskStatus.DETECTING);
        
        List<RecordingTask> tasks = Arrays.asList(task1, task2);
        
        // Mock RecordingManager???
        when(recordingManager.listActiveTasks()).thenReturn(tasks);
        
        // ??????????????
        mockMvc.perform(get("/api/recordings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].douyinId").value("test123"))
                .andExpect(jsonPath("$[0].status").value("RECORDING"))
                .andExpect(jsonPath("$[1].douyinId").value("test456"))
                .andExpect(jsonPath("$[1].status").value("DETECTING"));
    }
    
    /**
     * ????????????????- ?????
     */
    @Test
    void testListRecordings_EmptyList() throws Exception {
        // Mock RecordingManager??? - ????????
        when(recordingManager.listActiveTasks()).thenReturn(Arrays.asList());
        
        // ??????????????
        mockMvc.perform(get("/api/recordings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    /**
     * ????????? - ????????
     */
    @Test
    void testStartRecording_MissingRequestBody() throws Exception {
        // ??????????????
        mockMvc.perform(post("/api/recordings/start")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * ????????? - ?????SON???
     */
    @Test
    void testStartRecording_InvalidJson() throws Exception {
        // ??????????????
        mockMvc.perform(post("/api/recordings/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * ???????????ecordingStatus???
     */
    private RecordingStatus createRecordingStatus(RecordingTask task) {
        RecordingStatus status = new RecordingStatus();
        status.setTaskId(task.getTaskId());
        status.setStatus(task.getStatus());
        if (task.getError() != null) {
            status.setError(task.getError());
        }
        return status;
    }
}
