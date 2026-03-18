import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"
import type { ReactNode } from "react"
import DashboardPage from "../pages/dashboard/DashboardPage"
import InventoryPage from "../pages/inventory/InventoryPage"
import InventoryDetailPage from "../pages/inventory/InventoryDetailPage"
import InventoryAdjustmentPage from "../pages/inventory/InventoryAdjustmentPage"
import MedicinePage from "../pages/medicine/MedicinePage"
import BatchPage from "../pages/batches/BatchPage"
import LoginPage from "../pages/login/LoginPage"
import { getAuthToken, getUserRoles, type AppRole } from "../utils/auth"
import PaymentPage from "../pages/payments/PaymentPage"
import ForecastPage from "../pages/forecast/ForecastPage"
import AlertsPage from "../pages/alerts/AlertsPage"
import SettingsPage from "../pages/settings/SettingsPage"
import OrdersPage from "../pages/orders/OrdersPage"
import ReportPage from "../pages/reports/ReportPage"
import UserPage from "../pages/users/UserPage"
import MedicineRequestsListPage from "../pages/procurement/MedicineRequestsListPage"
import CreateMedicineRequestPage from "../pages/procurement/CreateMedicineRequestPage"
import MedicineRequestApprovalPage from "../pages/procurement/MedicineRequestApprovalPage"
import PurchaseOrdersPage from "../pages/procurement/PurchaseOrdersPage"
import PurchaseOrderDetailPage from "../pages/procurement/PurchaseOrderDetailPage"
import CreatePurchaseOrderPage from "../pages/procurement/CreatePurchaseOrderPage"
import RequestsManagementPage from "../pages/procurement/RequestsManagementPage"
import GoodsReceiptsPage from "../pages/procurement/GoodsReceiptsPage"
import GoodsReceiptCreatePage from "../pages/procurement/GoodsReceiptCreatePage"

const ProtectedRoute = ({
  children,
  allowedRoles,
}: {
  children: ReactNode
  allowedRoles?: AppRole[]
}) => {
  const token = getAuthToken()

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const userRoles = getUserRoles()
    const allowed = allowedRoles.some((role) => userRoles.includes(role))

    if (!allowed) {
      return <Navigate to="/dashboard" replace />
    }
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
          path="/inventory/:medicineId"
          element={
            <ProtectedRoute>
              <InventoryDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/inventory/adjustments/new"
          element={
            <ProtectedRoute>
              <InventoryAdjustmentPage />
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
              <AlertsPage />
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
        <Route
          path="/reports"
          element={
            <ProtectedRoute>
              <ReportPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute>
              <UserPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicine-requests"
          element={
            <ProtectedRoute
              allowedRoles={[
                "ROLE_ADMIN",
                "ROLE_WAREHOUSE_MANAGER",
                "ROLE_WAREHOUSE_STAFF",
              ]}
            >
              <MedicineRequestsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/requests"
          element={
            <ProtectedRoute
              allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]}
            >
              <RequestsManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicine-requests/create"
          element={
            <ProtectedRoute>
              <CreateMedicineRequestPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/requests/new"
          element={
            <ProtectedRoute>
              <CreateMedicineRequestPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicine-requests/approval"
          element={
            <ProtectedRoute
              allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]}
            >
              <MedicineRequestApprovalPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders"
          element={
            <ProtectedRoute>
              <PurchaseOrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/create"
          element={
            <ProtectedRoute>
              <CreatePurchaseOrderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/:id/edit"
          element={
            <ProtectedRoute>
              <CreatePurchaseOrderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/:id"
          element={
            <ProtectedRoute>
              <PurchaseOrderDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/goods-receipts"
          element={
            <ProtectedRoute>
              <GoodsReceiptsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/goods-receipts/new/:purchaseOrderId"
          element={
            <ProtectedRoute>
              <GoodsReceiptCreatePage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}

export default AppRouter
