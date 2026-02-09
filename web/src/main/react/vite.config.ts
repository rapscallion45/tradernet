import { defineConfig, loadEnv } from "vite"
import react from "@vitejs/plugin-react"
import viteTsconfigPaths from "vite-tsconfig-paths"

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "")
  const serverBaseUrl = env.VITE_SERVER_BASE_URL
  const proxyUrl = serverBaseUrl?.startsWith("http") ? new URL(serverBaseUrl) : null
  const proxyTarget = proxyUrl ? `${proxyUrl.protocol}//${proxyUrl.host}` : "http://localhost:8080"
  const proxyBasePath = proxyUrl
    ? (proxyUrl.pathname.replace(/\/$/, "") === "/" ? "" : proxyUrl.pathname.replace(/\/$/, ""))
    : "/api"

  return {
    plugins: [react(), viteTsconfigPaths()],
    server: {
      port: 3000,
      proxy: {
        "/api": {
          target: proxyTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => (proxyBasePath === "/api" ? path : path.replace(/^\/api/, proxyBasePath)),
        },
      },
    },
  }
})
