import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Proxy all API calls to backend
      "/auth": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
      "/medicines": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
      "/batches": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
      "/warehouses": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
      "/orders": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
      "/alerts": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
    },
  },
})
