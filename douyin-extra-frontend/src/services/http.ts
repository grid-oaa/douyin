import axios from 'axios'
import { normalizeError } from './error'
import { logError } from './logger'
import { reportError } from './monitoring'

const baseURL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').trim()

// 统一的 HTTP 客户端，后续在此扩展拦截器与错误归一化
const http = axios.create({
  baseURL,
  timeout: 15000,
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const normalized = normalizeError(error)
    const method = error?.config?.method?.toUpperCase?.()
    const url = error?.config?.url
    logError(normalized.displayMessage, { status: normalized.status, path: normalized.path, method, url })
    void reportError({ ...normalized, method, url })
    return Promise.reject(normalized)
  },
)

export default http
