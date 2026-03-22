import { apiFetch } from "./api"
import type { PageResponse } from "./types"

export type MedicineRequestStatus = "PENDING" | "APPROVED" | "REJECTED"

export type MedicineRequestItem = {
  medicineId: number
  medicineName?: string
  quantity: number
  notes?: string
}

export type MedicineRequest = {
  requestId: number
  warehouseId: number
  warehouseName?: string
  requiredDate?: string
  notes?: string
  requestedBy: string
  createdDate: string
  status: MedicineRequestStatus
  items: MedicineRequestItem[]
}

export type CreateMedicineRequestPayload = {
  warehouseId: number
  requiredDate?: string
  notes?: string
  items: Array<{
    medicineId: number
    quantity: number
    notes?: string
  }>
}

export type GetMedicineRequestsParams = {
  page?: number
  size?: number
  medicineName?: string
  status?: MedicineRequestStatus
  startDate?: string
  endDate?: string
}

const buildMedicineRequestEndpoint = (
  endpoint: string,
  params: GetMedicineRequestsParams = {},
) => {
  const searchParams = new URLSearchParams()

  if (typeof params.page === "number") {
    searchParams.set("page", String(params.page))
  }
  if (typeof params.size === "number") {
    searchParams.set("size", String(params.size))
  }
  if (params.medicineName?.trim()) {
    searchParams.set("medicineName", params.medicineName.trim())
  }
  if (params.status) {
    searchParams.set("status", params.status)
  }
  if (params.startDate) {
    searchParams.set("startDate", params.startDate)
  }
  if (params.endDate) {
    searchParams.set("endDate", params.endDate)
  }

  const query = searchParams.toString()
  return query ? `${endpoint}?${query}` : endpoint
}

const getRequestPage = async (
  endpoint: string,
  params: GetMedicineRequestsParams = {},
): Promise<PageResponse<MedicineRequest>> => {
  return apiFetch<PageResponse<MedicineRequest>>(
    buildMedicineRequestEndpoint(endpoint, params),
  )
}

const getAllRequestPages = async (
  endpoint: string,
  params: Omit<GetMedicineRequestsParams, "page" | "size"> = {},
): Promise<MedicineRequest[]> => {
  const firstPage = await getRequestPage(endpoint, params)

  if (firstPage.totalPages <= 1) {
    return firstPage.content
  }

  const pageRequests = Array.from({ length: firstPage.totalPages - 1 }, (_, i) =>
    getRequestPage(endpoint, {
      ...params,
      page: i + 1,
      size: firstPage.size,
    }),
  )

  const remainingPages = await Promise.all(pageRequests)

  return [
    ...firstPage.content,
    ...remainingPages.flatMap((page) => page.content),
  ]
}

export const getMedicineRequestsPage = async (
  params: GetMedicineRequestsParams = {},
): Promise<PageResponse<MedicineRequest>> => {
  return getRequestPage("/medicine-requests", params)
}

export const getMyMedicineRequestsPage = async (
  params: GetMedicineRequestsParams = {},
): Promise<PageResponse<MedicineRequest>> => {
  return getRequestPage("/medicine-requests/my", params)
}

export const getMedicineRequests = async (
  params: Omit<GetMedicineRequestsParams, "page" | "size"> = {},
): Promise<MedicineRequest[]> => {
  return getAllRequestPages("/medicine-requests", params)
}

export const getMyMedicineRequests = async (
  params: Omit<GetMedicineRequestsParams, "page" | "size"> = {},
): Promise<MedicineRequest[]> => {
  return getAllRequestPages("/medicine-requests/my", params)
}

export const createMedicineRequest = async (
  payload: CreateMedicineRequestPayload,
): Promise<MedicineRequest> => {
  return apiFetch<MedicineRequest>("/medicine-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export const approveMedicineRequest = async (requestId: number): Promise<MedicineRequest> => {
  return apiFetch<MedicineRequest>(`/medicine-requests/${requestId}/approve`, {
    method: "POST",
  })
}

export const rejectMedicineRequest = async (requestId: number): Promise<MedicineRequest> => {
  return apiFetch<MedicineRequest>(`/medicine-requests/${requestId}/reject`, {
    method: "POST",
  })
}
