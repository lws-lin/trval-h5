import { createApp } from 'vue'
import { Locale } from 'vant'
import zhCN from 'vant/es/locale/lang/zh-CN'
import App from './App.vue'
import router from './router'
import './style.css'

Locale.use('zh-CN', zhCN)

createApp(App).use(router).mount('#app')
