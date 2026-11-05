import { getUsername } from './auth'

function draftKey() {
  return `trval-chat-draft:${getUsername() || '旅行用户'}`
}

export function loadDraft() {
  try {
    const raw = sessionStorage.getItem(draftKey())
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function saveDraft(draft) {
  sessionStorage.setItem(draftKey(), JSON.stringify(draft))
}
