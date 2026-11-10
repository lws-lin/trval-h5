<script setup>
import { fetchPlan } from '../../api/plan'
import { normalizeCollected, visibleText } from '../../utils/a2ui'
import { loadDraft, saveDraft } from '../../utils/draft'
import { findSaved, getPlan, savePlan } from '../../utils/plans'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const source = String(route.query.from || 'home')
const loading = ref(source === 'home')
const failed = ref(false)
const saved = ref(false)
const plan = ref(null)

const query = () => ({
  departText: String(route.query.departText || ''),
  departCode: String(route.query.departCode || ''),
  destText: String(route.query.destText || ''),
  destCode: String(route.query.destCode || ''),
  budget: String(route.query.budget || ''),
  days: String(route.query.days || ''),
  extra: String(route.query.extra || ''),
})

function chatRecord(draft) {
  if (!draft?.plan?.schedule?.length || !draft.collected?.departText || !draft.collected?.destText) return null
  return {
    ...draft.collected,
    id: draft.recordId || '',
    summary: draft.plan.summary,
    schedule: draft.plan.schedule,
    messages: (draft.messages || [])
      .map((item) => ({
        role: item.role,
        content: item.role === 'assistant' ? visibleText(item.content) : item.content,
      }))
      .filter((item) => item.content),
  }
}

function markSaved() {
  saved.value = source === 'mine' || Boolean(plan.value && findSaved(plan.value))
}

async function loadPlan() {
  failed.value = false
  if (source === 'mine') {
    plan.value = getPlan(String(route.query.id || ''))
    failed.value = !plan.value
    markSaved()
    return
  }
  if (source === 'chat') {
    plan.value = chatRecord(loadDraft())
    markSaved()
    return
  }

  const params = query()
  if (!params.departText || !params.destText) {
    plan.value = null
    return
  }
  loading.value = true
  try {
    const result = await fetchPlan(params)
    plan.value = { ...params, ...result, messages: [] }
    markSaved()
  } catch {
    failed.value = true
    plan.value = null
  } finally {
    loading.value = false
  }
}

function onSave() {
  if (!plan.value || saved.value) return
  const draft = source === 'chat' ? loadDraft() : null
  const stored = savePlan({
    ...plan.value,
    id: plan.value.id || draft?.recordId || '',
    messages: plan.value.messages || [],
  })
  if (source === 'chat' && draft) {
    saveDraft({ ...draft, recordId: stored.id })
  }
  plan.value = { ...plan.value, id: stored.id }
  saved.value = true
  showToast(draft?.recordId ? '已覆盖原来的记录' : '已保存到本机')
}

function continueChat() {
  const record = plan.value
  if (!record?.messages?.length) return
  const messages = record.messages.map((item, index) => ({
    id: index + 1,
    role: item.role,
    content: item.content,
  }))
  saveDraft({
    recordId: record.id,
    messages,
    collected: {
      ...normalizeCollected(),
      departText: record.departText || '',
      destText: record.destText || '',
      budget: record.budget || '',
      days: record.days || '',
      extra: record.extra || '',
    },
    plan: {
      summary: record.summary || '',
      schedule: record.schedule || [],
    },
  })
  router.replace({ name: 'chat' })
}

function leaveEmpty() {
  if (source === 'chat') {
    router.replace({ name: 'chat' })
    return
  }
  if (source === 'mine') {
    router.replace({ name: 'mine' })
    return
  }
  router.replace({ name: 'home' })
}

function displayBudget(value) {
  return value ? `${value} 元` : '未填写'
}

function displayDays(value) {
  return value ? `${value} 天` : '未填写'
}

if (source === 'home') onMounted(loadPlan)
else loadPlan()
</script>

<template>
  <div class="detail">
    <div v-if="loading" class="detail-status">
      <van-loading vertical>正在生成行程，请稍候</van-loading>
    </div>

    <div v-else-if="failed" class="detail-status">
      <van-empty :description="source === 'mine' ? '没有找到这条记录' : '行程生成失败'">
        <van-button v-if="source === 'home'" plain hairline color="#323233" @click="loadPlan">重新加载</van-button>
        <van-button v-else plain hairline color="#323233" @click="leaveEmpty">返回</van-button>
      </van-empty>
    </div>

    <div v-else-if="!plan" class="detail-status">
      <van-empty description="还没有可查看的行程">
        <van-button plain hairline color="#323233" @click="leaveEmpty">返回</van-button>
      </van-empty>
    </div>

    <template v-else>
      <section class="card">
        <h2 class="card-title">行程概要</h2>
        <p class="summary">{{ plan.summary }}</p>
        <van-cell title="出发地" :value="plan.departText" />
        <van-cell title="目的地" :value="plan.destText" />
        <van-cell title="预算" :value="displayBudget(plan.budget)" />
        <van-cell title="天数" :value="displayDays(plan.days)" />
        <van-cell v-if="plan.extra" title="其他需求" :value="plan.extra" />
      </section>

      <section v-for="item in plan.schedule" :key="item.day" class="card">
        <h2 class="card-title">第 {{ item.day }} 天 · {{ item.title }}</h2>
        <p class="day-content">{{ item.content }}</p>
      </section>

      <section v-if="plan.messages?.length" class="card">
        <h2 class="card-title">对话记录</h2>
        <p v-for="(item, index) in plan.messages" :key="index" class="day-content">
          {{ item.role === 'user' ? '我' : '助手' }}：{{ item.content }}
        </p>
      </section>

      <van-button
        v-if="source === 'mine' && plan.messages?.length"
        class="save"
        block
        plain
        hairline
        type="primary"
        @click="continueChat"
      >
        继续对话
      </van-button>
      <van-button
        v-if="source !== 'mine'"
        class="save"
        block
        plain
        hairline
        type="primary"
        :disabled="saved"
        @click="onSave"
      >
        {{ saved ? '已保存' : '保存行程规划' }}
      </van-button>
    </template>
  </div>
</template>

<style scoped>
.detail {
  box-sizing: border-box;
  min-height: calc(100vh - 46px);
  padding: 12px;
  background: #f7f8fa;
}

.detail-status {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 70px);
}

.card {
  margin-bottom: 12px;
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

.summary,
.day-content {
  margin: 0;
  padding: 0 16px 16px;
  color: #646566;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.detail :deep(.van-cell__value) {
  flex: 1.4;
  color: #323233;
}

.save {
  margin-top: 4px;
}
</style>
