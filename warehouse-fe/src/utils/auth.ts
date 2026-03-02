export const AUTH_TOKEN_KEY = "warehouse_auth_token"

export const getAuthToken = (): string | null => {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export const setAuthToken = (token: string): void => {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export const clearAuthToken = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
}
