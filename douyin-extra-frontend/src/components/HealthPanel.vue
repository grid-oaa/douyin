<script setup lang="ts">
import { ref } from 'vue'
import { apiHealth } from '../services/api'
import type { NormalizedError } from '../services/error'
import type { HealthResponse } from '../types/recording'

const loading = ref(false)
const error = ref('')
const result = ref<HealthResponse | null>(null)

async function handleCheck() {
  loading.value = true
  error.value = ''
  try {
    result.value = await apiHealth()
  } catch (err) {
    const normalized = err as NormalizedError
    error.value = normalized.displayMessage || '健康检查失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="panel">
    <div class="panel-header">
      <h2 class="panel-title">健康检查</h2>
      <button class="ghost-btn" type="button" :disabled="loading" @click="handleCheck">
        {{ loading ? '检查中...' : '发起检查' }}
      </button>
    </div>
    <p class="panel-tip">用于确认后端服务与依赖组件可用性。</p>
    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="result" class="result">
      <div class="row">
        <span class="label">状态</span>
        <span class="value">{{ result.status }}</span>
      </div>
      <div v-if="result.components" class="components">
        <div
          v-for="(component, key) in result.components"
          :key="key"
          class="row"
        >
          <span class="label">{{ key }}</span>
          <span class="value">{{ component.status }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  border-radius: 12px;
  border: 1px solid #e6e6e6;
  background: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.panel-title {
  margin: 0;
  font-size: 16px;
}

.panel-tip {
  margin: 0;
  font-size: 12px;
  color: #6a6a6a;
}

.ghost-btn {
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  padding: 6px 12px;
  cursor: pointer;
}

.error {
  background: #ffe8e8;
  color: #b42318;
  border: 1px solid #f5b5b5;
  padding: 8px 12px;
  border-radius: 8px;
}

.result {
  border-top: 1px solid #eee;
  padding-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.row {
  display: flex;
  gap: 12px;
}

.label {
  width: 64px;
  color: #6a6a6a;
}
</style>
