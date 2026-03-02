export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
}

const API_BASE_URL = import.meta.env.VITE_API_URL ?? ""

export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw new Error("Invalid username or password")
  }

  const data = (await response.json()) as LoginResponse

  if (!data.token) {
    throw new Error("Login response is missing token")
  }

  return data
}
