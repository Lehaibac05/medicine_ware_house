import { ApiError, apiFetch } from './api'

export interface GeneralSettings {
  pharmacyName: string
  contactEmail: string
  address: string
  phoneNumber: string
}

export interface InventorySettings {
  lowStockThreshold: number
  expiryAlertDays: number
  enableAIForecast: boolean
  autoOrderEnabled: boolean
  reorderPoint: number
}

export interface SecuritySettings {
  passwordPolicy: string
  sessionTimeout: number
  twoFactorAuth: boolean
  loginAttempts: number
}

export interface NotificationSettings {
  emailNotifications: boolean
  lowStockAlert: boolean
  expiryAlert: boolean
  orderAlert: boolean
}

export interface SystemSettings {
  darkMode: boolean
  language: string
  timezone: string
  dateFormat: string
}

export interface Settings {
  general: GeneralSettings
  inventory: InventorySettings
  security: SecuritySettings
  notifications: NotificationSettings
  system: SystemSettings
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

// Update general settings
export const updateGeneralSettings = async (
  settings: GeneralSettings
): Promise<void> => {
  return await apiFetch<void>('/settings/general', {
    method: 'PUT',
    body: JSON.stringify(settings),
  })
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

// Update security settings
export const updateSecuritySettings = async (
  settings: SecuritySettings
): Promise<void> => {
  return await apiFetch<void>('/settings/security', {
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

// Update system settings
export const updateSystemSettings = async (
  settings: SystemSettings
): Promise<void> => {
  return await apiFetch<void>('/settings/system', {
    method: 'PUT',
    body: JSON.stringify(settings),
  })
}

// Default settings (fallback)
export const getDefaultSettings = (): Settings => {
  return {
    general: {
      pharmacyName: 'Main Street Pharmacy',
      contactEmail: 'admin@pharmacy.com',
      address: '123 Health Ave, Medical District, City State',
      phoneNumber: '+1 (555) 012-3456',
    },
    inventory: {
      lowStockThreshold: 50,
      expiryAlertDays: 30,
      enableAIForecast: true,
      autoOrderEnabled: false,
      reorderPoint: 10,
    },
    security: {
      passwordPolicy: 'medium',
      sessionTimeout: 15,
      twoFactorAuth: false,
      loginAttempts: 5,
    },
    notifications: {
      emailNotifications: true,
      lowStockAlert: true,
      expiryAlert: true,
      orderAlert: true,
    },
    system: {
      darkMode: false,
      language: 'en',
      timezone: 'UTC+7',
      dateFormat: 'DD/MM/YYYY',
    },
  }
}
