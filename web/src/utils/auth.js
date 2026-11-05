import { DEFAULT_MODEL } from './models'

const API_KEY = 'token'
const USERNAME = 'username'
const MODEL = 'model'

export function getApiKey() {
  return localStorage.getItem(API_KEY) || ''
}

export function getUsername() {
  return localStorage.getItem(USERNAME) || ''
}

export function getModel() {
  return localStorage.getItem(MODEL) || DEFAULT_MODEL
}

export function isLoggedIn() {
  return Boolean(getApiKey())
}

export function saveLogin(username, apiKey, model) {
  localStorage.setItem(USERNAME, username)
  localStorage.setItem(API_KEY, apiKey)
  localStorage.setItem(MODEL, model || DEFAULT_MODEL)
}

export function clearLogin() {
  localStorage.removeItem(USERNAME)
  localStorage.removeItem(API_KEY)
  localStorage.removeItem(MODEL)
}
