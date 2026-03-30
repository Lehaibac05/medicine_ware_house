import http from "./http"
import {
  approveMedicineRequest,
  createMedicineRequest,
  getMedicineRequests,
  getMedicineRequestsPage,
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
    qrBankTransferLink?: string
    email?: string
  }
  warehouse?: {
    warehouseId: number
    warehouseName: string
  }
  items: Array<{
    itemId: number
    requestedQuantity: number
    receivedQuantity?: number
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
  createdAt?: string
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

export type SupplierInvoice = {
  invoiceId: number
  invoiceCode: string
  status: string
  invoiceDate?: string
  dueDate?: string
  totalAmount: number
  paidAmount: number
  remainingAmount: number
  notes?: string
  verificationNotes?: string
  rejectionReason?: string
  hasMismatch?: boolean
  mismatchWarning?: string
  supplier?: {
    supplierId: number
    supplierName: string
    qrBankTransferLink?: string
  }
  goodsReceipt?: {
    receiptId: number
    receiptCode: string
    purchaseOrderCode?: string
  }
  payments: Array<{
    paymentId: number
    paymentDate?: string
    amount?: number
    method?: string
    transactionReference?: string
    status?: string
    notes?: string
  }>
}

export type CreateSupplierInvoicePayload = {
  goodsReceiptId: number
  invoiceDate?: string
  dueDate?: string
  notes?: string
  items: Array<{
    medicineId: number
    quantity: number
    unitPrice: number
    notes?: string
  }>
}

export type VerifySupplierInvoicePayload = {
  verificationNotes?: string
}

export type RejectSupplierInvoicePayload = {
  reason?: string
}

export type PaySupplierInvoicePayload = {
  amount: number
  method?: string
  transactionReference?: string
  notes?: string
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

  getRequests: async (params?: {
    page?: number
    size?: number
    status?: string
    medicineName?: string
    startDate?: string
    endDate?: string
  }) => {
    if (params?.page !== undefined && params?.size !== undefined) {
      const data = await getMedicineRequestsPage(params as Parameters<typeof getMedicineRequestsPage>[0])
      return data
    }
    const data = await getMedicineRequests()
    return { content: data, totalElements: data.length, totalPages: 1, size: data.length, number: 0 }
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
  getGoodsReceipts: async (params?: { page?: number; size?: number }) => {
    const query = new URLSearchParams()
    if (params?.page !== undefined) {
      query.append("page", String(params.page))
    }
    if (params?.size !== undefined) {
      query.append("size", String(params.size))
    }
    const url = query.toString() ? `/goods-receipts?${query.toString()}` : "/goods-receipts"
    const response = await http.get<any>(url)
    return response.data
  },
  getGoodsReceiptById: async (id: number) => {
    const response = await http.get<GoodsReceipt>(`/goods-receipts/${id}`)
    return response.data
  },
  approveGoodsReceipt: async (id: number, approved = true, notes?: string) => {
    const response = await http.post<GoodsReceipt>(`/goods-receipts/${id}/approve`, {
      approved,
      notes,
    })
    return response.data
  },

  getSupplierInvoices: async () => {
    const response = await http.get<SupplierInvoice[]>("/supplier-invoices")
    return response.data
  },

  getSupplierInvoiceById: async (id: number) => {
    const response = await http.get<SupplierInvoice>(`/supplier-invoices/${id}`)
    return response.data
  },

  createSupplierInvoice: async (payload: CreateSupplierInvoicePayload) => {
    const response = await http.post<SupplierInvoice>("/supplier-invoices", payload)
    return response.data
  },

  verifySupplierInvoice: async (id: number, payload?: VerifySupplierInvoicePayload) => {
    const response = await http.post<SupplierInvoice>(`/supplier-invoices/${id}/verify`, payload ?? {})
    return response.data
  },

  rejectSupplierInvoice: async (id: number, payload?: RejectSupplierInvoicePayload) => {
    const response = await http.post<SupplierInvoice>(`/supplier-invoices/${id}/reject`, payload ?? {})
    return response.data
  },

  paySupplierInvoice: async (id: number, payload: PaySupplierInvoicePayload) => {
    const response = await http.post<SupplierInvoice>(`/supplier-invoices/${id}/pay`, payload)
    return response.data
  },
}
