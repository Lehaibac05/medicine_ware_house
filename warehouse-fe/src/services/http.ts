import axios from "axios"
import { AUTH_TOKEN_KEY } from "../utils/auth"

const apiBaseURL = import.meta.env.VITE_API_URL ?? ""

export const http = axios.create({
  baseURL: apiBaseURL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default http
