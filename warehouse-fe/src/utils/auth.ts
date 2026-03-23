export const AUTH_TOKEN_KEY = "warehouse_auth_token"
export const FORCE_CHANGE_PASSWORD_KEY = "warehouse_force_change_password"
export const CURRENT_USER_KEY = "warehouse_current_user"
export const AUTH_STATE_CHANGE_EVENT = "warehouse-auth-state-change"

export type CurrentUserProfile = {
  userId: number
  username: string
  fullName: string
  email: string
  status: string
  lastLogin: string | null
  roleId: number | null
  roleName: string | null
}

export type AppRole =
  | "ROLE_ADMIN"
  | "ROLE_WAREHOUSE_MANAGER"
  | "ROLE_WAREHOUSE_STAFF"
  | "ROLE_ACCOUNTANT"
  | "ROLE_REQUESTER"
  | string

export const getAuthToken = (): string | null => {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export const setAuthToken = (token: string): void => {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  window.dispatchEvent(new Event(AUTH_STATE_CHANGE_EVENT))
}

export const clearAuthToken = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(FORCE_CHANGE_PASSWORD_KEY)
  localStorage.removeItem(CURRENT_USER_KEY)
  window.dispatchEvent(new Event(AUTH_STATE_CHANGE_EVENT))
}

export const setCurrentUserProfile = (user: CurrentUserProfile | null): void => {
  if (!user) {
    localStorage.removeItem(CURRENT_USER_KEY)
    window.dispatchEvent(new Event(AUTH_STATE_CHANGE_EVENT))
    return
  }

  localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new Event(AUTH_STATE_CHANGE_EVENT))
}

export const getCurrentUserProfile = (): CurrentUserProfile | null => {
  const raw = localStorage.getItem(CURRENT_USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as CurrentUserProfile
  } catch {
    return null
  }
}

export const setForceChangePasswordRequired = (required: boolean): void => {
  if (required) {
    localStorage.setItem(FORCE_CHANGE_PASSWORD_KEY, "true")
    window.dispatchEvent(new Event(AUTH_STATE_CHANGE_EVENT))
    return
  }

  localStorage.removeItem(FORCE_CHANGE_PASSWORD_KEY)
  window.dispatchEvent(new Event(AUTH_STATE_CHANGE_EVENT))
}

export const isForceChangePasswordRequired = (): boolean => {
  return localStorage.getItem(FORCE_CHANGE_PASSWORD_KEY) === "true"
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
  if (!role) return "Khách"

  if (role === "ROLE_ADMIN") return "Quản trị viên"
  if (role === "ROLE_WAREHOUSE_MANAGER") return "Quản lý kho"
  if (role === "ROLE_WAREHOUSE_STAFF") return "Nhân viên kho"
  if (role === "ROLE_ACCOUNTANT") return "Kế toán"
  if (role === "ROLE_REQUESTER") return "Người yêu cầu"

  return role.replace(/^ROLE_/, "").replace(/_/g, " ")
}

export const getUserName = (): string => {
  const currentUser = getCurrentUserProfile()
  if (currentUser?.fullName?.trim()) {
    return currentUser.fullName.trim()
  }

  const token = getAuthToken()
  if (!token) return ""

  const payload = parseJwtPayload(token)
  if (!payload) return ""

  return (
    (payload.name as string) ||
    (payload.username as string) ||
    (payload.sub as string) ||
    ""
  )
}
