import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"
import type { ReactNode } from "react"
import DashboardPage from "../pages/dashboard/DashboardPage"
import InventoryPage from "../pages/inventory/InventoryPage"
import MedicinePage from "../pages/medicine/MedicinePage"
import BatchPage from "../pages/batches/BatchPage"
import LoginPage from "../pages/login/LoginPage"
import { getAuthToken } from "../utils/auth"
import PaymentPage from "../pages/payments/PaymentPage"
import ForecastPage from "../pages/forecast/ForecastPage"
import AlertPage from "../pages/alerts/AlertsPage"
import SettingsPage from "../pages/settings/SettingsPage"
import OrdersPage from "../pages/orders/OrdersPage"

const ProtectedRoute = ({ children }: { children: ReactNode }) => {
  const token = getAuthToken()

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return children
}

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <Navigate to={getAuthToken() ? "/dashboard" : "/login"} replace />
          }
        />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/inventory"
          element={
            <ProtectedRoute>
              <InventoryPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicines"
          element={
            <ProtectedRoute>
              <MedicinePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/batches"
          element={
            <ProtectedRoute>
              <BatchPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payments"
          element={
            <ProtectedRoute>
              <PaymentPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/forecast"
          element={
            <ProtectedRoute>
              <ForecastPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/alerts"
          element={
            <ProtectedRoute>
              <AlertPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <SettingsPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default AppRouter
