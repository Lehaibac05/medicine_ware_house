import { setAuthToken } from "./api"
import type { User } from "./types"

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
  refreshToken?: string
  user?: User
  forceChangePassword?: boolean
}

const API_BASE_URL = import.meta.env.VITE_API_URL ?? ""

export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  const url = `${API_BASE_URL}/auth/login`
  
  console.log("Login attempt:", {
    username: payload.username,
    url,
    API_BASE_URL
  })
  
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  })

  console.log("Login response:", {
    status: response.status,
    ok: response.ok,
    statusText: response.statusText
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    console.error("Login failed:", errorData)
    throw new Error(errorData.error || "Invalid username or password")
  }

  const data = (await response.json()) as LoginResponse
  
  console.log("Login successful, token received:", data.token ? "Yes" : "No")

  if (!data.token) {
    throw new Error("Login response is missing token")
  }

  // Save token to localStorage
  setAuthToken(data.token)

  return data
}
