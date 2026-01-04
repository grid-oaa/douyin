import http from './http'
import type {
  HealthResponse,
  RecordingResponse,
  RecordingStatus,
  StartRecordingRequest,
  UpdateCookieRequest,
} from '../types/recording'

export function apiStartRecording(payload: StartRecordingRequest): Promise<RecordingResponse> {
  return http.post<RecordingResponse>('/api/recordings/start', payload).then((res) => res.data)
}

export function apiStopRecording(taskId: string): Promise<RecordingResponse> {
  return http.post<RecordingResponse>(`/api/recordings/${taskId}/stop`).then((res) => res.data)
}

export function apiGetStatus(taskId: string): Promise<RecordingStatus> {
  return http.get<RecordingStatus>(`/api/recordings/${taskId}/status`).then((res) => res.data)
}

export function apiListRecordings(): Promise<RecordingResponse[]> {
  return http.get<RecordingResponse[]>('/api/recordings').then((res) => res.data)
}

export function apiUpdateCookie(payload: UpdateCookieRequest): Promise<void> {
  return http.post<void>('/api/config/cookie', payload).then(() => undefined)
}

export function apiHealth(): Promise<HealthResponse> {
  return http.get<HealthResponse>('/actuator/health').then((res) => res.data)
}
