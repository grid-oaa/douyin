export interface LogContext {
  status?: number
  path?: string
  method?: string
  url?: string
}

export function logError(message: string, context?: LogContext): void {
  // 关键请求失败日志记录
  console.error('[douyin-extra] 请求失败', { message, ...context })
}

export function logInfo(message: string, context?: Record<string, unknown>): void {
  console.info('[douyin-extra] 信息', { message, ...context })
}
