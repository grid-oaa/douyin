const LAST_OUTPUT_DIR_KEY = 'mp4save.lastOutputDir'

export function getLastOutputDir(): string | null {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    return window.localStorage.getItem(LAST_OUTPUT_DIR_KEY)
  } catch {
    return null
  }
}

export function setLastOutputDir(outputDir: string): void {
  if (!outputDir || typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.setItem(LAST_OUTPUT_DIR_KEY, outputDir)
  } catch {
    return
  }
}
