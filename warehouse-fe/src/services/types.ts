// Common types for API responses
export type Medicine = {
  medicineId: number
  name: string
  manufacturer: string
  storageCondition: string
  description: string
}

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type Warehouse = {
  warehouseId: number
  name?: string
  location?: string
  description?: string
}

export type Batch = {
  batchId: number
  lotNumber: string
  manufactureDate: string // ISO date string
  expiryDate: string // ISO date string
  quantity: number
  status: string
  medicine?: Medicine
  warehouse?: Warehouse
}
