import { defineStore } from 'pinia'
import { apiGetStatus, apiListRecordings, apiStartRecording, apiStopRecording } from '../services/api'
import type { NormalizedError } from '../services/error'
import { PollingController } from '../services/polling'
import { isTerminalStatus } from '../constants/recording'
import type { RecordingResponse, RecordingStatus } from '../types/recording'
import { setLastOutputDir } from '../utils/storage'

interface RecordingState {
  list: RecordingResponse[]
  statusMap: Record<string, RecordingStatus>
  loading: boolean
  error?: string
}

const polling = new PollingController()

export const useRecordingStore = defineStore('recording', {
  state: (): RecordingState => ({
    list: [],
    statusMap: {},
    loading: false,
    error: undefined,
  }),
  actions: {
    setError(error?: string) {
      this.error = error
    },
    mergeStatus(status: RecordingStatus) {
      this.statusMap[status.taskId] = status
      const index = this.list.findIndex((item) => item.taskId === status.taskId)
      if (index >= 0) {
        this.list[index] = { ...this.list[index], status: status.status, error: status.error ?? null }
      }
    },
    async fetchList() {
      this.loading = true
      this.setError(undefined)
      try {
        const list = await apiListRecordings()
        this.list = list
        list.forEach((item) => {
          this.statusMap[item.taskId] = {
            taskId: item.taskId,
            status: item.status,
            error: item.error ?? null,
          }
        })
      } catch (error) {
        const normalized = error as NormalizedError
        this.setError(normalized.displayMessage)
      } finally {
        this.loading = false
      }
    },
    async fetchStatus(taskId: string) {
      this.setError(undefined)
      try {
        const status = await apiGetStatus(taskId)
        this.mergeStatus(status)
        if (isTerminalStatus(status.status)) {
          polling.stopTaskPolling(taskId)
        }
      } catch (error) {
        const normalized = error as NormalizedError
        this.setError(normalized.displayMessage)
      }
    },
    async startRecording(douyinId: string, auto: boolean, outputDir: string) {
      this.setError(undefined)
      if (!outputDir || !outputDir.trim()) {
        this.setError('请选择保存目录')
        return
      }
      try {
        const response = await apiStartRecording({ douyinId, auto, outputDir })
        this.list = [response, ...this.list.filter((item) => item.taskId !== response.taskId)]
        this.statusMap[response.taskId] = {
          taskId: response.taskId,
          status: response.status,
          error: response.error ?? null,
        }
        setLastOutputDir(outputDir)
      } catch (error) {
        const normalized = error as NormalizedError
        this.setError(normalized.displayMessage)
      }
    },
    async stopRecording(taskId: string) {
      this.setError(undefined)
      try {
        const response = await apiStopRecording(taskId)
        this.list = this.list.map((item) =>
          item.taskId === response.taskId ? { ...item, ...response } : item,
        )
        this.statusMap[response.taskId] = {
          taskId: response.taskId,
          status: response.status,
          error: response.error ?? null,
        }
      } catch (error) {
        const normalized = error as NormalizedError
        this.setError(normalized.displayMessage)
      }
    },
    startListPolling(intervalMs = 15000) {
      polling.startListPolling(intervalMs, () => this.fetchList())
    },
    stopListPolling() {
      polling.stopListPolling()
    },
    startTaskPolling(taskId: string, intervalMs = 3000) {
      polling.startTaskPolling(taskId, intervalMs, (id) => this.fetchStatus(id))
    },
    stopTaskPolling(taskId: string) {
      polling.stopTaskPolling(taskId)
    },
    stopAllPolling() {
      polling.stopAll()
    },
  },
})
