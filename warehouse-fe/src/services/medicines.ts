import { apiFetch } from "./api"
import type { Medicine } from "./types"

export const getMedicines = async (): Promise<Medicine[]> => {
  return apiFetch<Medicine[]>("/medicines")
}

export const createMedicine = async (
  medicine: Omit<Medicine, "medicineId">
): Promise<Medicine> => {
  return apiFetch<Medicine>("/medicines", {
    method: "POST",
    body: JSON.stringify(medicine),
  })
}

export const updateMedicine = async (
  id: number,
  medicine: Partial<Omit<Medicine, "medicineId">>
): Promise<Medicine> => {
  return apiFetch<Medicine>(`/medicines/${id}`, {
    method: "PUT",
    body: JSON.stringify(medicine),
  })
}

export const deleteMedicine = async (id: number): Promise<void> => {
  return apiFetch<void>(`/medicines/${id}`, {
    method: "DELETE",
  })
}
