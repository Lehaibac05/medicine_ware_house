import { apiFetch } from "./api"

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

export const getMedicineRequests = async (): Promise<MedicineRequest[]> => {
  return apiFetch<MedicineRequest[]>("/medicine-requests")
}

export const getMyMedicineRequests = async (): Promise<MedicineRequest[]> => {
  return apiFetch<MedicineRequest[]>("/medicine-requests/my")
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
