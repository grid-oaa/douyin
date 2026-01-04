import { describe, expect, it, beforeEach, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

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
    startListPolling = vi.fn()
    stopListPolling = vi.fn()
    startTaskPolling = vi.fn()
    stopTaskPolling = vi.fn()
    stopAll = vi.fn()
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

const completedRecording = {
  ...sampleRecording,
  status: 'COMPLETED',
}

function findButtonByText(wrapper: ReturnType<typeof mount>, text: string) {
  return wrapper.findAll('button').find((btn) => btn.text().trim() === text)
}

describe('集成流程测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('11.1 录制任务创建到详情打开流程', async () => {
    const api = await import('../src/services/api')
    vi.mocked(api.apiListRecordings).mockResolvedValue([])
    vi.mocked(api.apiStartRecording).mockResolvedValue(sampleRecording)

    const { default: HomeView } = await import('../src/views/HomeView.vue')
    const wrapper = mount(HomeView, {
      global: { plugins: [createPinia()] },
    })

    await flushPromises()

    await wrapper.find('input.field-input').setValue('user-1')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('user-1')
    expect(wrapper.text()).toContain('task-1')

    const detailButton = findButtonByText(wrapper, '详情')
    await detailButton?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('任务详情')
    expect(wrapper.text()).toContain('task-1')
  })

  it('11.2 停止任务与状态更新流程', async () => {
    const api = await import('../src/services/api')
    vi.mocked(api.apiListRecordings).mockResolvedValue([sampleRecording])
    vi.mocked(api.apiStopRecording).mockResolvedValue(completedRecording)

    const { default: HomeView } = await import('../src/views/HomeView.vue')
    const wrapper = mount(HomeView, {
      global: { plugins: [createPinia()] },
    })

    await flushPromises()

    const stopButton = findButtonByText(wrapper, '停止录制')
    await stopButton?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('已完成')
  })

  it('11.3 Cookie 更新成功流程', async () => {
    const api = await import('../src/services/api')
    vi.mocked(api.apiListRecordings).mockResolvedValue([])
    vi.mocked(api.apiUpdateCookie).mockResolvedValue()

    const { default: HomeView } = await import('../src/views/HomeView.vue')
    const wrapper = mount(HomeView, {
      global: { plugins: [createPinia()] },
    })

    await flushPromises()

    const textarea = wrapper.find('textarea.cookie-input')
    await textarea.setValue('cookie=abc')
    const updateButton = findButtonByText(wrapper, '更新 Cookie')
    await updateButton?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('更新成功')
  })

  it('11.4 健康检查展示流程', async () => {
    const api = await import('../src/services/api')
    vi.mocked(api.apiListRecordings).mockResolvedValue([])
    vi.mocked(api.apiHealth).mockResolvedValue({ status: 'UP' })

    const { default: HomeView } = await import('../src/views/HomeView.vue')
    const wrapper = mount(HomeView, {
      global: { plugins: [createPinia()] },
    })

    await flushPromises()

    const checkButton = findButtonByText(wrapper, '发起检查')
    await checkButton?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('UP')
  })
})
