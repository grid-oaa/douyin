package com.douyin.liverecorder.service;

import com.douyin.liverecorder.model.RecordingTask;
import com.douyin.liverecorder.model.TaskStatus;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property tests for enforcing concurrent recording limits.
 */
class ConcurrentLimitPropertyTest {

    @Property(tries = 100)
    @Label("Reject tasks beyond the concurrent limit")
    void tasksBeyondConcurrentLimitShouldBeRejected(
            @ForAll @IntRange(min = 1, max = 5) int maxConcurrentTasks,
            @ForAll @IntRange(min = 1, max = 3) int extraTasks) {

        MockRecordingManager manager = new MockRecordingManager(maxConcurrentTasks);

        List<RecordingTask> tasks = new ArrayList<>();
        for (int i = 0; i < maxConcurrentTasks; i++) {
            RecordingTask task = manager.createTask("user" + i, false);
            task.setStatus(TaskStatus.RECORDING);
            tasks.add(task);
        }

        assertThat(tasks).hasSize(maxConcurrentTasks);

        for (int i = 0; i < extraTasks; i++) {
            final int index = i;
            assertThatThrownBy(() -> manager.createTask("extraUser" + index, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrent limit")
                .hasMessageContaining(String.valueOf(maxConcurrentTasks));
        }

        assertThat(manager.getTaskCount()).isEqualTo(maxConcurrentTasks);
    }

    @Property(tries = 100)
    @Label("Allow new tasks after some tasks complete")
    void newTasksCanBeCreatedAfterTasksComplete(
            @ForAll @IntRange(min = 2, max = 5) int maxConcurrentTasks,
            @ForAll @IntRange(min = 1, max = 2) int tasksToComplete) {

        int actualTasksToComplete = Math.min(tasksToComplete, maxConcurrentTasks);
        MockRecordingManager manager = new MockRecordingManager(maxConcurrentTasks);

        List<RecordingTask> tasks = new ArrayList<>();
        for (int i = 0; i < maxConcurrentTasks; i++) {
            RecordingTask task = manager.createTask("user" + i, false);
            task.setStatus(TaskStatus.RECORDING);
            tasks.add(task);
        }

        assertThatThrownBy(() -> manager.createTask("extraUser", false))
            .isInstanceOf(IllegalStateException.class);

        for (int i = 0; i < actualTasksToComplete; i++) {
            tasks.get(i).setStatus(TaskStatus.COMPLETED);
        }

        List<RecordingTask> newTasks = new ArrayList<>();
        for (int i = 0; i < actualTasksToComplete; i++) {
            RecordingTask newTask = manager.createTask("newUser" + i, false);
            newTask.setStatus(TaskStatus.RECORDING);
            newTasks.add(newTask);
        }

        assertThat(newTasks).hasSize(actualTasksToComplete);

        assertThatThrownBy(() -> manager.createTask("anotherExtraUser", false))
            .isInstanceOf(IllegalStateException.class);
    }

    @Property(tries = 100)
    @Label("Error message includes the limit count")
    void errorMessageShouldContainClearLimitInformation(
            @ForAll @IntRange(min = 1, max = 10) int maxConcurrentTasks) {

        MockRecordingManager manager = new MockRecordingManager(maxConcurrentTasks);

        for (int i = 0; i < maxConcurrentTasks; i++) {
            RecordingTask task = manager.createTask("user" + i, false);
            task.setStatus(TaskStatus.RECORDING);
        }

        assertThatThrownBy(() -> manager.createTask("extraUser", false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("concurrent limit")
            .hasMessageContaining(String.valueOf(maxConcurrentTasks));
    }

    @Property(tries = 100)
    @Label("Only active tasks count toward limit")
    void onlyActiveTasksShouldCountTowardsConcurrentLimit(
            @ForAll @IntRange(min = 2, max = 5) int maxConcurrentTasks) {

        MockRecordingManager manager = new MockRecordingManager(maxConcurrentTasks);

        List<RecordingTask> tasks = new ArrayList<>();
        int activeCount = maxConcurrentTasks / 2;
        for (int i = 0; i < activeCount; i++) {
            RecordingTask task = manager.createTask("activeUser" + i, false);
            task.setStatus(TaskStatus.RECORDING);
            tasks.add(task);
        }

        for (int i = 0; i < 5; i++) {
            RecordingTask task = manager.createTask("completedUser" + i, false);
            task.setStatus(TaskStatus.COMPLETED);
            tasks.add(task);
        }

        int remainingSlots = maxConcurrentTasks - activeCount;
        for (int i = 0; i < remainingSlots; i++) {
            RecordingTask task = manager.createTask("newUser" + i, false);
            assertThat(task).isNotNull();
        }

        assertThatThrownBy(() -> manager.createTask("extraUser", false))
            .isInstanceOf(IllegalStateException.class);
    }

    private static class MockRecordingManager {
        private final java.util.concurrent.ConcurrentHashMap<String, RecordingTask> taskMap =
            new java.util.concurrent.ConcurrentHashMap<>();
        private final int maxConcurrentTasks;

        public MockRecordingManager(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
        }

        public RecordingTask createTask(String douyinId, boolean autoEnabled) {
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
            task.setStatus(TaskStatus.PENDING);
            taskMap.put(task.getTaskId(), task);
            return task;
        }

        public int getTaskCount() {
            return taskMap.size();
        }
    }
}
