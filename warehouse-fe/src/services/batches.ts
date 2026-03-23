import { apiFetch } from "./api"
import type { Batch, PageResponse } from "./types"

const normalizePage = <T>(page: PageResponse<T> | T[]): PageResponse<T> => {
  if (Array.isArray(page)) {
    const size = page.length
    return {
      content: page,
      totalElements: page.length,
      totalPages: page.length > 0 ? 1 : 0,
      size,
      number: 0,
    }
  }

  const content = Array.isArray(page.content) ? page.content : []
  return {
    ...page,
    content,
    totalElements: page.totalElements ?? content.length,
    totalPages: page.totalPages ?? (content.length > 0 ? 1 : 0),
    size: page.size ?? content.length,
    number: page.number ?? 0,
  }
}

export const getAllBatches = async (params?: {
  page?: number
  size?: number
}): Promise<Batch[] | PageResponse<Batch>> => {
  if (params?.page !== undefined && params?.size !== undefined) {
    const query = new URLSearchParams()
    query.append("page", String(params.page))
    query.append("size", String(params.size))
    const response = await apiFetch<PageResponse<Batch>>(`/batches?${query.toString()}`)
    return normalizePage(response)
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
