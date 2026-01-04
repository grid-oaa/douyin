<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import { formatDuration, formatFileSize } from '../utils/format'
import type { RecordingResponse, RecordingStatus } from '../types/recording'

interface Props {
  open: boolean
  recording?: RecordingResponse
  status?: RecordingStatus
}

defineProps<Props>()
const emit = defineEmits<{ (e: 'close'): void }>()

function handleClose() {
  emit('close')
}
</script>

<template>
  <div v-if="open" class="mask">
    <section class="panel">
      <header class="panel-header">
        <div class="panel-title">
          <span>任务详情</span>
          <StatusBadge v-if="recording" :status="recording.status" />
        </div>
        <button class="link-btn" type="button" @click="handleClose">关闭</button>
      </header>

      <div v-if="recording" class="panel-body">
        <div class="row">
          <span class="label">任务ID</span>
          <span class="value">{{ recording.taskId }}</span>
        </div>
        <div class="row">
          <span class="label">抖音号</span>
          <span class="value">{{ recording.douyinId }}</span>
        </div>
        <div class="row">
          <span class="label">输出路径</span>
          <span class="value">{{ recording.outputPath || '-' }}</span>
        </div>
        <div class="row">
          <span class="label">错误信息</span>
          <span class="value error">{{ recording.error || '-' }}</span>
        </div>

        <div class="divider"></div>

        <div class="row">
          <span class="label">时长</span>
          <span class="value">{{ formatDuration(status?.progress?.duration) }}</span>
        </div>
        <div class="row">
          <span class="label">文件大小</span>
          <span class="value">{{ formatFileSize(status?.progress?.fileSize) }}</span>
        </div>
        <div class="row">
          <span class="label">码率</span>
          <span class="value">{{ status?.progress?.bitrate || '-' }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
}

.panel {
  width: min(560px, 100%);
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.row {
  display: flex;
  gap: 12px;
}

.label {
  width: 72px;
  color: #6a6a6a;
}

.value {
  color: #2b2b2b;
  word-break: break-all;
}

.error {
  color: #b42318;
}

.divider {
  height: 1px;
  background: #eee;
  margin: 4px 0;
}

.link-btn {
  background: none;
  border: none;
  color: #2b65d9;
  cursor: pointer;
  padding: 0;
}
</style>
