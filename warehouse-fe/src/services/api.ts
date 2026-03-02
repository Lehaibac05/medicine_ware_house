// API helper functions
import { AUTH_TOKEN_KEY } from '../utils/auth'

const API_BASE_URL = import.meta.env.VITE_API_URL ?? ""

export class ApiError extends Error {
  status: number
  statusText: string

  constructor(
    message: string,
    status: number,
    statusText: string
  ) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.statusText = statusText
  }
}

export const getAuthToken = (): string | null => {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export const setAuthToken = (token: string): void => {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export const removeAuthToken = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
}

export const apiFetch = async <T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> => {
  const token = getAuthToken()

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  }

  // Merge with provided headers
  if (options.headers) {
    const providedHeaders = options.headers as Record<string, string>
    Object.assign(headers, providedHeaders)
  }

  if (token) {
    headers["Authorization"] = `Bearer ${token}`
    console.log("🔑 Sending request with token:", token.substring(0, 20) + "...")
  } else {
    console.warn("⚠️ No token found in localStorage")
  }

  console.log("📡 API Request:", endpoint, "Method:", options.method || "GET")

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    throw new ApiError(
      `API Error: ${response.statusText}`,
      response.status,
      response.statusText
    )
  }

  // Handle empty responses (e.g., DELETE)
  const contentType = response.headers.get("content-type")
  if (!contentType || !contentType.includes("application/json")) {
    return null as T
  }

  return (await response.json()) as T
}
