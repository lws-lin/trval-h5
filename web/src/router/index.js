import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录', navTitle: '登录', hideTabbar: true },
  },
  {
    path: '/',
    redirect: '/index',
  },
  {
    path: '/index',
    name: 'home',
    component: () => import('../views/home/index.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/chat/index.vue'),
    meta: { title: '对话' },
  },
  {
    path: '/mine',
    name: 'mine',
    component: () => import('../views/mine/index.vue'),
    meta: { title: '我的' },
  },
  {
    path: '/detail',
    name: 'detail',
    component: () => import('../views/detail/index.vue'),
    meta: { title: '行程详情', navTitle: '行程详情', showBack: true, hideTabbar: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.name !== 'login' && !isLoggedIn()) {
    return { name: 'login' }
  }
  if (to.name === 'login' && isLoggedIn()) {
    return { name: 'home' }
  }
})

export default router
