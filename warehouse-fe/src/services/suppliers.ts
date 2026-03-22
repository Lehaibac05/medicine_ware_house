import { apiFetch } from "./api"
import type { Supplier } from "./types"

export type { Supplier } from "./types"

export type SupplierQuery = {
  status?: string;
  supplierName?: string;
  keyword?: string;
  page?: number;
  size?: number;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export const getSuppliers = async (
  params: SupplierQuery
): Promise<PageResponse<Supplier>> => {
  const query = new URLSearchParams();

  if (params.status && params.status !== "all") {
    query.append("status", params.status);
  }

  if (params.keyword) {
    query.append("keyword", params.keyword);
  }

  if (params.supplierName) {
    query.append("supplierName", params.supplierName);
  }

  query.append("page", String(params.page ?? 0));
  query.append("size", String(params.size ?? 10));

  return apiFetch<PageResponse<Supplier>>(`/suppliers?${query.toString()}`);
};

export const getActiveSuppliers = async (): Promise<Supplier[]> => {
  return apiFetch<Supplier[]>("/suppliers/active")
}

export const createSupplier = async (
  supplier: Omit<Supplier, "supplierId" | "createdAt" | "updatedAt">
): Promise<Supplier> => {
  return apiFetch<Supplier>("/suppliers", {
    method: "POST",
    body: JSON.stringify(supplier),
  })
}

export const updateSupplier = async (
  id: number,
  supplier: Partial<Omit<Supplier, "supplierId" | "createdAt" | "updatedAt">>
): Promise<Supplier> => {
  return apiFetch<Supplier>(`/suppliers/${id}`, {
    method: "PUT",
    body: JSON.stringify(supplier),
  })
}

export const deleteSupplier = async (id: number): Promise<void> => {
  return apiFetch<void>(`/suppliers/${id}`, {
    method: "DELETE",
  })
}
