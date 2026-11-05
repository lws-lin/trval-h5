import request from '../utils/request'

export function fetchPlan(params) {
  return request.post('/plan', params, { timeout: 90000 })
}
