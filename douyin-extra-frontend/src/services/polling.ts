export type PollingHandler = () => void | Promise<void>
export type TaskPollingHandler = (taskId: string) => void | Promise<void>

export class PollingController {
  private listTimerId: number | null = null
  private taskTimerMap = new Map<string, number>()

  startListPolling(intervalMs: number, handler: PollingHandler): void {
    this.stopListPolling()
    this.listTimerId = window.setInterval(() => {
      void handler()
    }, intervalMs)
  }

  stopListPolling(): void {
    if (this.listTimerId !== null) {
      window.clearInterval(this.listTimerId)
      this.listTimerId = null
    }
  }

  startTaskPolling(taskId: string, intervalMs: number, handler: TaskPollingHandler): void {
    this.stopTaskPolling(taskId)
    const timerId = window.setInterval(() => {
      void handler(taskId)
    }, intervalMs)
    this.taskTimerMap.set(taskId, timerId)
  }

  stopTaskPolling(taskId: string): void {
    const timerId = this.taskTimerMap.get(taskId)
    if (timerId !== undefined) {
      window.clearInterval(timerId)
      this.taskTimerMap.delete(taskId)
    }
  }

  stopAll(): void {
    this.stopListPolling()
    this.taskTimerMap.forEach((timerId) => window.clearInterval(timerId))
    this.taskTimerMap.clear()
  }
}
