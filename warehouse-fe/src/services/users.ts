import { apiFetch } from "./api"
import type { PageResponse, User } from "./types"

export type GetUsersParams = {
  page?: number
  size?: number
  search?: string
  sortBy?: "username" | "email" | "fullName" | "status"
  sortDir?: "asc" | "desc"
}

const normalizePage = <T>(page: PageResponse<T> | T[]): PageResponse<T> => {
  if (Array.isArray(page)) {
    const size = page.length
    return {
      content: page,
      totalElements: page.length,
      totalPages: page.length > 0 ? 1 : 0,
      size,
      number: 0,
    }
  }

  const content = Array.isArray(page.content) ? page.content : []
  return {
    ...page,
    content,
    totalElements: page.totalElements ?? content.length,
    totalPages: page.totalPages ?? (content.length > 0 ? 1 : 0),
    size: page.size ?? content.length,
    number: page.number ?? 0,
  }
}

export const getUsers = async (
  params: GetUsersParams = {}
): Promise<PageResponse<User>> => {
  const searchParams = new URLSearchParams()

  if (typeof params.page === "number") {
    searchParams.set("page", String(params.page))
  }
  if (typeof params.size === "number") {
    searchParams.set("size", String(params.size))
  }
  if (params.search?.trim()) {
    searchParams.set("search", params.search.trim())
  }
  if (params.sortBy) {
    searchParams.set("sortBy", params.sortBy)
  }
  if (params.sortDir) {
    searchParams.set("sortDir", params.sortDir)
  }

  const query = searchParams.toString()
  const endpoint = query ? `/users?${query}` : "/users"

  const response = await apiFetch<PageResponse<User>>(endpoint)
  return normalizePage(response)
}

export type CreateUserPayload = {
  username: string
  fullName: string
  email: string
  status: string
  roleId?: number
}

export type UpdateUserPayload = {
  fullName?: string
  email?: string
  status?: string
  password?: string
  roleId?: number
}

export type ChangePasswordPayload = {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export const createUser = async (
  payload: CreateUserPayload
): Promise<User> => {
  return apiFetch<User>("/users", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export const updateUser = async (
  userId: number,
  payload: UpdateUserPayload,
): Promise<User> => {
  return apiFetch<User>(`/users/${userId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  })
}

export const changePassword = async (
  payload: ChangePasswordPayload,
): Promise<{ message: string }> => {
  return apiFetch<{ message: string }>("/users/change-password", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export type ForceChangePasswordPayload = {
  newPassword: string
  confirmPassword: string
}

export const forceChangePassword = async (
  payload: ForceChangePasswordPayload,
): Promise<{ message: string }> => {
  return apiFetch<{ message: string }>("/users/force-change-password", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}
