import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as Icons from '@element-plus/icons-vue'
import { autoAnimatePlugin } from '@formkit/auto-animate/vue'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import './styles/tokens.css'

const app = createApp(App)
for (const [k, v] of Object.entries(Icons)) app.component(k, v)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(autoAnimatePlugin)

const theme = useThemeStore()
theme.apply()

app.mount('#app')
