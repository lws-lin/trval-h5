import { apiBaseUrl } from '../config'
import { getApiKey, getModel } from '../utils/auth'

export async function streamChat(params, onEvent) {
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  const apiKey = getApiKey()
  if (apiKey) {
    headers.Authorization = `Bearer ${apiKey}`
    headers['X-Model'] = getModel()
  }

  const response = await fetch(`${apiBaseUrl}/chat`, {
    method: 'POST',
    headers,
    body: JSON.stringify(params),
  })
  const type = response.headers.get('content-type') || ''
  if (type.includes('application/json')) {
    const body = await response.json()
    throw new Error(body.message || '回复失败了，请再试一次')
  }
  if (!response.ok || !response.body) {
    throw new Error('网络异常')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    parts.forEach((part) => emitPart(part, onEvent))
  }
  if (buffer.trim()) emitPart(buffer, onEvent)
}

function emitPart(part, onEvent) {
  const line = part.split('\n').find((item) => item.startsWith('data:'))
  if (!line) return
  const payload = JSON.parse(line.slice(5).trim())
  if (payload.error) throw new Error(payload.error)
  if (payload.delta) onEvent(payload.delta)
}
