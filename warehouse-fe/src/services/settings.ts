import { ApiError, apiFetch } from './api'

export interface InventorySettings {
  lowStockThreshold: number
  expiryAlertDays: number
  enableAIForecast: boolean
  autoOrderEnabled: boolean
  reorderPoint: number
}

export interface NotificationSettings {
  emailNotifications: boolean
  lowStockAlert: boolean
  expiryAlert: boolean
  orderAlert: boolean
}

export interface Settings {
  inventory: InventorySettings
  notifications: NotificationSettings
}

// Get all settings
export const getSettings = async (): Promise<Settings> => {
  try {
    return await apiFetch<Settings>('/settings', {
      method: 'GET',
    })
  } catch (error) {
    if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
      throw error
    }
    console.error('Failed to fetch settings:', error)
    // Return default settings if API fails
    return getDefaultSettings()
  }
}

// Update inventory settings
export const updateInventorySettings = async (
  settings: InventorySettings
): Promise<void> => {
  return await apiFetch<void>('/settings/inventory', {
    method: 'PUT',
    body: JSON.stringify(settings),
  })
}

// Update notification settings
export const updateNotificationSettings = async (
  settings: NotificationSettings
): Promise<void> => {
  return await apiFetch<void>('/settings/notifications', {
    method: 'PUT',
    body: JSON.stringify(settings),
  })
}

// Default settings (fallback)
export const getDefaultSettings = (): Settings => {
  return {
    inventory: {
      lowStockThreshold: 50,
      expiryAlertDays: 30,
      enableAIForecast: true,
      autoOrderEnabled: false,
      reorderPoint: 10,
    },
    notifications: {
      emailNotifications: true,
      lowStockAlert: true,
      expiryAlert: true,
      orderAlert: true,
    },
  }
}
