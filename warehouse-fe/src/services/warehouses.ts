import { apiFetch } from "./api"
import type { Warehouse } from "./types"

export const getWarehouses = async (): Promise<Warehouse[]> => {
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
