<script setup>
import { useCascaderAreaData } from '@vant/area-data'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const areaOptions = useCascaderAreaData().map((province) => ({
  text: province.text,
  value: province.value,
  children: (province.children || []).map((city) => ({
    text: city.text,
    value: city.value,
  })),
}))

const form = ref({
  departText: '',
  departCode: '',
  destText: '',
  destCode: '',
  budget: '',
  days: '',
})

const showArea = ref(false)
const areaTarget = ref('depart')
const cascaderValue = ref('')

const areaTitle = computed(() =>
  areaTarget.value === 'depart' ? '选择出发城市' : '选择目的地',
)

const canStart = computed(() => Boolean(form.value.departCode && form.value.destCode))

function onStartPlan() {
  if (!form.value.departCode && !form.value.destCode) {
    showToast('请选择出发地和目的地')
    return
  }
  if (!form.value.departCode) {
    showToast('请选择出发地')
    return
  }
  if (!form.value.destCode) {
    showToast('请选择目的地')
    return
  }

  router.push({
    name: 'detail',
    query: {
      departText: form.value.departText,
      departCode: form.value.departCode,
      destText: form.value.destText,
      destCode: form.value.destCode,
      budget: form.value.budget,
      days: form.value.days,
      from: 'home',
    },
  })
}

const hotCities = [
  { name: '北京', province: '北京市', text: '北京市 / 北京市', code: '110100' },
  { name: '上海', province: '上海市', text: '上海市 / 上海市', code: '310100' },
  { name: '广州', province: '广东省', text: '广东省 / 广州市', code: '440100' },
  { name: '深圳', province: '广东省', text: '广东省 / 深圳市', code: '440300' },
  { name: '杭州', province: '浙江省', text: '浙江省 / 杭州市', code: '330100' },
  { name: '成都', province: '四川省', text: '四川省 / 成都市', code: '510100' },
  { name: '西安', province: '陕西省', text: '陕西省 / 西安市', code: '610100' },
  { name: '重庆', province: '重庆市', text: '重庆市 / 重庆市', code: '500100' },
]

function openArea(target) {
  areaTarget.value = target
  cascaderValue.value = target === 'depart' ? form.value.departCode : form.value.destCode
  showArea.value = true
}

function onFinish({ selectedOptions, value }) {
  const text = selectedOptions.map((item) => item.text).join(' / ')
  if (areaTarget.value === 'depart') {
    form.value.departText = text
    form.value.departCode = value
  } else {
    form.value.destText = text
    form.value.destCode = value
  }
  showArea.value = false
}

function pickHotCity(city) {
  form.value.destText = city.text
  form.value.destCode = city.code
  showToast(`已选择目的地：${city.name}`)
}
</script>

<template>
  <div class="home">
    <section class="card">
      <h2 class="card-title">规划你的旅程</h2>
      <van-cell-group :border="false">
        <van-field
          :model-value="form.departText"
          readonly
          is-link
          label="出发地"
          label-width="4.2em"
          placeholder="请选择出发地"
          @click="openArea('depart')"
        />
        <van-field
          :model-value="form.destText"
          readonly
          is-link
          label="目的地"
          label-width="4.2em"
          placeholder="请选择目的地"
          @click="openArea('dest')"
        />
        <van-field
          v-model="form.budget"
          type="digit"
          label="预算"
          label-width="4.2em"
          placeholder="请输入预算"
          maxlength="8"
        >
          <template #extra>元</template>
        </van-field>
        <van-field
          v-model="form.days"
          type="digit"
          label="天数"
          label-width="4.2em"
          placeholder="请输入天数"
          maxlength="3"
        >
          <template #extra>天</template>
        </van-field>
        <div class="plan-action">
          <div class="plan-border" :class="{ 'is-ready': canStart }">
            <van-button class="plan-button" plain block @click="onStartPlan">
              开始规划
            </van-button>
          </div>
        </div>
      </van-cell-group>
    </section>

    <section class="card">
      <h2 class="card-title">快捷入口</h2>
      <van-grid :column-num="2" :border="false" clickable>
        <van-grid-item icon="chat-o" text="对话" to="/chat" />
        <van-grid-item icon="user-o" text="我的" to="/mine" />
      </van-grid>
    </section>

    <section class="card">
      <h2 class="card-title">热门城市</h2>
      <div class="city-tags">
        <button
          v-for="city in hotCities"
          :key="city.code"
          type="button"
          class="city-tag"
          :class="{ 'is-active': form.destCode === city.code }"
          @click="pickHotCity(city)"
        >
          {{ city.name }}
        </button>
      </div>
    </section>

    <van-popup v-model:show="showArea" round position="bottom">
      <van-cascader
        :key="areaTarget"
        v-model="cascaderValue"
        :title="areaTitle"
        :options="areaOptions"
        @close="showArea = false"
        @finish="onFinish"
      />
    </van-popup>
  </div>
</template>

<style scoped>
.home {
  box-sizing: border-box;
  min-height: calc(100vh - 46px - 50px);
  padding: 12px;
  background: #f7f8fa;
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

.plan-action {
  padding: 8px 16px 16px;
}

.plan-border {
  position: relative;
  overflow: hidden;
  padding: 1.5px;
  border-radius: var(--van-button-radius);
  background: #dcdee0;
}

.plan-border.is-ready {
  background: var(--van-primary-color);
}

.plan-border.is-ready::before {
  content: '';
  position: absolute;
  inset: -120%;
  background: conic-gradient(
    var(--van-primary-color) 0deg,
    var(--van-primary-color) 300deg,
    #fff 330deg,
    var(--van-primary-color) 360deg
  );
  animation: border-spin 1.8s linear infinite;
}

.plan-border.is-ready :deep(.plan-button) {
  color: var(--van-primary-color);
}

.plan-border :deep(.plan-button) {
  position: relative;
  z-index: 1;
  border: none;
  background: #fff;
  color: #c8c9cc;
}

@keyframes border-spin {
  to {
    transform: rotate(360deg);
  }
}

.city-tags {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 4px 16px 16px;
}

.city-tag {
  margin: 0;
  padding: 6px 0;
  border: 1px solid transparent;
  border-radius: 999px;
  background: #f2f3f5;
  color: #323233;
  font: inherit;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
}

.city-tag.is-active {
  border-color: var(--van-primary-color);
  background: #fff;
  color: var(--van-primary-color);
}

.home :deep(.van-field__control) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
