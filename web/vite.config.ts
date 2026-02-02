import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import viteTsconfigPaths from "vite-tsconfig-paths"

export default defineConfig({
  plugins: [react(), viteTsconfigPaths()],
  resolve: {
    alias: [{ find: /^components\/(.*)/, replacement: "/src/components/$1" }],
  },
  server: {
    port: 3000,
    proxy: {
      "/tradernet": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
