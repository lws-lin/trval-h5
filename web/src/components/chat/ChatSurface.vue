<script setup>
import { hasCollected } from '../../utils/a2ui'

const props = defineProps({
  collected: {
    type: Object,
    required: true,
  },
  suggestions: {
    type: Array,
    default: () => [],
  },
  canPlan: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['pick', 'plan'])

const fields = [
  { key: 'departText', label: '出发地' },
  { key: 'destText', label: '目的地' },
  { key: 'budget', label: '预算', suffix: ' 元' },
  { key: 'days', label: '天数', suffix: ' 天' },
  { key: 'extra', label: '其他需求' },
]

function fieldText(field) {
  const value = props.collected[field.key]
  return value ? `${value}${field.suffix || ''}` : ''
}
</script>

<template>
  <div class="surface">
    <section v-if="hasCollected(collected)" class="card">
      <h3 class="card-title">已确认的行程信息</h3>
      <van-cell
        v-for="field in fields"
        v-show="collected[field.key]"
        :key="field.key"
        :title="field.label"
        :value="fieldText(field)"
      />
    </section>

    <div v-if="suggestions.length && !disabled" class="chips">
      <button
        v-for="item in suggestions"
        :key="item"
        type="button"
        class="chip"
        @click="emit('pick', item)"
      >
        {{ item }}
      </button>
    </div>

    <van-button
      v-if="canPlan"
      class="plan"
      block
      plain
      hairline
      type="primary"
      :disabled="disabled"
      @click="emit('plan')"
    >
      生成行程
    </van-button>
  </div>
</template>

<style scoped>
.surface {
  width: min(100%, 320px);
  margin-top: 8px;
}

.card {
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
}

.card-title {
  margin: 0;
  padding: 12px 16px 4px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.chip {
  margin: 0;
  padding: 6px 12px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: #fff;
  color: #323233;
  font: inherit;
  font-size: 13px;
  line-height: 20px;
}

.plan {
  margin-top: 8px;
}

.surface :deep(.van-cell__value) {
  flex: 1.4;
  color: #323233;
}
</style>
