import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// 前端 dev server(:5173);/api 代理至 Spring Boot 後端(:8080),含 SSE 串流。
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
