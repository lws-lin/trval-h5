<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearLogin, getUsername } from '../../utils/auth'
import { listPlans } from '../../utils/plans'

const router = useRouter()
const records = ref([])
const displayName = computed(() => getUsername() || '旅行用户')

function cityName(text = '') {
  return text.split('/').pop().trim() || text
}

function recordTitle(record) {
  return `${cityName(record.departText)} → ${cityName(record.destText)}`
}

function recordLabel(record) {
  const budget = record.budget ? `预算 ${record.budget} 元` : '预算未填写'
  const days = record.days ? `${record.days} 天` : '天数未填写'
  return `${days} · ${budget} · ${record.createdAt}`
}

function loadMine() {
  records.value = listPlans()
}

function logout() {
  clearLogin()
  router.replace({ name: 'login' })
}

function openRecord(record) {
  router.push({
    name: 'detail',
    query: {
      from: 'mine',
      id: record.id,
    },
  })
}

onMounted(loadMine)
</script>

<template>
  <div class="mine">
      <section class="profile">
        <div class="avatar" aria-hidden="true">
          <van-icon name="user-o" />
        </div>
        <h1 class="name">{{ displayName }}</h1>
        <p class="meta">已规划 {{ records.length }} 次</p>
      </section>

      <section class="card">
        <h2 class="card-title">规划记录</h2>
        <van-empty v-if="!records.length" description="还没有规划记录" />
        <template v-else>
          <van-cell
            v-for="record in records"
            :key="record.id"
            :title="recordTitle(record)"
            :label="recordLabel(record)"
            is-link
            @click="openRecord(record)"
          />
        </template>
      </section>

      <van-button class="logout" block plain hairline color="#323233" @click="logout">退出登录</van-button>
  </div>
</template>

<style scoped>
.mine {
  box-sizing: border-box;
  min-height: calc(100vh - 46px - 50px);
  padding: 12px;
  background: #f7f8fa;
}

.mine-status {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
}

.profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 12px;
  padding: 28px 16px 24px;
  background: #fff;
  border-radius: 12px;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #f2f3f5;
  color: #646566;
  font-size: 36px;
}

.name {
  margin: 12px 0 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.meta {
  margin: 4px 0 0;
  color: #969799;
  font-size: 13px;
  line-height: 1.5;
}

.card {
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
}

.card-title {
  margin: 0;
  padding: 16px 16px 8px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.logout {
  margin-top: 12px;
}
</style>
