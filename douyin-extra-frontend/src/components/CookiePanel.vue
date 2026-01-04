<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiUpdateCookie } from '../services/api'
import type { NormalizedError } from '../services/error'

const cookie = ref('')
const showCookie = ref(false)
const loading = ref(false)
const message = ref('')
const storageKey = 'douyin-extra-cookie'

onMounted(() => {
  const saved = window.localStorage.getItem(storageKey)
  if (saved) {
    cookie.value = saved
  }
})

async function handleSubmit() {
  message.value = ''
  if (!cookie.value.trim()) {
    message.value = '请输入 Cookie'
    return
  }
  loading.value = true
  try {
    await apiUpdateCookie({ cookie: cookie.value.trim() })
    message.value = '更新成功'
    window.localStorage.setItem(storageKey, cookie.value.trim())
    cookie.value = ''
  } catch (error) {
    const normalized = error as NormalizedError
    message.value = normalized.displayMessage || '更新失败'
  } finally {
    loading.value = false
  }
}

function handleClearLocal() {
  window.localStorage.removeItem(storageKey)
  cookie.value = ''
  message.value = '已清空本地 Cookie'
}
</script>

<template>
  <section class="panel">
    <h2 class="panel-title">运行时 Cookie 更新</h2>
    <p class="panel-tip">请谨慎填写，Cookie 属于敏感信息，避免在公共环境粘贴。</p>
    <textarea
      v-model="cookie"
      class="cookie-input"
      :class="{ masked: !showCookie }"
      rows="3"
      placeholder="请输入抖音 Cookie"
    ></textarea>
    <div class="panel-actions">
      <label class="toggle">
        <input v-model="showCookie" type="checkbox" />
        <span>显示 Cookie</span>
      </label>
      <button class="primary-btn" type="button" :disabled="loading" @click="handleSubmit">
        {{ loading ? '提交中...' : '更新 Cookie' }}
      </button>
      <button class="primary-btn" type="button" @click="handleClearLocal">清空本地 Cookie</button>
      <span class="message">{{ message }}</span>
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
  gap: 12px;
}

.panel-title {
  margin: 0;
  font-size: 16px;
}

.panel-tip {
  margin: 0;
  color: #6a6a6a;
  font-size: 12px;
}

.cookie-input {
  resize: vertical;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #d0d0d0;
  font-size: 13px;
}

.cookie-input.masked {
  -webkit-text-security: disc;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #5a5a5a;
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

.message {
  font-size: 12px;
  color: #3a3a3a;
}
</style>
