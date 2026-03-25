import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // npm run build 하면 Spring Boot static 폴더로 바로 빌드
    outDir: '../GameMatcher/src/main/resources/static',
    emptyOutDir: true,
  },
})
