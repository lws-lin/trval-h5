<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { saveLogin } from '../../utils/auth'
import { DEFAULT_MODEL, MODEL_OPTIONS } from '../../utils/models'

const router = useRouter()
const username = ref('')
const apiKey = ref('')
const model = ref(DEFAULT_MODEL)
const showModel = ref(false)

const modelLabel = computed(
  () => MODEL_OPTIONS.find((item) => item.value === model.value)?.text || '',
)

function onConfirmModel({ selectedOptions }) {
  model.value = selectedOptions[0]?.value || DEFAULT_MODEL
  showModel.value = false
}

function onSubmit() {
  const name = username.value.trim()
  const key = apiKey.value.trim()
  if (!name) {
    showToast('请输入用户名')
    return
  }
  if (!key) {
    showToast('请输入 API Key')
    return
  }
  saveLogin(name, key, model.value)
  router.replace('/index')
}
</script>

<template>
  <div class="login">
    <section class="card">
      <h1 class="card-title">登录后开始规划</h1>
      <p class="hint">用户名用于页面展示。API Key 和模型只保存在这台设备上，请求规划时会交给后端。</p>
      <van-field v-model="username" label="用户名" placeholder="请输入用户名" maxlength="20" />
      <van-field
        v-model="apiKey"
        label="API Key"
        type="text"
        placeholder="请输入 DeepSeek API Key"
      />
      <van-field
        :model-value="modelLabel"
        label="模型"
        readonly
        is-link
        placeholder="请选择模型"
        @click="showModel = true"
      />
      <van-popup v-model:show="showModel" round position="bottom">
        <van-picker
          :columns="MODEL_OPTIONS"
          :model-value="[model]"
          @confirm="onConfirmModel"
          @cancel="showModel = false"
        />
      </van-popup>
      <div class="action">
        <van-button block type="primary" @click="onSubmit">进入</van-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login {
  box-sizing: border-box;
  min-height: calc(100vh - 46px);
  padding: 12px;
  background: #f7f8fa;
}

.card {
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
}

.card-title {
  margin: 0;
  padding: 16px 16px 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.hint {
  margin: 8px 0 0;
  padding: 0 16px;
  color: #969799;
  font-size: 13px;
  line-height: 1.6;
}

.action {
  padding: 8px 16px 16px;
}
</style>
