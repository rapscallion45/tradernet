import axios from "axios"

const apiClient = axios.create({
  baseURL: "/tradernet", // will be proxied in dev via vite.config.ts
  withCredentials: true,
})

export default apiClient
