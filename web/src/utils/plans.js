import { getUsername } from './auth'

const PLANS_KEY = 'trval-plans'

function currentUser() {
  return getUsername() || '旅行用户'
}

function readAll() {
  try {
    const raw = localStorage.getItem(PLANS_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      const all = { [currentUser()]: parsed.map((item) => ({ ...item, username: currentUser() })) }
      localStorage.setItem(PLANS_KEY, JSON.stringify(all))
      return all
    }
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function writeAll(all) {
  localStorage.setItem(PLANS_KEY, JSON.stringify(all))
}

export function listPlans() {
  const list = readAll()[currentUser()]
  return Array.isArray(list) ? list : []
}

export function getPlan(id) {
  return listPlans().find((item) => item.id === id) || null
}

export function findSaved(record) {
  const key = fingerprint(record)
  return listPlans().find((item) => fingerprint(item) === key) || null
}

export function savePlan(record) {
  const username = currentUser()
  const all = readAll()
  const list = Array.isArray(all[username]) ? all[username] : []
  const now = formatNow()
  if (record.id) {
    const index = list.findIndex((item) => item.id === record.id)
    if (index >= 0) {
      const next = { ...list[index], ...record, username, id: record.id, createdAt: now }
      const rest = list.filter((item) => item.id !== record.id)
      all[username] = [next, ...rest]
      writeAll(all)
      return next
    }
  }
  const stored = { ...record, username, id: `${Date.now()}`, createdAt: now }
  all[username] = [stored, ...list]
  writeAll(all)
  return stored
}

function fingerprint(record) {
  return JSON.stringify({
    departText: record?.departText || '',
    destText: record?.destText || '',
    budget: record?.budget || '',
    days: record?.days || '',
    extra: record?.extra || '',
    summary: record?.summary || '',
    schedule: record?.schedule || [],
    messages: record?.messages || [],
  })
}

function formatNow() {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
}
