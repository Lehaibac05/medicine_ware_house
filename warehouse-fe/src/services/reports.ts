import http from "./http"

export type InventoryStatus = "LOW_STOCK" | "NORMAL" | "EXPIRING_SOON"

export type InventoryReportItem = {
  medicineId: number
  medicineName: string
  warehouseId: number
  warehouseName: string
  totalStock: number
  batchCount: number
  nearestExpiryDate?: string
  status: InventoryStatus
  reorderLevel?: number
}

export type InventoryReportResponse = {
  items: InventoryReportItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  generatedAt: string
  exportColumns: string[]
}

export type FinancialStatus = "PAID" | "PARTIAL" | "UNPAID"

export type FinancialReportItem = {
  invoiceId: number
  invoiceCode: string
  supplierId?: number
  supplierName?: string
  invoiceDate?: string
  dueDate?: string
  totalAmount: number
  paidAmount: number
  remainingAmount: number
  status: FinancialStatus
}

export type FinancialReportResponse = {
  items: FinancialReportItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  totalInvoiceAmount: number
  totalPaidAmount: number
  totalRemainingAmount: number
  generatedAt: string
  exportColumns: string[]
}

export type DashboardSummaryResponse = {
  totalMedicines: number
  totalStock: number
  lowStockCount: number
  expiringSoonCount: number
  totalInvoices: number
  totalPaid: number
  totalUnpaid: number
  stockByWarehouse: Array<{ warehouseId: number; warehouseName: string; totalStock: number }>
  invoiceStatusDistribution: Array<{ status: string; value: number }>
  monthlySpending: Array<{ month: string; amount: number }>
  lowStockItems: Array<{
    medicineId: number
    medicineName: string
    warehouseId: number
    warehouseName: string
    totalStock: number
    reorderLevel: number
  }>
  expiringBatchItems: Array<{
    batchId: number
    lotNumber: string
    medicineId: number
    medicineName: string
    warehouseId: number
    warehouseName: string
    expiryDate: string
    quantity: number
  }>
}

export type InventoryReportParams = {
  warehouseId?: number
  medicineName?: string
  medicineGroup?: string
  status?: InventoryStatus
  expiryFrom?: string
  expiryTo?: string
  page?: number
  size?: number
}

export type FinancialReportParams = {
  fromDate?: string
  toDate?: string
  supplierId?: number
  status?: FinancialStatus
  page?: number
  size?: number
}

export const reportApi = {
  getDashboardSummary: async () => {
    const response = await http.get<DashboardSummaryResponse>("/reports/dashboard")
    return response.data
  },

  getInventoryReport: async (params: InventoryReportParams) => {
    const response = await http.get<InventoryReportResponse>("/reports/inventory", { params })
    return response.data
  },

  exportInventoryReport: async (params: Omit<InventoryReportParams, "page" | "size">) => {
    const response = await http.get<InventoryReportResponse>("/reports/inventory/export", { params })
    return response.data
  },

  getFinancialReport: async (params: FinancialReportParams) => {
    const response = await http.get<FinancialReportResponse>("/reports/financial", { params })
    return response.data
  },

  exportFinancialReport: async (params: Omit<FinancialReportParams, "page" | "size">) => {
    const response = await http.get<FinancialReportResponse>("/reports/financial/export", { params })
    return response.data
  },
}
