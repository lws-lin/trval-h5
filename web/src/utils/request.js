import axios from 'axios'
import { showToast } from 'vant'
import { apiBaseUrl } from '../config'
import { getModel } from './auth'

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      config.headers['X-Model'] = getModel()
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      showToast(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body?.data ?? body
  },
  (error) => {
    const message = error.code === 'ECONNABORTED'
      ? '请求超时，请稍后再试'
      : error.response?.data?.message || error.message || '网络异常'
    showToast(message)
    return Promise.reject(error)
  },
)

export default request
