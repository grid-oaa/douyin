<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import { formatFileSize } from '../utils/format'
import type { RecordingResponse } from '../types/recording'

interface Props {
  recording: RecordingResponse
  disableStop?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'stop', taskId: string): void
  (e: 'open', taskId: string): void
}>()

function handleStop() {
  emit('stop', props.recording.taskId)
}

function handleOpen() {
  emit('open', props.recording.taskId)
}
</script>

<template>
  <article class="card">
    <header class="card-header">
      <div class="card-title">
        <span class="card-id">抖音号：{{ recording.douyinId }}</span>
        <StatusBadge :status="recording.status" />
      </div>
      <button class="link-btn" type="button" @click="handleOpen">详情</button>
    </header>

    <div class="card-body">
      <div class="card-row">
        <span class="label">任务ID</span>
        <span class="value">{{ recording.taskId }}</span>
      </div>
      <div class="card-row">
        <span class="label">文件大小</span>
        <span class="value">{{ formatFileSize(recording.fileSize ?? undefined) }}</span>
      </div>
      <div class="card-row">
        <span class="label">输出路径</span>
        <span class="value">{{ recording.outputPath || '-' }}</span>
      </div>
      <div v-if="recording.error" class="card-row error">
        <span class="label">错误</span>
        <span class="value">{{ recording.error }}</span>
      </div>
    </div>

    <footer class="card-footer">
      <button class="danger-btn" type="button" :disabled="disableStop" @click="handleStop">
        停止录制
      </button>
    </footer>
  </article>
</template>

<style scoped>
.card {
  border-radius: 12px;
  border: 1px solid #e6e6e6;
  background: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.card-id {
  font-weight: 600;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.card-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.label {
  width: 64px;
  color: #6a6a6a;
}

.value {
  color: #2b2b2b;
  word-break: break-all;
}

.error .value {
  color: #b42318;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

.danger-btn {
  background: #ffe2e2;
  border: 1px solid #f5b5b5;
  color: #a11616;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.danger-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.link-btn {
  background: none;
  border: none;
  color: #2b65d9;
  cursor: pointer;
  padding: 0;
}
</style>
