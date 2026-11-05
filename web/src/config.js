// 接口前缀。开发默认 /api，由 Vite 转到本机后端。
// 部署时改 web/.env.production 里的 VITE_API_BASE_URL，改完需要重新构建。
export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
