import { apiFetch } from "./api"
import type { PageResponse, Warehouse } from "./types"

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

export const getWarehouses = async (params?: {
  page?: number
  size?: number
}): Promise<Warehouse[]> => {
  if (params?.page !== undefined && params?.size !== undefined) {
    const query = new URLSearchParams()
    query.append("page", String(params.page))
    query.append("size", String(params.size))
    const response = await apiFetch<PageResponse<Warehouse>>(`/warehouses?${query.toString()}`)
    return normalizePage(response).content
  }
  const response = await apiFetch<PageResponse<Warehouse>>("/warehouses")
  return normalizePage(response).content
}

export const createWarehouse = async (
  warehouse: Omit<Warehouse, "warehouseId">
): Promise<Warehouse> => {
  return apiFetch<Warehouse>("/warehouses", {
    method: "POST",
    body: JSON.stringify(warehouse),
  })
}

export const updateWarehouse = async (
  id: number,
  warehouse: Partial<Omit<Warehouse, "warehouseId">>
): Promise<Warehouse> => {
  return apiFetch<Warehouse>(`/warehouses/${id}`, {
    method: "PUT",
    body: JSON.stringify(warehouse),
  })
}

export const deleteWarehouse = async (id: number): Promise<void> => {
  return apiFetch<void>(`/warehouses/${id}`, {
    method: "DELETE",
  })
}
