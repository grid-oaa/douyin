import type { NormalizedError } from './error'

export interface ErrorReportPayload extends NormalizedError {
  method?: string
  url?: string
}

// 预留错误上报接口位置，后续对接监控平台
export async function reportError(payload: ErrorReportPayload): Promise<void> {
  if (import.meta.env.DEV) {
    console.info('[douyin-extra] error-report placeholder', payload)
  }
}
