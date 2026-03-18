import { apiFetch } from "./api"

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
}

const toQueryString = (query?: InventoryQuery) => {
  if (!query) return ""

  const params = new URLSearchParams()
  if (query.medicineName) params.set("medicineName", query.medicineName)
  if (query.warehouseId) params.set("warehouseId", String(query.warehouseId))
  if (query.status) params.set("status", query.status)
  if (query.expiryFrom) params.set("expiryFrom", query.expiryFrom)
  if (query.expiryTo) params.set("expiryTo", query.expiryTo)

  const qs = params.toString()
  return qs ? `?${qs}` : ""
}

export const getInventory = async (query?: InventoryQuery): Promise<InventoryRow[]> => {
  return apiFetch<InventoryRow[]>(`/inventory${toQueryString(query)}`)
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
