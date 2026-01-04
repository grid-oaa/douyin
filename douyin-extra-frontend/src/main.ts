import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/base.css'

const app = createApp(App)

// 挂载路由与状态管理
app.use(createPinia())
app.use(router)
app.mount('#app')
