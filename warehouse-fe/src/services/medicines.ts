import { apiFetch } from "./api"
import type { Medicine, PageResponse } from "./types"

export type { Medicine }

export type GetMedicinesParams = {
  page?: number
  size?: number
  search?: string
  manufacturer?: string
  storageCondition?: string
  sortBy?: "name" | "manufacturer" | "storageCondition"
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

export const getMedicines = async (
  params: GetMedicinesParams = {}
): Promise<PageResponse<Medicine>> => {
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
  if (params.manufacturer?.trim()) {
    searchParams.set("manufacturer", params.manufacturer.trim())
  }
  if (params.storageCondition?.trim()) {
    searchParams.set("storageCondition", params.storageCondition.trim())
  }
  if (params.sortBy) {
    searchParams.set("sortBy", params.sortBy)
  }
  if (params.sortDir) {
    searchParams.set("sortDir", params.sortDir)
  }

  const query = searchParams.toString()
  const endpoint = query ? `/medicines?${query}` : "/medicines"
  const response = await apiFetch<PageResponse<Medicine>>(endpoint)
  return normalizePage(response)
}

export const getAllMedicines = async (): Promise<Medicine[]> => {
  const firstPage = await getMedicines({
    page: 0,
    size: 200,
    sortBy: "name",
    sortDir: "asc",
  })

  if (firstPage.totalPages <= 1) {
    return firstPage.content
  }

  const pageRequests = Array.from({ length: firstPage.totalPages - 1 }, (_, i) =>
    getMedicines({
      page: i + 1,
      size: firstPage.size,
      sortBy: "name",
      sortDir: "asc",
    })
  )

  const remainingPages = await Promise.all(pageRequests)

  return [
    ...firstPage.content,
    ...remainingPages.flatMap((page) => page.content),
  ]
}

export const createMedicine = async (
  medicine: Omit<Medicine, "medicineId">
): Promise<Medicine> => {
  return apiFetch<Medicine>("/medicines", {
    method: "POST",
    body: JSON.stringify(medicine),
  })
}

export const updateMedicine = async (
  id: number,
  medicine: Partial<Omit<Medicine, "medicineId">>
): Promise<Medicine> => {
  return apiFetch<Medicine>(`/medicines/${id}`, {
    method: "PUT",
    body: JSON.stringify(medicine),
  })
}

export const deleteMedicine = async (id: number): Promise<void> => {
  return apiFetch<void>(`/medicines/${id}`, {
    method: "DELETE",
  })
}
