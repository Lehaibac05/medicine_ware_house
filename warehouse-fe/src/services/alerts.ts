import { apiFetch } from "./api"

export type Alert = {
  alertId: number
  alertType: string // LOW_STOCK, EXPIRING_SOON, EXPIRED, SYSTEM
  severity: string // LOW, MEDIUM, HIGH, CRITICAL
  status: string // OPEN, IN_PROGRESS, RESOLVED
  message: string
  description: string
  createdAt: string
  resolvedAt?: string
  batchId?: number
  lotNumber?: string
  medicineId?: number
  medicineName?: string
  warehouseId?: number
  warehouseName?: string
  resolvedByUserId?: number
  resolvedByUsername?: string
}

export type AlertStats = {
  lowStockCount: number
  expiringSoonCount: number
  expiredCount: number
  systemWarningsCount: number
  totalActiveAlerts: number
}

export type AlertHistory = {
  historyId: number
  alertId: number
  action: string
  oldStatus?: string
  newStatus: string
  comment?: string
  timestamp: string
  username?: string
  userFullName?: string
}

export const getAllAlerts = async (): Promise<Alert[]> => {
  return apiFetch<Alert[]>("/alerts", {
    method: "GET",
  })
}

export const getActiveAlerts = async (): Promise<Alert[]> => {
  return apiFetch<Alert[]>("/alerts/active", {
    method: "GET",
  })
}

export const getAlertById = async (id: number): Promise<Alert> => {
  return apiFetch<Alert>(`/alerts/${id}`, {
    method: "GET",
  })
}

export const getAlertsByType = async (type: string): Promise<Alert[]> => {
  return apiFetch<Alert[]>(`/alerts/type/${type}`, {
    method: "GET",
  })
}

export const getAlertsBySeverity = async (severity: string): Promise<Alert[]> => {
  return apiFetch<Alert[]>(`/alerts/severity/${severity}`, {
    method: "GET",
  })
}

export const getAlertsByStatus = async (status: string): Promise<Alert[]> => {
  return apiFetch<Alert[]>(`/alerts/status/${status}`, {
    method: "GET",
  })
}

export const getAlertStats = async (): Promise<AlertStats> => {
  return apiFetch<AlertStats>("/alerts/stats", {
    method: "GET",
  })
}

export const resolveAlert = async (
  id: number,
  comment?: string
): Promise<Alert> => {
  return apiFetch<Alert>(`/alerts/${id}/resolve`, {
    method: "POST",
    body: JSON.stringify({ comment }),
  })
}

export const updateAlertStatus = async (
  id: number,
  status: string
): Promise<Alert> => {
  return apiFetch<Alert>(`/alerts/${id}/status?status=${status}`, {
    method: "PATCH",
  })
}

export const getAlertHistory = async (id: number): Promise<AlertHistory[]> => {
  return apiFetch<AlertHistory[]>(`/alerts/${id}/history`, {
    method: "GET",
  })
}

export const triggerAlertCheck = async (): Promise<string> => {
  return apiFetch<string>("/alerts/check", {
    method: "POST",
  })
}

export const checkAndGenerateAlerts = async (): Promise<{ message: string, alertsGenerated: number }> => {
  return apiFetch<{ message: string, alertsGenerated: number }>("/alerts/scan", {
    method: "POST",
  })
}
