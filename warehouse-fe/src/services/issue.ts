import { apiFetch } from "./api"

export type IssueStatus = "PENDING" | "APPROVED" | "REJECTED" | "COMPLETED"

export type IssueExecutionItem = {
  orderItemId: number
  orderId: number
  medicineId: number
  batchId: number
  medicineName?: string
  lotNumber?: string
  expiryDate?: string
  quantity: number
  warehouseId?: number
  warehouseName?: string
  issuedById?: number
  issuedByName?: string
  department?: string
  issuedAt?: string
}

export type IssueRequest = {
  orderId: number
  medicineId: number
  medicineName?: string
  requestedQuantity: number
  approvedQuantity?: number
  issuedQuantity?: number
  remainingQuantity?: number
  department?: string
  purpose?: string
  warehouseId?: number
  warehouseName?: string
  requestDate?: string
  neededDate?: string
  status: IssueStatus
  rejectionReason?: string
  createdById?: number
  createdByName?: string
  approvedById?: number
  approvedByName?: string
  approvedAt?: string
  completedAt?: string
  createdAt?: string
  updatedAt?: string
  availableStockInWarehouse?: number
  suggestedAvailableQuantity?: number
  alternativeWarehouses?: Array<{
    warehouseId: number
    warehouseName?: string
    availableQuantity: number
  }>
  items: IssueExecutionItem[]
}

export type CreateIssueRequestPayload = {
  medicineId: number
  quantity: number
  warehouseId: number
  department: string
  purpose?: string
  neededDate?: string
}

export type ApproveIssuePayload = {
  allowPartial?: boolean
}

export type RejectIssuePayload = {
  reason?: string
}

export type IssueHistoryFilter = {
  medicineId?: number
  department?: string
  fromDate?: string
  toDate?: string
}

export const createIssueRequest = async (payload: CreateIssueRequestPayload): Promise<IssueRequest> => {
  return apiFetch<IssueRequest>("/issue-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export const getIssueRequests = async (): Promise<IssueRequest[]> => {
  return apiFetch<IssueRequest[]>("/issue-requests")
}

export const getMyIssueRequests = async (): Promise<IssueRequest[]> => {
  return apiFetch<IssueRequest[]>("/issue-requests/my")
}

export const getIssueRequestById = async (id: number): Promise<IssueRequest> => {
  return apiFetch<IssueRequest>(`/issue-requests/${id}`)
}

export const approveIssueRequest = async (id: number, payload: ApproveIssuePayload): Promise<IssueRequest> => {
  return apiFetch<IssueRequest>(`/issue-requests/${id}/approve`, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export const rejectIssueRequest = async (id: number, payload: RejectIssuePayload): Promise<IssueRequest> => {
  return apiFetch<IssueRequest>(`/issue-requests/${id}/reject`, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export const executeIssue = async (requestId: number): Promise<IssueRequest> => {
  return apiFetch<IssueRequest>(`/issues/execute/${requestId}`, {
    method: "POST",
  })
}

export const getIssueHistory = async (filters?: IssueHistoryFilter): Promise<IssueExecutionItem[]> => {
  const params = new URLSearchParams()
  if (filters?.medicineId) params.set("medicineId", String(filters.medicineId))
  if (filters?.department?.trim()) params.set("department", filters.department.trim())
  if (filters?.fromDate) params.set("fromDate", filters.fromDate)
  if (filters?.toDate) params.set("toDate", filters.toDate)
  const query = params.toString()
  return apiFetch<IssueExecutionItem[]>(`/issues/history${query ? `?${query}` : ""}`)
}

export const getIssueStockInsight = async (
  medicineId: number,
  warehouseId: number,
  quantity?: number,
): Promise<IssueRequest> => {
  const params = new URLSearchParams()
  params.set("medicineId", String(medicineId))
  params.set("warehouseId", String(warehouseId))
  if (quantity && Number.isFinite(quantity) && quantity > 0) {
    params.set("quantity", String(quantity))
  }
  return apiFetch<IssueRequest>(`/issues/stock-insight?${params.toString()}`)
}
