import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client 등이 Node의 global을 참조해 브라우저에서 "global is not defined" 발생 방지
    global: 'globalThis',
  },
  server: {
    // localhost(::1)뿐 아니라 127.0.0.1(IPv4)로도 접속 가능하도록
    host: true,
    proxy: {
      // Windows에서 localhost → IPv6(::1)만 시도해 ECONNREFUSED 나는 경우가 있어 IPv4 고정
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/img': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
