<script setup>
import ChatSurface from '../../components/chat/ChatSurface.vue'
import { streamChat } from '../../api/chat'
import { extractCollected, normalizeCollected, parseUi, visibleText } from '../../utils/a2ui'
import { loadDraft, saveDraft } from '../../utils/draft'
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const input = ref('')
const sending = ref(false)
const listRef = ref(null)
const storedDraft = loadDraft()

function createGreeting() {
  return {
    id: 1,
    role: 'assistant',
    content: '你好，我是旅游规划助手。告诉我出发地、目的地、预算或天数，我来帮你整理行程。其他话题我先不展开。',
    ui: {
      suggestions: ['想去成都', '预算 3000', '玩 3 天'],
      collected: normalizeCollected(),
    },
  }
}

const messages = ref(storedDraft?.messages?.length ? storedDraft.messages : [createGreeting()])
let messageId = messages.value.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1
const collected = ref({ ...normalizeCollected(), ...(storedDraft?.collected || {}) })
const plan = ref(storedDraft?.plan || null)
const recordId = ref(storedDraft?.recordId || '')

const lastAssistantId = computed(() => {
  const list = messages.value
  for (let index = list.length - 1; index >= 0; index -= 1) {
    if (list[index].role === 'assistant') return list[index].id
  }
  return null
})

const canPlan = computed(() => Boolean(collected.value.departText && collected.value.destText && plan.value?.schedule?.length))

watch([messages, collected, plan, recordId], () => {
  saveDraft({
    messages: messages.value,
    collected: collected.value,
    plan: plan.value,
    recordId: recordId.value,
  })
}, { deep: true })

function surfaceOf(item) {
  return item.ui || parseUi(item.content)
}

function mergeCollected(next) {
  const incoming = normalizeCollected(next)
  const current = { ...collected.value }
  Object.keys(current).forEach((key) => {
    if (incoming[key]) current[key] = incoming[key]
  })
  collected.value = current
}

function mergePlan(next) {
  if (next?.schedule?.length) plan.value = next
}

function absorbText(text) {
  mergeCollected(extractCollected(text))
}

function syncCollected() {
  messages.value.forEach((item) => {
    absorbText(item.role === 'assistant' ? visibleText(item.content) : item.content)
    const ui = parseUi(item.content)
    if (ui) {
      mergeCollected(ui.collected)
      mergePlan(ui.plan)
    }
  })
}

syncCollected()

async function scrollToBottom() {
  await nextTick()
  const list = listRef.value
  if (list) list.scrollTop = list.scrollHeight
}

async function send(text) {
  const content = (typeof text === 'string' ? text : input.value).trim()
  if (!content || sending.value) return

  messages.value.push({
    id: messageId++,
    role: 'user',
    content,
  })
  absorbText(content)
  input.value = ''
  const reply = {
    id: messageId++,
    role: 'assistant',
    content: '',
  }
  messages.value.push(reply)
  sending.value = true
  await scrollToBottom()

  const history = messages.value
    .filter((item) => item.id !== reply.id)
    .map((item) => ({
      role: item.role,
      content: item.role === 'assistant' ? visibleText(item.content) : item.content,
    }))
    .filter((item) => item.content)

  try {
    await streamChat({ messages: history, collected: collected.value }, (delta) => {
      reply.content += delta
      absorbText(visibleText(reply.content))
      const ui = parseUi(reply.content)
      if (ui) {
        mergeCollected(ui.collected)
        mergePlan(ui.plan)
      }
      scrollToBottom()
    })
    absorbText(visibleText(reply.content))
    const ui = parseUi(reply.content)
    if (ui) {
      mergeCollected(ui.collected)
      mergePlan(ui.plan)
    }
    if (!visibleText(reply.content)) reply.content = '我没有组织好回复，请再试一次。'
  } catch (error) {
    showToast(error.message || '回复失败了，请再试一次')
    if (!visibleText(reply.content)) reply.content = '回复失败了，请再试一次。'
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function resetConversation() {
  messages.value = [createGreeting()]
  messageId = 2
  collected.value = normalizeCollected()
  plan.value = null
  recordId.value = ''
  input.value = ''
}

async function startNew() {
  if (sending.value) return
  const hasConversation = messages.value.some((item) => item.role === 'user') || recordId.value || plan.value
  if (!hasConversation) {
    showToast('当前已经是新对话')
    return
  }
  try {
    await showConfirmDialog({
      title: '开启新对话',
      message: '当前对话会被清空。已经保存的行程记录还会留在「我的」里。',
      confirmButtonText: '开启新对话',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  resetConversation()
  await scrollToBottom()
}

function goPlan() {
  saveDraft({
    messages: messages.value,
    collected: collected.value,
    plan: plan.value,
    recordId: recordId.value,
  })
  router.push({ name: 'detail', query: { from: 'chat' } })
}

function onEnter(event) {
  if (event.shiftKey) return
  event.preventDefault()
  send()
}
</script>

<template>
  <section class="chat">
    <div class="chat-tools">
      <van-button
        class="new-chat"
        size="small"
        plain
        hairline
        type="primary"
        icon="plus"
        :disabled="sending"
        @click="startNew"
      >
        新对话
      </van-button>
    </div>
    <div ref="listRef" class="chat-list">
      <div
        v-for="item in messages"
        :key="item.id"
        class="chat-row"
        :class="item.role === 'user' ? 'is-user' : 'is-assistant'"
      >
        <div class="chat-bubble" :class="{ 'is-pending': item.role === 'assistant' && !visibleText(item.content) }">
          {{ visibleText(item.content) || (sending && item.id === lastAssistantId ? '正在回复...' : '') }}
        </div>
        <ChatSurface
          v-if="item.id === lastAssistantId"
          :collected="collected"
          :suggestions="surfaceOf(item)?.suggestions || []"
          :can-plan="canPlan"
          :disabled="sending"
          @pick="send"
          @plan="goPlan"
        />
      </div>
    </div>

    <form class="chat-bar" @submit.prevent="send">
      <van-field
        v-model="input"
        class="chat-field"
        type="textarea"
        rows="1"
        autosize
        placeholder="说说出发地、目的地或天数"
        :disabled="sending"
        @keydown.enter="onEnter"
      />
      <van-button
        class="chat-send"
        type="primary"
        size="small"
        native-type="submit"
        :disabled="!input.trim() || sending"
      >
        发送
      </van-button>
    </form>
  </section>
</template>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 46px - 50px);
  background: #f7f8fa;
}

.chat-tools {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px 0;
}

.new-chat {
  height: 28px;
  padding: 0 10px;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
}

.chat-row {
  display: flex;
  margin-bottom: 12px;
}

.chat-row.is-assistant {
  flex-direction: column;
  align-items: flex-start;
}

.chat-row.is-user {
  justify-content: flex-end;
}

.chat-bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.is-assistant .chat-bubble {
  background: #fff;
  color: #323233;
}

.is-user .chat-bubble {
  background: var(--van-primary-color);
  color: #fff;
}

.is-pending {
  color: #969799;
}

.chat-bar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-top: 1px solid #ebedf0;
}

.chat-field {
  flex: 1;
  padding: 0;
  background: transparent;
}

.chat-field :deep(.van-field__body) {
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 8px;
}

.chat-send {
  flex-shrink: 0;
  margin-bottom: 4px;
}
</style>
