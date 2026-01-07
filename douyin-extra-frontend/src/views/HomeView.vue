<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import CookiePanel from '../components/CookiePanel.vue'
import HealthPanel from '../components/HealthPanel.vue'
import RecordingCard from '../components/RecordingCard.vue'
import RecordingDetail from '../components/RecordingDetail.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { isTerminalStatus } from '../constants/recording'
import { useRecordingStore } from '../stores/recording'
import { getLastOutputDir } from '../utils/storage'

const store = useRecordingStore()

const douyinId = ref('')
const auto = ref(true)
const selectedTaskId = ref<string | null>(null)
const validationMessage = ref('')

const selectedRecording = computed(() =>
  store.list.find((item) => item.taskId === selectedTaskId.value),
)

const selectedStatus = computed(() =>
  selectedTaskId.value ? store.statusMap[selectedTaskId.value] : undefined,
)

const loading = computed(() => store.loading)
const isStartDisabled = computed(() => loading.value || !douyinId.value.trim())

async function pickOutputDir(): Promise<string | null> {
  // 使用上次目录作为默认选择
  const lastDir = getLastOutputDir()
  if (lastDir) {
    const reuse = window.confirm(`是否继续使用上次保存目录：${lastDir}？`)
    if (reuse) {
      return lastDir
    }
  }

  const picker = (window as Window & {
    showDirectoryPicker?: () => Promise<{ name: string }>
  }).showDirectoryPicker

  if (!picker) {
    validationMessage.value = '当前浏览器不支持目录选择'
    return null
  }

  try {
    const handle = await picker()
    return handle.name
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      validationMessage.value = '已取消选择保存目录'
    } else {
      validationMessage.value = '选择保存目录失败'
    }
    return null
  }
}

async function handleStart() {
  validationMessage.value = ''
  if (!douyinId.value.trim()) {
    validationMessage.value = '请输入抖音号'
    return
  }
  const outputDir = await pickOutputDir()
  if (!outputDir) {
    return
  }
  await store.startRecording(douyinId.value.trim(), auto.value, outputDir)
}

function handleOpen(taskId: string) {
  selectedTaskId.value = taskId
  store.startTaskPolling(taskId, 3000)
}

function handleCloseDetail() {
  selectedTaskId.value = null
}

function handleStop(taskId: string) {
  store.stopRecording(taskId)
}

function refreshList() {
  store.fetchList()
}

watch(
  () => store.list,
  (list) => {
    list.forEach((item) => {
      if (!isTerminalStatus(item.status)) {
        store.startTaskPolling(item.taskId, 3000)
      } else {
        store.stopTaskPolling(item.taskId)
      }
    })
  },
  { deep: true },
)

onMounted(() => {
  store.fetchList()
  store.startListPolling(15000)
})

onBeforeUnmount(() => {
  store.stopAllPolling()
})
</script>

<template>
  <section class="page">
    <div class="section-header">
      <div>
        <h2 class="section-title">录制任务总览</h2>
        <p class="section-tip">管理录制任务、查看状态与详情。</p>
      </div>
      <button class="ghost-btn" type="button" :disabled="loading" @click="refreshList">
        {{ loading ? '刷新中..' : '刷新列表' }}
      </button>
    </div>

    <div v-if="store.error" class="error-banner">{{ store.error }}</div>

    <form class="start-form" @submit.prevent="handleStart">
      <label class="field">
        <span class="field-label">抖音号</span>
        <input
          v-model="douyinId"
          class="field-input"
          :class="{ invalid: Boolean(validationMessage) }"
          placeholder="请输入抖音号"
        />
        <span v-if="validationMessage" class="field-error">{{ validationMessage }}</span>
      </label>
      <label class="field checkbox">
        <input v-model="auto" type="checkbox" />
        <span>自动录制</span>
      </label>
      <button class="primary-btn" type="submit" :disabled="isStartDisabled">开始录制</button>
    </form>

    <div class="list-grid">
      <RecordingCard
        v-for="item in store.list"
        :key="item.taskId"
        :recording="item"
        :disable-stop="isTerminalStatus(item.status)"
        @stop="handleStop"
        @open="handleOpen"
      />
    </div>

    <RecordingDetail
      :open="Boolean(selectedTaskId)"
      :recording="selectedRecording"
      :status="selectedStatus"
      @close="handleCloseDetail"
    />

    <section class="status-legend">
      <h3>状态映射</h3>
      <div class="legend-list">
        <StatusBadge status="PENDING" />
        <StatusBadge status="WAITING" />
        <StatusBadge status="DETECTING" />
        <StatusBadge status="RECORDING" />
        <StatusBadge status="STOPPING" />
        <StatusBadge status="COMPLETED" />
        <StatusBadge status="FAILED" />
        <StatusBadge status="CANCELLED" />
      </div>
    </section>

    <HealthPanel />
    <CookiePanel />
  </section>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.section-tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6a6a6a;
}

.start-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  background: #fff;
  border: 1px solid #e6e6e6;
  padding: 12px;
  border-radius: 10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  color: #6a6a6a;
}

.field-input {
  min-width: 220px;
  padding: 8px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
}

.field-input.invalid {
  border-color: #f5b5b5;
}

.field-error {
  font-size: 12px;
  color: #b42318;
}

.checkbox {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.primary-btn {
  background: #2b65d9;
  color: #fff;
  border: none;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.ghost-btn {
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
}

.list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.error-banner {
  background: #ffe8e8;
  color: #b42318;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #f5b5b5;
}

.status-legend {
  border-radius: 12px;
  border: 1px dashed #d8d8d8;
  padding: 12px;
  background: #fff;
}

.legend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 720px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .start-form {
    flex-direction: column;
    align-items: stretch;
  }

  .field-input {
    width: 100%;
  }
}
</style>




