import { Button, Card, Checkbox, Input, Layout, Select, Typography, message, InputNumber, Switch, Divider, Spin } from 'antd'
import { useState, useEffect } from 'react'
import { SaveOutlined, ReloadOutlined, BellOutlined, LockOutlined, SettingOutlined, DatabaseOutlined } from '@ant-design/icons'
import SidebarNav from '../dashboard/components/SidebarNav'
import TopBar from '../dashboard/components/TopBar'
import { 
  getSettings, 
  updateGeneralSettings, 
  updateInventorySettings, 
  updateSecuritySettings, 
  updateNotificationSettings, 
  updateSystemSettings 
} from '../../services/settings'

const { Content, Sider } = Layout
const { Text, Title } = Typography

const SettingsPage = () => {
  const [messageApi, contextHolder] = message.useMessage()
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(true)

  // General Settings State
  const [pharmacyName, setPharmacyName] = useState('Main Street Pharmacy')
  const [contactEmail, setContactEmail] = useState('admin@pharmacy.com')
  const [address, setAddress] = useState('123 Health Ave, Medical District, City State')
  const [phoneNumber, setPhoneNumber] = useState('+1 (555) 012-3456')

  // Inventory Settings State
  const [lowStockThreshold, setLowStockThreshold] = useState(50)
  const [expiryAlertDays, setExpiryAlertDays] = useState(30)
  const [enableAIForecast, setEnableAIForecast] = useState(true)
  const [autoOrderEnabled, setAutoOrderEnabled] = useState(false)
  const [reorderPoint, setReorderPoint] = useState(20)

  // Security Settings State
  const [passwordPolicy, setPasswordPolicy] = useState('medium')
  const [sessionTimeout, setSessionTimeout] = useState(15)
  const [twoFactorAuth, setTwoFactorAuth] = useState(false)
  const [loginAttempts, setLoginAttempts] = useState(5)

  // Notification Settings State
  const [emailNotifications, setEmailNotifications] = useState(true)
  const [lowStockAlert, setLowStockAlert] = useState(true)
  const [expiryAlert, setExpiryAlert] = useState(true)
  const [orderAlert, setOrderAlert] = useState(true)

  // System Settings State
  const [darkMode, setDarkMode] = useState(false)
  const [language, setLanguage] = useState('en')
  const [timezone, setTimezone] = useState('UTC+7')
  const [dateFormat, setDateFormat] = useState('DD/MM/YYYY')

  // Load settings on mount
  useEffect(() => {
    loadSettings()
  }, [])

  const loadSettings = async () => {
    try {
      setLoading(true)
      const settings = await getSettings()
      
      // Update all state
      setPharmacyName(settings.general.pharmacyName)
      setContactEmail(settings.general.contactEmail)
      setAddress(settings.general.address)
      setPhoneNumber(settings.general.phoneNumber)
      
      setLowStockThreshold(settings.inventory.lowStockThreshold)
      setExpiryAlertDays(settings.inventory.expiryAlertDays)
      setEnableAIForecast(settings.inventory.enableAIForecast)
      setAutoOrderEnabled(settings.inventory.autoOrderEnabled)
      setReorderPoint(settings.inventory.reorderPoint)
      
      setPasswordPolicy(settings.security.passwordPolicy)
      setSessionTimeout(settings.security.sessionTimeout)
      setTwoFactorAuth(settings.security.twoFactorAuth)
      setLoginAttempts(settings.security.loginAttempts)
      
      setEmailNotifications(settings.notifications.emailNotifications)
      setLowStockAlert(settings.notifications.lowStockAlert)
      setExpiryAlert(settings.notifications.expiryAlert)
      setOrderAlert(settings.notifications.orderAlert)
      
      setDarkMode(settings.system.darkMode)
      setLanguage(settings.system.language)
      setTimezone(settings.system.timezone)
      setDateFormat(settings.system.dateFormat)
    } catch (error) {
      console.error('Failed to load settings:', error)
      messageApi.warning('Using default settings')
    } finally {
      setLoading(false)
    }
  }

  const handleSaveGeneral = async () => {
    try {
      setSaving(true)
      await updateGeneralSettings({
        pharmacyName,
        contactEmail,
        address,
        phoneNumber
      })
      messageApi.success('General settings saved successfully')
    } catch (error) {
      messageApi.error('Failed to save settings')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveInventory = async () => {
    try {
      setSaving(true)
      await updateInventorySettings({
        lowStockThreshold,
        expiryAlertDays,
        enableAIForecast,
        autoOrderEnabled,
        reorderPoint
      })
      messageApi.success('Inventory settings saved successfully')
    } catch (error) {
      messageApi.error('Failed to save settings')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveSecurity = async () => {
    try {
      setSaving(true)
      await updateSecuritySettings({
        passwordPolicy,
        sessionTimeout,
        twoFactorAuth,
        loginAttempts
      })
      messageApi.success('Security settings saved successfully')
    } catch (error) {
      messageApi.error('Failed to save settings')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveNotifications = async () => {
    try {
      setSaving(true)
      await updateNotificationSettings({
        emailNotifications,
        lowStockAlert,
        expiryAlert,
        orderAlert
      })
      messageApi.success('Notification settings saved successfully')
    } catch (error) {
      messageApi.error('Failed to save settings')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveSystem = async () => {
    try {
      setSaving(true)
      await updateSystemSettings({
        darkMode,
        language,
        timezone,
        dateFormat
      })
      messageApi.success('System settings saved successfully')
    } catch (error) {
      messageApi.error('Failed to save settings')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="System Settings" subtitle="Configuration" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          {loading ? (
            <div className="flex items-center justify-center min-h-[400px]">
              <Spin size="large" tip="Loading settings..." />
            </div>
          ) : (
            <>
              <div>
                <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                  Configuration
                </Text>
                <Title level={3} className="!m-0">
                  System Settings
                </Title>
              </div>

          {/* General Settings */}
          <Card 
            className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={
              <div className="flex items-center gap-2">
                <SettingOutlined className="text-blue-600" />
                <Text strong className="text-base">GENERAL SETTINGS</Text>
              </div>
            }
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">PHARMACY NAME</Text>
                <Input 
                  value={pharmacyName}
                  onChange={(e) => setPharmacyName(e.target.value)}
                  placeholder="Enter pharmacy name"
                  size="large"
                />
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">CONTACT EMAIL</Text>
                <Input 
                  value={contactEmail}
                  onChange={(e) => setContactEmail(e.target.value)}
                  placeholder="admin@pharmacy.com"
                  type="email"
                  size="large"
                />
              </div>

              <div className="flex flex-col gap-2 md:col-span-2">
                <Text className="text-sm font-medium text-slate-700">ADDRESS</Text>
                <Input 
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="123 Health Ave, Medical District, City State"
                  size="large"
                />
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">PHONE NUMBER</Text>
                <Input 
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+1 (555) 012-3456"
                  size="large"
                />
              </div>
            </div>

            <div className="mt-6">
              <Button 
                type="primary" 
                size="large"
                icon={<SaveOutlined />}
                loading={saving}
                onClick={handleSaveGeneral}
              >
                SAVE CHANGES
              </Button>
            </div>
          </Card>

          {/* Inventory Settings */}
          <Card 
            className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={
              <div className="flex items-center gap-2">
                <DatabaseOutlined className="text-green-600" />
                <Text strong className="text-base">INVENTORY SETTINGS</Text>
              </div>
            }
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">LOW STOCK THRESHOLD</Text>
                <InputNumber 
                  value={lowStockThreshold}
                  onChange={(val) => setLowStockThreshold(val || 50)}
                  min={0}
                  max={1000}
                  className="w-full"
                  size="large"
                  addonAfter="units"
                />
                <Text className="text-xs text-slate-500">Trigger alert when stock falls below this value</Text>
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">EXPIRY ALERT DAYS</Text>
                <InputNumber 
                  value={expiryAlertDays}
                  onChange={(val) => setExpiryAlertDays(val || 30)}
                  min={1}
                  max={365}
                  className="w-full"
                  size="large"
                  addonAfter="days"
                />
                <Text className="text-xs text-slate-500">Alert before medicines expire</Text>
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">REORDER POINT</Text>
                <InputNumber 
                  value={reorderPoint}
                  onChange={(val) => setReorderPoint(val || 20)}
                  min={0}
                  max={1000}
                  className="w-full"
                  size="large"
                  addonAfter="units"
                />
                <Text className="text-xs text-slate-500">Automatic reorder trigger point</Text>
              </div>

              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">ENABLE AI FORECAST</Text>
                    <Text className="text-xs text-slate-500">Use AI to predict demand</Text>
                  </div>
                  <Switch 
                    checked={enableAIForecast}
                    onChange={setEnableAIForecast}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">AUTO-ORDER</Text>
                    <Text className="text-xs text-slate-500">Automatic purchase orders</Text>
                  </div>
                  <Switch 
                    checked={autoOrderEnabled}
                    onChange={setAutoOrderEnabled}
                  />
                </div>
              </div>
            </div>

            <div className="mt-6">
              <Button 
                type="primary" 
                size="large"
                icon={<SaveOutlined />}
                loading={saving}
                onClick={handleSaveInventory}
              >
                SAVE SETTINGS
              </Button>
            </div>
          </Card>

          {/* Security Settings */}
          <Card 
            className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={
              <div className="flex items-center gap-2">
                <LockOutlined className="text-red-600" />
                <Text strong className="text-base">SECURITY SETTINGS</Text>
              </div>
            }
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">PASSWORD POLICY</Text>
                <Select
                  value={passwordPolicy}
                  onChange={setPasswordPolicy}
                  size="large"
                  options={[
                    { value: 'low', label: 'Low (6+ characters)' },
                    { value: 'medium', label: 'Medium (8+ chars, Alpha-numeric)' },
                    { value: 'high', label: 'High (10+ chars, Special symbols)' },
                    { value: 'strict', label: 'Strict (12+ chars, All types)' },
                  ]}
                />
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">SESSION TIMEOUT</Text>
                <Select
                  value={sessionTimeout}
                  onChange={setSessionTimeout}
                  size="large"
                  options={[
                    { value: 5, label: '5 Minutes' },
                    { value: 15, label: '15 Minutes' },
                    { value: 30, label: '30 Minutes' },
                    { value: 60, label: '1 Hour' },
                    { value: 120, label: '2 Hours' },
                  ]}
                />
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">MAX LOGIN ATTEMPTS</Text>
                <InputNumber 
                  value={loginAttempts}
                  onChange={(val) => setLoginAttempts(val || 5)}
                  min={3}
                  max={10}
                  className="w-full"
                  size="large"
                  addonAfter="attempts"
                />
                <Text className="text-xs text-slate-500">Lock account after failed attempts</Text>
              </div>

              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">TWO-FACTOR AUTHENTICATION</Text>
                    <Text className="text-xs text-slate-500">Extra security layer</Text>
                  </div>
                  <Switch 
                    checked={twoFactorAuth}
                    onChange={setTwoFactorAuth}
                  />
                </div>
              </div>
            </div>

            <div className="mt-6">
              <Button 
                type="primary" 
                size="large"
                icon={<SaveOutlined />}
                loading={saving}
                onClick={handleSaveSecurity}
                danger
              >
                SAVE SECURITY SETTINGS
              </Button>
            </div>
          </Card>

          {/* Notification Settings */}
          <Card 
            className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={
              <div className="flex items-center gap-2">
                <BellOutlined className="text-purple-600" />
                <Text strong className="text-base">NOTIFICATION SETTINGS</Text>
              </div>
            }
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">EMAIL NOTIFICATIONS</Text>
                    <Text className="text-xs text-slate-500">Receive updates via email</Text>
                  </div>
                  <Switch 
                    checked={emailNotifications}
                    onChange={setEmailNotifications}
                  />
                </div>

                <Divider className="my-2" />

                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">LOW STOCK ALERTS</Text>
                    <Text className="text-xs text-slate-500">When inventory is low</Text>
                  </div>
                  <Switch 
                    checked={lowStockAlert}
                    onChange={setLowStockAlert}
                    disabled={!emailNotifications}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">EXPIRY ALERTS</Text>
                    <Text className="text-xs text-slate-500">When medicines are expiring</Text>
                  </div>
                  <Switch 
                    checked={expiryAlert}
                    onChange={setExpiryAlert}
                    disabled={!emailNotifications}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">ORDER ALERTS</Text>
                    <Text className="text-xs text-slate-500">New orders and updates</Text>
                  </div>
                  <Switch 
                    checked={orderAlert}
                    onChange={setOrderAlert}
                    disabled={!emailNotifications}
                  />
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">NOTIFICATION EMAIL</Text>
                <Input 
                  value={contactEmail}
                  disabled
                  size="large"
                  className="bg-slate-50"
                />
                <Text className="text-xs text-slate-500 mt-2">
                  All notifications will be sent to this email address. Update in General Settings.
                </Text>
              </div>
            </div>

            <div className="mt-6">
              <Button 
                type="primary" 
                size="large"
                icon={<SaveOutlined />}
                loading={saving}
                onClick={handleSaveNotifications}
              >
                SAVE NOTIFICATION SETTINGS
              </Button>
            </div>
          </Card>

          {/* System Preferences */}
          <Card 
            className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
            title={
              <div className="flex items-center gap-2">
                <SettingOutlined className="text-orange-600" />
                <Text strong className="text-base">SYSTEM PREFERENCES</Text>
              </div>
            }
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">LANGUAGE</Text>
                <Select
                  value={language}
                  onChange={setLanguage}
                  size="large"
                  options={[
                    { value: 'en', label: 'English' },
                    { value: 'vi', label: 'Tiếng Việt' },
                    { value: 'es', label: 'Español' },
                    { value: 'fr', label: 'Français' },
                  ]}
                />
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">TIMEZONE</Text>
                <Select
                  value={timezone}
                  onChange={setTimezone}
                  size="large"
                  options={[
                    { value: 'UTC+7', label: 'UTC+7 (Bangkok, Hanoi)' },
                    { value: 'UTC+8', label: 'UTC+8 (Singapore, Manila)' },
                    { value: 'UTC+9', label: 'UTC+9 (Tokyo, Seoul)' },
                    { value: 'UTC-5', label: 'UTC-5 (New York)' },
                    { value: 'UTC+0', label: 'UTC+0 (London)' },
                  ]}
                />
              </div>

              <div className="flex flex-col gap-2">
                <Text className="text-sm font-medium text-slate-700">DATE FORMAT</Text>
                <Select
                  value={dateFormat}
                  onChange={setDateFormat}
                  size="large"
                  options={[
                    { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (31/12/2026)' },
                    { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (12/31/2026)' },
                    { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (2026-12-31)' },
                  ]}
                />
              </div>

              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div>
                    <Text className="text-sm font-medium text-slate-700 block">DARK MODE</Text>
                    <Text className="text-xs text-slate-500">Use dark theme</Text>
                  </div>
                  <Switch 
                    checked={darkMode}
                    onChange={setDarkMode}
                  />
                </div>
                <Text className="text-xs text-yellow-600">
                  ⚠️ Dark mode is coming soon
                </Text>
              </div>
            </div>

            <div className="mt-6 flex gap-3">
              <Button 
                type="primary" 
                size="large"
                icon={<SaveOutlined />}
                loading={saving}
                onClick={handleSaveSystem}
              >
                SAVE PREFERENCES
              </Button>
              <Button 
                size="large"
                icon={<ReloadOutlined />}
                onClick={() => window.location.reload()}
              >
                RESET TO DEFAULT
              </Button>
            </div>
          </Card>

          {/* System Information */}
          <Card 
            className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
          >
            <div className="text-center text-slate-500 text-sm">
              <Text className="block">Pharmacy Inventory Management System</Text>
              <Text className="block mt-1">Version 1.0.0 • © 2026 All Rights Reserved</Text>
              <Text className="block mt-1 text-xs">Last Updated: March 1, 2026</Text>
            </div>
          </Card>
            </>
          )}
        </Content>
      </Layout>
    </Layout>
  )
}

export default SettingsPage
