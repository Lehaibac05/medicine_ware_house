import { apiFetch } from "./api"
import type { PageResponse } from "./types"

export type ActivityLogItem = {
  logId: number
  action: string
  targetObject: string
  timestamp: string
  ipAddress: string
  userId: number | null
  username: string | null
  fullName: string | null
}

export type GetActivityLogsParams = {
  page?: number
  size?: number
  action?: string
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

export const getActivityLogs = async (
  params: GetActivityLogsParams = {},
): Promise<PageResponse<ActivityLogItem>> => {
  const searchParams = new URLSearchParams()

  if (typeof params.page === "number") {
    searchParams.set("page", String(params.page))
  }
  if (typeof params.size === "number") {
    searchParams.set("size", String(params.size))
  }
  if (params.action?.trim()) {
    searchParams.set("action", params.action.trim())
  }

  const query = searchParams.toString()
  const endpoint = query ? `/admin/activity-logs?${query}` : "/admin/activity-logs"

  const response = await apiFetch<PageResponse<ActivityLogItem>>(endpoint)
  return normalizePage(response)
}
