import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

import 'element-plus/dist/index.css'
import '@/assets/styles/index.scss'

/**
 * 创建应用实例
 */
const app = createApp(App)

/**
 * 注册 Element Plus 图标
 */
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

/**
 * 使用 Pinia 状态管理
 */
app.use(createPinia())

/**
 * 使用 Vue Router
 */
app.use(router)

/**
 * 使用 Element Plus
 */
app.use(ElementPlus, {
  locale: zhCn
})

/**
 * 挂载应用
 */
app.mount('#app')
