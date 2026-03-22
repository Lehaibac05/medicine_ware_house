import { apiFetch } from "./api"
import type { Batch, PageResponse } from "./types"

export const getAllBatches = async (params?: {
  page?: number
  size?: number
}): Promise<Batch[] | PageResponse<Batch>> => {
  if (params?.page !== undefined && params?.size !== undefined) {
    const query = new URLSearchParams()
    query.append("page", String(params.page))
    query.append("size", String(params.size))
    return apiFetch<PageResponse<Batch>>(`/batches?${query.toString()}`)
  }
  return apiFetch<Batch[]>("/batches")
}

export const getBatchesByMedicine = async (
  medicineId: number
): Promise<Batch[]> => {
  return apiFetch<Batch[]>(`/medicines/${medicineId}/batches`)
}

export const createBatch = async (
  medicineId: number,
  batch: Omit<Batch, "batchId" | "medicine">
): Promise<Batch> => {
  return apiFetch<Batch>(`/medicines/${medicineId}/batches`, {
    method: "POST",
    body: JSON.stringify(batch),
  })
}

export const updateBatch = async (
  medicineId: number,
  batchId: number,
  batch: Partial<Omit<Batch, "batchId" | "medicine">>
): Promise<Batch> => {
  return apiFetch<Batch>(`/medicines/${medicineId}/batches/${batchId}`, {
    method: "PUT",
    body: JSON.stringify(batch),
  })
}

export const deleteBatch = async (
  medicineId: number,
  batchId: number
): Promise<void> => {
  return apiFetch<void>(`/medicines/${medicineId}/batches/${batchId}`, {
    method: "DELETE",
  })
}
