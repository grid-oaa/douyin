import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const envBaseUrl = (env.VITE_API_BASE_URL || '').trim()
  const target = envBaseUrl.startsWith('http') ? envBaseUrl : 'http://localhost:8080'

  return {
    plugins: [vue()],
    test: {
      environment: 'jsdom',
    },
    server: {
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
        },
        '/actuator': {
          target,
          changeOrigin: true,
        },
      },
    },
  }
})
