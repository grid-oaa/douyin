export type RecordingStatusType =
  | 'PENDING'
  | 'WAITING'
  | 'DETECTING'
  | 'RECORDING'
  | 'STOPPING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'

export interface RecordingResponse {
  taskId: string
  douyinId: string
  status: RecordingStatusType
  streamUrl?: string | null
  outputPath?: string | null
  startTime: string
  endTime?: string | null
  fileSize?: number | null
  error?: string | null
}

export interface RecordingProgress {
  duration?: number
  fileSize?: number
  bitrate?: string
}

export interface RecordingStatus {
  taskId: string
  status: RecordingStatusType
  progress?: RecordingProgress
  error?: string | null
}

export interface StartRecordingRequest {
  douyinId: string
  auto?: boolean
}

export interface UpdateCookieRequest {
  cookie: string
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

export interface HealthResponse {
  status: string
  components?: Record<string, { status: string }>
}
