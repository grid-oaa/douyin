import axios from 'axios'
import type { ErrorResponse } from '../types/recording'

export interface NormalizedError {
  message: string
  status?: number
  path?: string
  displayMessage: string
}

const statusMessageMap: Record<number, string> = {
  400: '输入格式错误，请检查抖音号或 Cookie',
  429: '任务过多，请稍后重试',
  503: '服务不可用，请检查网络或稍后再试',
}

function buildDisplayMessage(error: { message: string; status?: number; path?: string }): string {
  const preferred = error.status ? statusMessageMap[error.status] : undefined
  const baseMessage = preferred || error.message || '请求失败'
  if (error.status || error.path) {
    const statusPart = error.status ? `status: ${error.status}` : ''
    const pathPart = error.path ? `path: ${error.path}` : ''
    const detail = [statusPart, pathPart].filter(Boolean).join(', ')
    return `${baseMessage}（${detail}）`
  }
  return baseMessage
}

// 将后端错误与网络错误统一归一化，方便 UI 展示
export function normalizeError(error: unknown): NormalizedError {
  if (axios.isAxiosError(error)) {
    const response = error.response
    const data = response?.data as Partial<ErrorResponse> | undefined
    const normalized = {
      message: data?.message || error.message || '请求失败',
      status: data?.status || response?.status,
      path: data?.path,
    }
    return { ...normalized, displayMessage: buildDisplayMessage(normalized) }
  }

  if (error instanceof Error) {
    const normalized = { message: error.message }
    return { ...normalized, displayMessage: buildDisplayMessage(normalized) }
  }

  const normalized = { message: '未知错误' }
  return { ...normalized, displayMessage: buildDisplayMessage(normalized) }
}
