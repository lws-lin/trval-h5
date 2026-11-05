import vue from '@vitejs/plugin-vue'
import { VantResolver } from '@vant/auto-import-resolver'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [VantResolver()],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [VantResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    server: {
      proxy: {
        '/api': {
          target: env.API_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,
          timeout: 0,
          proxyTimeout: 0,
        },
      },
    },
  }
})
