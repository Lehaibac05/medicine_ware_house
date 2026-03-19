export const AUTH_TOKEN_KEY = "warehouse_auth_token"

export type AppRole =
  | "ROLE_ADMIN"
  | "ROLE_WAREHOUSE_MANAGER"
  | "ROLE_WAREHOUSE_STAFF"
  | "ROLE_ACCOUNTANT"
  | "ROLE_SUPPLIER"
  | string

export const getAuthToken = (): string | null => {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export const setAuthToken = (token: string): void => {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export const clearAuthToken = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
}

const parseJwtPayload = (token: string): Record<string, unknown> | null => {
  try {
    const parts = token.split(".")
    if (parts.length < 2) return null

    const normalized = parts[1].replace(/-/g, "+").replace(/_/g, "/")
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4)
    const payload = atob(padded)
    return JSON.parse(payload) as Record<string, unknown>
  } catch {
    return null
  }
}

export const getUserRoles = (): AppRole[] => {
  const token = getAuthToken()
  if (!token) return []

  const payload = parseJwtPayload(token)
  if (!payload) return []

  const rawRoles = payload.roles
  if (!Array.isArray(rawRoles)) return []

  return rawRoles.filter((role): role is AppRole => typeof role === "string")
}

export const hasAnyRole = (roles: AppRole[]): boolean => {
  const userRoles = getUserRoles()
  return roles.some((role) => userRoles.includes(role))
}

export const getPrimaryRole = (): AppRole | null => {
  const roles = getUserRoles()
  return roles[0] ?? null
}

export const getRoleLabel = (role: AppRole | null): string => {
  if (!role) return "Guest"

  if (role === "ROLE_ADMIN") return "Admin"
  if (role === "ROLE_WAREHOUSE_MANAGER") return "Warehouse Manager"
  if (role === "ROLE_WAREHOUSE_STAFF") return "Warehouse Staff"
  if (role === "ROLE_ACCOUNTANT") return "Accountant"
  if (role === "ROLE_SUPPLIER") return "Supplier"

  return role.replace(/^ROLE_/, "").replace(/_/g, " ")
}
