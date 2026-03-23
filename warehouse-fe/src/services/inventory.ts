import { apiFetch } from "./api"
import type { PageResponse } from "./types"

export type InventoryStatus = "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON"

export type InventoryRow = {
  medicineId: number
  medicineName: string
  warehouseId: number
  warehouseName: string
  totalStock: number
  batchCount: number
  nearestExpiryDate?: string
  status: InventoryStatus
}

export type InventoryBatchInfo = {
  batchId: number
  lotNumber: string
  quantity: number
  manufactureDate?: string
  expiryDate?: string
}

export type InventoryDetailGroup = {
  medicineId: number
  medicineName: string
  warehouseId: number
  warehouseName: string
  totalStock: number
  batches: InventoryBatchInfo[]
}

export type InventorySummary = {
  totalMedicines: number
  totalBatches: number
  lowStockCount: number
  expiringSoonCount: number
}

export type ExpiringBatch = {
  batchId: number
  lotNumber: string
  quantity: number
  manufactureDate?: string
  expiryDate?: string
  medicineId: number
  medicineName: string
  warehouseId: number
  warehouseName: string
}

type InventoryQuery = {
  medicineName?: string
  warehouseId?: number
  status?: string
  expiryFrom?: string
  expiryTo?: string
  page?: number
  size?: number
}

const toQueryString = (query?: InventoryQuery) => {
  if (!query) return ""

  const params = new URLSearchParams()
  if (query.medicineName) params.set("medicineName", query.medicineName)
  if (query.warehouseId) params.set("warehouseId", String(query.warehouseId))
  if (query.status) params.set("status", query.status)
  if (query.expiryFrom) params.set("expiryFrom", query.expiryFrom)
  if (query.expiryTo) params.set("expiryTo", query.expiryTo)
  if (query.page !== undefined) params.set("page", String(query.page))
  if (query.size !== undefined) params.set("size", String(query.size))

  const qs = params.toString()
  return qs ? `?${qs}` : ""
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

export const getInventory = async (
  query?: InventoryQuery,
): Promise<InventoryRow[] | PageResponse<InventoryRow>> => {
  if (query?.page !== undefined && query?.size !== undefined) {
    const response = await apiFetch<PageResponse<InventoryRow>>(`/inventory/paged${toQueryString(query)}`);
    return normalizePage(response);
  }
  // For non-paginated requests, get a large page to simulate getting all data
  const largePageQuery = { ...query, page: 0, size: 10000 };
  const response = await apiFetch<PageResponse<InventoryRow>>(`/inventory/paged${toQueryString(largePageQuery)}`);
  return normalizePage(response).content;
}

export const getInventoryDetail = async (medicineId: number): Promise<InventoryDetailGroup[]> => {
  return apiFetch<InventoryDetailGroup[]>(`/inventory/${medicineId}`)
}

export const getLowStockInventory = async (): Promise<InventoryRow[]> => {
  return apiFetch<InventoryRow[]>("/inventory/low-stock")
}

export const getExpiringInventory = async (): Promise<ExpiringBatch[]> => {
  return apiFetch<ExpiringBatch[]>("/inventory/expiring")
}

export const getInventorySummary = async (): Promise<InventorySummary> => {
  return apiFetch<InventorySummary>("/inventory/summary")
}
