import type { RecordingStatusType } from '../types/recording'

export const recordingStatuses: RecordingStatusType[] = [
  'PENDING',
  'WAITING',
  'DETECTING',
  'RECORDING',
  'STOPPING',
  'COMPLETED',
  'FAILED',
  'CANCELLED',
]

export const statusColorMap: Record<RecordingStatusType, string> = {
  PENDING: 'gray',
  WAITING: 'gray',
  DETECTING: 'gray',
  RECORDING: 'green',
  STOPPING: 'yellow',
  COMPLETED: 'blue',
  FAILED: 'red',
  CANCELLED: 'red',
}

export const statusLabelMap: Record<RecordingStatusType, string> = {
  PENDING: '等待中',
  WAITING: '等待中',
  DETECTING: '检测中',
  RECORDING: '录制中',
  STOPPING: '停止中',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
}

const terminalStatuses: RecordingStatusType[] = ['COMPLETED', 'FAILED', 'CANCELLED']

export function isTerminalStatus(status: RecordingStatusType): boolean {
  return terminalStatuses.includes(status)
}
