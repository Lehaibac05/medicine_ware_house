import { apiFetch } from "./api"

export type Supplier = {
  supplierId: number
  supplierName: string
  contactPerson?: string
  phoneNumber?: string
  email?: string
  address?: string
  taxCode?: string
  status?: string
}

export const getSuppliers = async (): Promise<Supplier[]> => {
  return apiFetch<Supplier[]>("/suppliers")
}

export const getActiveSuppliers = async (): Promise<Supplier[]> => {
  return apiFetch<Supplier[]>("/suppliers/active")
}
