import request from '../utils/request'

export function fetchMine() {
  return request.get('/mine')
}
