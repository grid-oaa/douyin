import { describe, expect, it, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { statusColorMap, statusLabelMap } from '../src/constants/recording'
import RecordingDetail from '../src/components/RecordingDetail.vue'
import RecordingCard from '../src/components/RecordingCard.vue'
import CookiePanel from '../src/components/CookiePanel.vue'
import HealthPanel from '../src/components/HealthPanel.vue'

vi.mock('../src/services/api', () => ({
  apiStartRecording: vi.fn(),
  apiGetStatus: vi.fn(),
  apiListRecordings: vi.fn(),
  apiStopRecording: vi.fn(),
  apiUpdateCookie: vi.fn(),
  apiHealth: vi.fn(),
}))

vi.mock('../src/services/polling', () => {
  class PollingController {
    static instance: PollingController | null = null
    startListPolling = vi.fn()
    stopListPolling = vi.fn()
    startTaskPolling = vi.fn()
    stopTaskPolling = vi.fn()
    stopAll = vi.fn()

    constructor() {
      PollingController.instance = this
    }
  }
  return { PollingController }
})

const sampleRecording = {
  taskId: 'task-1',
  douyinId: 'user-1',
  status: 'RECORDING',
  streamUrl: null,
  outputPath: '/tmp/output.mp4',
  startTime: '2026-01-03T14:30:25',
  endTime: null,
  fileSize: 1024,
  error: null,
}

describe('属性测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('属性1: 任务创建一致性', async () => {
    const api = await import('../src/services/api')
    const { useRecordingStore } = await import('../src/stores/recording')
    vi.mocked(api.apiStartRecording).mockResolvedValue(sampleRecording)

    const store = useRecordingStore()
    await store.startRecording('user-1', true)

    expect(store.list[0]?.taskId).toBe('task-1')
    expect(store.statusMap['task-1']?.status).toBe('RECORDING')
  })

  it('属性2: 终态任务停止轮询', async () => {
    const api = await import('../src/services/api')
    const { useRecordingStore } = await import('../src/stores/recording')
    const { PollingController } = await import('../src/services/polling')
    vi.mocked(api.apiGetStatus).mockResolvedValue({
      taskId: 'task-2',
      status: 'COMPLETED',
      progress: { duration: 120 },
    })

    const store = useRecordingStore()
    await store.fetchStatus('task-2')

    expect(PollingController.instance?.stopTaskPolling).toHaveBeenCalledWith('task-2')
  })

  it('属性3: 任务详情完整性', () => {
    const wrapper = mount(RecordingDetail, {
      props: {
        open: true,
        recording: sampleRecording,
        status: {
          taskId: 'task-1',
          status: 'RECORDING',
          progress: { duration: 120, fileSize: 2048, bitrate: '1024kbps' },
        },
      },
    })

    expect(wrapper.text()).toContain('task-1')
    expect(wrapper.text()).toContain('user-1')
    expect(wrapper.text()).toContain('/tmp/output.mp4')
    expect(wrapper.text()).toContain('1024kbps')
  })

  it('属性4: 停止操作安全性', () => {
    const wrapper = mount(RecordingCard, {
      props: {
        recording: sampleRecording,
        disableStop: true,
      },
    })
    const button = wrapper.get('button.danger-btn')
    expect((button.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('属性5: Cookie 更新可靠性', async () => {
    const api = await import('../src/services/api')
    vi.mocked(api.apiUpdateCookie).mockResolvedValue()

    const wrapper = mount(CookiePanel)
    await wrapper.find('textarea').setValue('cookie=abc')
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('更新成功')
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toBe('')
  })

  it('属性6: 状态映射一致性', () => {
    const statuses = [
      'PENDING',
      'WAITING',
      'DETECTING',
      'RECORDING',
      'STOPPING',
      'COMPLETED',
      'FAILED',
      'CANCELLED',
    ]
    statuses.forEach((status) => {
      expect(statusColorMap[status as keyof typeof statusColorMap]).toBeTruthy()
      expect(statusLabelMap[status as keyof typeof statusLabelMap]).toBeTruthy()
    })
  })

  it('属性7: 健康检查可用性', async () => {
    const api = await import('../src/services/api')
    vi.mocked(api.apiHealth).mockResolvedValue({ status: 'UP' })

    const wrapper = mount(HealthPanel)
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('UP')
  })
})
