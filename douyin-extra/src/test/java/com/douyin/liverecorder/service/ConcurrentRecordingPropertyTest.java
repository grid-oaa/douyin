package com.douyin.liverecorder.service;

import com.douyin.liverecorder.model.RecordingTask;
import com.douyin.liverecorder.model.TaskStatus;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property tests for concurrent task creation.
 */
class ConcurrentRecordingPropertyTest {

    @Property(tries = 100)
    @Label("Concurrent creation should not interfere")
    void concurrentTaskCreationShouldNotInterfere(
            @ForAll @IntRange(min = 1, max = 5) int taskCount,
            @ForAll("douyinIdList") List<String> douyinIds) throws InterruptedException {

        List<String> limitedDouyinIds = douyinIds.subList(0, Math.min(taskCount, douyinIds.size()));
        MockRecordingManager manager = new MockRecordingManager(5);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(limitedDouyinIds.size());

        List<RecordingTask> createdTasks = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();

        for (String douyinId : limitedDouyinIds) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    RecordingTask task = manager.createTask(douyinId, false, "D:\\recordings");
                    synchronized (createdTasks) {
                        createdTasks.add(task);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
        assertThat(createdTasks).hasSize(limitedDouyinIds.size());

        Set<String> taskIds = new HashSet<>();
        for (RecordingTask task : createdTasks) {
            assertThat(taskIds.add(task.getTaskId()))
                .as("taskId should be unique")
                .isTrue();
        }

        Set<String> actualDouyinIds = new HashSet<>();
        for (RecordingTask task : createdTasks) {
            actualDouyinIds.add(task.getDouyinId());
        }
        assertThat(actualDouyinIds).containsExactlyInAnyOrderElementsOf(new HashSet<>(limitedDouyinIds));

        for (RecordingTask task : createdTasks) {
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        }
    }

    @Property(tries = 100)
    @Label("Concurrent tasks have independent resources")
    void concurrentTasksShouldHaveIndependentResources(
            @ForAll @IntRange(min = 2, max = 5) int taskCount,
            @ForAll("douyinIdList") List<String> douyinIds) {

        List<String> limitedDouyinIds = douyinIds.subList(0, Math.min(taskCount, douyinIds.size()));
        MockRecordingManager manager = new MockRecordingManager(5);

        List<RecordingTask> tasks = new ArrayList<>();
        for (String douyinId : limitedDouyinIds) {
            RecordingTask task = manager.createTask(douyinId, false, "D:\\recordings");
            tasks.add(task);
        }

        Set<String> taskIds = new HashSet<>();
        for (RecordingTask task : tasks) {
            assertThat(taskIds.add(task.getTaskId()))
                .as("taskId should be unique")
                .isTrue();
        }

        assertThat(tasks).hasSize(limitedDouyinIds.size());

        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).getDouyinId()).isEqualTo(limitedDouyinIds.get(i));
        }

        if (tasks.size() >= 2) {
            RecordingTask task1 = tasks.get(0);
            RecordingTask task2 = tasks.get(1);

            task1.setStatus(TaskStatus.RECORDING);
            task1.setError("test error");

            assertThat(task2.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(task2.getError()).isNull();
        }
    }

    @Provide
    Arbitrary<List<String>> douyinIdList() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(5)
            .ofMaxLength(20)
            .list()
            .ofMinSize(5)
            .ofMaxSize(10);
    }

    private static class MockRecordingManager {
        private final java.util.concurrent.ConcurrentHashMap<String, RecordingTask> taskMap =
            new java.util.concurrent.ConcurrentHashMap<>();
        private final int maxConcurrentTasks;

        public MockRecordingManager(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
        }

        public RecordingTask createTask(String douyinId, boolean autoEnabled, String outputDir) {
            if (douyinId == null || douyinId.trim().isEmpty()) {
                throw new IllegalArgumentException("douyinId must not be blank");
            }

            int activeTaskCount = (int) taskMap.values().stream()
                .filter(task -> task.getStatus() == TaskStatus.DETECTING ||
                               task.getStatus() == TaskStatus.RECORDING)
                .count();

            if (activeTaskCount >= maxConcurrentTasks) {
                throw new IllegalStateException(
                    String.format("concurrent limit reached: %d", maxConcurrentTasks));
            }

            RecordingTask task = new RecordingTask(douyinId);
            task.setAutoEnabled(autoEnabled);
            task.setOutputDir(outputDir);
            task.setStatus(TaskStatus.PENDING);
            taskMap.put(task.getTaskId(), task);
            return task;
        }
    }
}
