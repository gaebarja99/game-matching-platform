import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client references `global` in the browser, so alias it.
    global: 'globalThis',
  },
  server: {
    // Allow access from both localhost and 127.0.0.1.
    host: true,
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
