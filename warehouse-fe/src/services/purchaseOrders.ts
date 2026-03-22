import { ApiError, apiFetch, getAuthToken } from "./api"

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type PurchaseOrderItem = {
  itemId: number
  requestedQuantity: number
  receivedQuantity: number
  unitPrice: number
  totalPrice: number
  expectedExpiryDate?: string
  actualExpiryDate?: string
  notes?: string
  medicine?: {
    medicineId: number
    medicineName?: string
    sku?: string
  }
}

export type PurchaseOrder = {
  purchaseOrderId: number
  orderCode: string
  status: string
  expectedDeliveryDate?: string
  totalAmount: number
  notes?: string
  createdAt?: string
  updatedAt?: string
  supplier?: {
    supplierId: number
    supplierName: string
    contactPerson?: string
    phoneNumber?: string
    email?: string
  }
  warehouse?: {
    warehouseId: number
    warehouseName: string
  }
  items: PurchaseOrderItem[]
}

export type CreatePurchaseOrderPayload = {
  supplierId: number
  warehouseId: number
  expectedDeliveryDate?: string
  notes?: string
  items: Array<{
    medicineId: number
    requestedQuantity: number
    unitPrice: number
    expectedExpiryDate?: string
    notes?: string
  }>
}

export const getPurchaseOrders = async (params?: {
  supplierId?: number
  warehouseId?: number
  status?: string
  page?: number
  size?: number
}): Promise<PageResponse<PurchaseOrder>> => {
  const query = new URLSearchParams()

  if (params?.supplierId) query.append("supplierId", String(params.supplierId))
  if (params?.warehouseId) query.append("warehouseId", String(params.warehouseId))
  if (params?.status && params.status !== "all") {
    query.append("status", params.status)
  }

  if (params?.page !== undefined) {
    query.append("page", String(params.page))
  }

  if (params?.size !== undefined) {
    query.append("size", String(params.size))
  }

  const url = query.toString()
    ? `/purchase-orders?${query.toString()}`
    : `/purchase-orders`

  return apiFetch<PageResponse<PurchaseOrder>>(url)
}

export const getPurchaseOrderById = async (id: number): Promise<PurchaseOrder> => {
  return apiFetch<PurchaseOrder>(`/purchase-orders/${id}`)
}

export const confirmPurchaseOrder = async (id: number): Promise<PurchaseOrder> => {
  return apiFetch<PurchaseOrder>(`/purchase-orders/${id}/confirm`, { method: "POST" })
}

export const updatePurchaseOrderStatus = async (
  id: number,
  status: string,
): Promise<PurchaseOrder> => {
  return apiFetch<PurchaseOrder>(`/purchase-orders/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status }),
  })
}

export const createPurchaseOrder = async (
  payload: CreatePurchaseOrderPayload,
): Promise<PurchaseOrder> => {
  return apiFetch<PurchaseOrder>("/purchase-orders", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export const updatePurchaseOrder = async (
  id: number,
  payload: CreatePurchaseOrderPayload,
): Promise<PurchaseOrder> => {
  return apiFetch<PurchaseOrder>(`/purchase-orders/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  })
}

export const sendPurchaseOrderEmail = async (
  id: number,
): Promise<{ message: string; orderCode?: string; supplierEmail?: string }> => {
  return apiFetch<{ message: string; orderCode?: string; supplierEmail?: string }>(
    `/purchase-orders/${id}/send-email`,
    { method: "POST" },
  )
}

export const downloadPurchaseOrderPdf = async (id: number, orderCode?: string): Promise<void> => {
  const token = getAuthToken()
  const apiBaseUrl = import.meta.env.VITE_API_URL ?? ""

  const headers: Record<string, string> = {}
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${apiBaseUrl}/purchase-orders/${id}/pdf`, {
    method: "GET",
    headers,
  })

  if (!response.ok) {
    throw new ApiError(`API Error: ${response.statusText}`, response.status, response.statusText)
  }

  const blob = await response.blob()
  const url = window.URL.createObjectURL(blob)
  const anchor = document.createElement("a")
  anchor.href = url
  const fileName = `${orderCode || `PO-${id}`}.pdf`
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
}
