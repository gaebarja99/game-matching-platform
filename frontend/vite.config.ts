import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
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
});
