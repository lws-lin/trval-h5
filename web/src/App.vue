<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function onBack() {
  if (route.name === 'detail') {
    const from = route.query.from
    if (from === 'mine') {
      router.replace({ name: 'mine' })
      return
    }
    if (from === 'home') {
      router.replace({ name: 'home' })
      return
    }
    router.replace({ name: 'chat' })
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.replace('/index')
}
</script>

<template>
  <div class="layout" :class="{ 'layout--sub': route.meta.hideTabbar }">
    <van-nav-bar
      :title="route.meta.navTitle || '旅游规划'"
      :left-arrow="!!route.meta.showBack"
      fixed
      placeholder
      safe-area-inset-top
      @click-left="onBack"
    />
    <main class="layout-main">
      <router-view />
    </main>
    <van-tabbar v-if="!route.meta.hideTabbar" route placeholder safe-area-inset-bottom>
      <van-tabbar-item replace to="/index" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/chat" icon="chat-o">对话</van-tabbar-item>
      <van-tabbar-item replace to="/mine" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>
