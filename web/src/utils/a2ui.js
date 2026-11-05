export const A2UI_MARKER = '[[a2ui]]'

const COLLECTED_FIELDS = ['departText', 'destText', 'budget', 'days', 'extra']

export function visibleText(raw = '') {
  const text = String(raw)
  const index = text.indexOf(A2UI_MARKER)
  if (index >= 0) return text.slice(0, index).trim()
  for (let size = A2UI_MARKER.length - 1; size > 0; size -= 1) {
    if (text.endsWith(A2UI_MARKER.slice(0, size))) return text.slice(0, -size)
  }
  return text
}

const FIELD_ALIASES = {
  departText: ['departText', 'depart', 'from', '出发地', '出发城市'],
  destText: ['destText', 'dest', 'to', '目的地'],
  budget: ['budget', '预算'],
  days: ['days', 'day', '天数'],
  extra: ['extra', '其他需求', '其他'],
}

export function parseUi(raw = '') {
  const text = String(raw)
  const index = text.lastIndexOf(A2UI_MARKER)
  if (index < 0) return null
  const json = extractJsonObject(text.slice(index + A2UI_MARKER.length))
  if (!json) return null
  try {
    const data = JSON.parse(json)
    const collected = data?.collected && typeof data.collected === 'object' ? data.collected : data
    return {
      suggestions: normalizeSuggestions(data?.suggestions),
      collected: normalizeCollected(collected),
      plan: normalizePlan(data?.plan),
    }
  } catch {
    return null
  }
}

export function normalizeCollected(value) {
  const source = value && typeof value === 'object' ? value : {}
  return Object.fromEntries(COLLECTED_FIELDS.map((key) => [key, pickField(source, FIELD_ALIASES[key])]))
}

function pickField(source, keys) {
  for (const key of keys) {
    const value = source[key]
    if (value != null && String(value).trim()) return String(value).trim()
  }
  return ''
}

function extractJsonObject(text) {
  const cleaned = String(text).replace(/```json/gi, '').replace(/```/g, '')
  const start = cleaned.indexOf('{')
  if (start < 0) return null
  let depth = 0
  let inString = false
  let escaped = false
  for (let index = start; index < cleaned.length; index += 1) {
    const char = cleaned[index]
    if (inString) {
      if (escaped) escaped = false
      else if (char === '\\') escaped = true
      else if (char === '"') inString = false
      continue
    }
    if (char === '"') inString = true
    else if (char === '{') depth += 1
    else if (char === '}') {
      depth -= 1
      if (depth === 0) return cleaned.slice(start, index + 1)
    }
  }
  return null
}

export function normalizePlan(value) {
  if (!value || typeof value !== 'object') return null
  const summary = String(value.summary || '').trim()
  const schedule = Array.isArray(value.schedule)
    ? value.schedule
        .map((item, index) => ({
          day: Number(item?.day) || index + 1,
          title: String(item?.title || '').trim(),
          content: String(item?.content || '').trim(),
        }))
        .filter((item) => item.title && item.content)
    : []
  if (!summary && !schedule.length) return null
  return { summary, schedule }
}

const CITY = '[\\u4e00-\\u9fa5]{2,7}?'
const CITY_TAIL = '(?=记下|出发|玩|，|。|？|\\?|$)'

export function extractCollected(text) {
  const raw = String(text || '').replace(/\s+/g, '')
  const collected = normalizeCollected()
  collected.departText = matchCity(raw, [
    new RegExp(`从(${CITY})出发`),
    new RegExp(`出发地(?:是|为|[:：])?(${CITY})${CITY_TAIL}`),
    new RegExp(`(${CITY})出发`),
  ])
  collected.destText = matchCity(raw, [
    new RegExp(`(?:想去|要去|去往|目的地(?:是|为|[:：])?)(${CITY})${CITY_TAIL}`),
    new RegExp(`去(${CITY})${CITY_TAIL}`),
  ])
  if (collected.destText && collected.destText === collected.departText) collected.destText = ''
  const days = raw.match(/(\d{1,2})天/)
  if (days) {
    const count = Number(days[1])
    if (count >= 1 && count <= 15) collected.days = String(count)
  }
  const budget = raw.match(/预算(?:大概|大约|是|为|[:：])?(\d{2,7})/) || raw.match(/(\d{3,7})元/)
  if (budget) collected.budget = budget[1]
  const extra = raw.match(/(?:其他需求(?:是|为|[:：])?|希望)([^，。？?]{2,40})/)
  if (extra) collected.extra = extra[1]
  return collected
}

function matchCity(text, patterns) {
  for (const pattern of patterns) {
    const matched = text.match(pattern)
    const city = acceptCity(matched?.[1])
    if (city) return city
  }
  return ''
}

function acceptCity(name) {
  if (!name || /哪个|什么|哪里|哪儿|怎么|多少|几/.test(name)) return ''
  return name
}

export function hasCollected(collected) {
  return COLLECTED_FIELDS.some((key) => collected?.[key])
}

function normalizeSuggestions(value) {
  if (!Array.isArray(value)) return []
  return value.map((item) => String(item || '').trim()).filter(Boolean).slice(0, 4)
}
