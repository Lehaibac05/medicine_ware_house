import { apiFetch } from "./api"
import type { PageResponse, Warehouse } from "./types"

export const getWarehouses = async (params?: {
  page?: number
  size?: number
}): Promise<Warehouse[] | PageResponse<Warehouse>> => {
  if (params?.page !== undefined && params?.size !== undefined) {
    const query = new URLSearchParams()
    query.append("page", String(params.page))
    query.append("size", String(params.size))
    return apiFetch<PageResponse<Warehouse>>(`/warehouses?${query.toString()}`)
  }
  return apiFetch<Warehouse[]>("/warehouses")
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
