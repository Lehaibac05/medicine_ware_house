import { apiFetch } from "./api"
import type { PageResponse, User } from "./types"

export type GetUsersParams = {
  page?: number
  size?: number
  search?: string
  sortBy?: "username" | "email" | "fullName" | "status"
  sortDir?: "asc" | "desc"
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

  return apiFetch<PageResponse<User>>(endpoint)
}

export type CreateUserPayload = {
  username: string
  fullName: string
  email: string
  status: string
  roleId?: number
}

export const createUser = async (
  payload: CreateUserPayload
): Promise<User> => {
  return apiFetch<User>("/users", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}
