import http from "./http"
import {
  approveMedicineRequest,
  createMedicineRequest,
  getMedicineRequests,
  rejectMedicineRequest,
} from "./medicineRequests"

export type InventoryStatus = "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON"

export type InventoryItem = {
  medicineId: number
  medicineName: string
  warehouseId: number
  warehouseName: string
  totalStock: number
  batchCount: number
  nearestExpiryDate?: string
  status: InventoryStatus
}

export type MedicineRequest = {
  requestId: number
  warehouseId: number
  warehouseName?: string
  requiredDate?: string
  notes?: string
  requestedBy: string
  createdDate: string
  status: "PENDING" | "APPROVED" | "REJECTED"
  items: Array<{
    medicineId: number
    medicineName?: string
    quantity: number
    notes?: string
  }>
}

export type PurchaseOrder = {
  purchaseOrderId: number
  orderCode: string
  status: string
  expectedDeliveryDate?: string
  totalAmount: number
  notes?: string
  createdAt?: string
  supplier?: {
    supplierId: number
    supplierName: string
    email?: string
  }
  warehouse?: {
    warehouseId: number
    warehouseName: string
  }
  items: Array<{
    itemId: number
    requestedQuantity: number
    unitPrice: number
    totalPrice: number
    expectedExpiryDate?: string
    notes?: string
    medicine?: {
      medicineId: number
      medicineName?: string
    }
  }>
}

export type GoodsReceipt = {
  receiptId: number
  receiptCode: string
  status: string
  receivedAt?: string
  approvedAt?: string
  qualityPassed?: boolean
  qualityCheckNotes?: string
  receivedBy?: {
    userId: number
    username: string
    fullName: string
  }
  approvedBy?: {
    userId: number
    username: string
    fullName: string
  }
  purchaseOrder?: PurchaseOrder
}

export const workflowApi = {
  getInventory: async (params?: {
    medicineName?: string
    warehouseId?: number
    status?: string
  }) => {
    const response = await http.get<InventoryItem[]>("/inventory", { params })
    return response.data
  },
  getLowStockInventory: async () => {
    const response = await http.get<InventoryItem[]>("/inventory/low-stock")
    return response.data
  },
  getExpiringInventory: async () => {
    const response = await http.get<InventoryItem[]>("/inventory/expiring")
    return response.data
  },

  getRequests: async () => {
    const data = await getMedicineRequests()
    return data as MedicineRequest[]
  },
  createRequest: async (payload: {
    warehouseId: number
    requiredDate?: string
    notes?: string
    items: Array<{ medicineId: number; quantity: number; notes?: string }>
  }) => {
    const data = await createMedicineRequest(payload)
    return data as MedicineRequest
  },
  approveRequest: async (id: number) => {
    const data = await approveMedicineRequest(id)
    return data as MedicineRequest
  },
  rejectRequest: async (id: number) => {
    const data = await rejectMedicineRequest(id)
    return data as MedicineRequest
  },

  getPurchaseOrders: async () => {
    const response = await http.get<PurchaseOrder[]>("/purchase-orders")
    return response.data
  },
  getPurchaseOrderById: async (id: number) => {
    const response = await http.get<PurchaseOrder>(`/purchase-orders/${id}`)
    return response.data
  },
  updatePurchaseOrderStatus: async (id: number, status: string) => {
    const response = await http.put<PurchaseOrder>(`/purchase-orders/${id}/status`, { status })
    return response.data
  },
  confirmPurchaseOrder: async (id: number) => {
    const response = await http.post<PurchaseOrder>(`/purchase-orders/${id}/confirm`)
    return response.data
  },

  createGoodsReceipt: async (payload: {
    purchaseOrderId: number
    qualityCheckNotes?: string
    qualityPassed?: boolean
    receivedItems: Array<{
      itemId: number
      receivedQuantity: number
      actualExpiryDate?: string
      lotNumber?: string
      manufactureDate?: string
    }>
  }) => {
    const response = await http.post<GoodsReceipt>("/goods-receipts", payload)
    return response.data
  },
  getGoodsReceipts: async () => {
    const response = await http.get<GoodsReceipt[]>("/goods-receipts")
    return response.data
  },
  approveGoodsReceipt: async (id: number, approved = true, notes?: string) => {
    const response = await http.post<GoodsReceipt>(`/goods-receipts/${id}/approve`, {
      approved,
      notes,
    })
    return response.data
  },
}
