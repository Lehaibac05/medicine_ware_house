import { apiFetch } from "./api"

export type OrderItem = {
  orderItemId?: number
  batchId: number
  medicineName?: string
  lotNumber?: string
  quantity: number
  unitPrice: number
  discount?: number
  tax?: number
  totalPrice?: number
}

export type Order = {
  orderId: number
  orderDate: string
  status: string
  subTotal: number
  discountAmount: number
  taxAmount: number
  totalAmount: number
  userId: number
  userName?: string
  userEmail?: string
  items: OrderItem[]
}

export type CreateOrderRequest = {
  userId: number
  discountAmount?: number
  taxAmount?: number
  items: {
    batchId: number
    quantity: number
    unitPrice: number
    discount?: number
    tax?: number
  }[]
}

export type UpdateOrderStatusRequest = {
  status: string
}

export const getOrders = async (): Promise<Order[]> => {
  return apiFetch<Order[]>("/orders", {
    method: "GET",
  })
}

export const getOrderById = async (id: number): Promise<Order> => {
  return apiFetch<Order>(`/orders/${id}`, {
    method: "GET",
  })
}

export const getOrdersByStatus = async (status: string): Promise<Order[]> => {
  return apiFetch<Order[]>(`/orders/status/${status}`, {
    method: "GET",
  })
}

export const getOrdersByUser = async (userId: number): Promise<Order[]> => {
  return apiFetch<Order[]>(`/orders/user/${userId}`, {
    method: "GET",
  })
}

export const createOrder = async (
  data: CreateOrderRequest
): Promise<Order> => {
  return apiFetch<Order>("/orders", {
    method: "POST",
    body: JSON.stringify(data),
  })
}

export const updateOrderStatus = async (
  id: number,
  status: string
): Promise<Order> => {
  return apiFetch<Order>(`/orders/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  })
}

export const deleteOrder = async (id: number): Promise<void> => {
  return apiFetch<void>(`/orders/${id}`, {
    method: "DELETE",
  })
}
