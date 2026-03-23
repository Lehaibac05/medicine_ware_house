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

const normalizePage = <T>(page: PageResponse<T> | T[]): PageResponse<T> => {
  if (Array.isArray(page)) {
    const size = page.length;
    return {
      content: page,
      totalElements: page.length,
      totalPages: page.length > 0 ? 1 : 0,
      size,
      number: 0,
    };
  }

  const content = Array.isArray(page.content) ? page.content : [];
  return {
    ...page,
    content,
    totalElements: page.totalElements ?? content.length,
    totalPages: page.totalPages ?? (content.length > 0 ? 1 : 0),
    size: page.size ?? content.length,
    number: page.number ?? 0,
  };
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

  const response = await apiFetch<PageResponse<Supplier>>(`/suppliers?${query.toString()}`);
  return normalizePage(response);
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
