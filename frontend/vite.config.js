// vite.config.ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
  host: true,         
  port: 3000,
  strictPort: true,
  proxy: {
    "/api": {
      target: process.env.BACKEND_URL || "http://backend:8080",
      changeOrigin: true,
      
      rewrite: (path) => path.replace(/^\/api/, ""),
    },
  },
  watch: {
    usePolling: true, // nécessaire sous Docker Desktop
    interval: 100,
  },
}

});
