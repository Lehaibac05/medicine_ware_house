import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"
import type { ReactNode } from "react"
import DashboardPage from "../pages/dashboard/DashboardPage"
import RequesterDashboardPage from "../pages/dashboard/RequesterDashboardPage"
import InventoryPage from "../pages/inventory/InventoryPage"
import InventoryAdjustmentPage from "../pages/inventory/InventoryAdjustmentPage"
import MedicinePage from "../pages/medicine/MedicinePage"
import BatchPage from "../pages/batches/BatchPage"
import LoginPage from "../pages/login/LoginPage"
import { getAuthToken, getUserRoles, type AppRole } from "../utils/auth"
import PaymentPage from "../pages/payments/PaymentPage"
import ForecastPage from "../pages/forecast/ForecastPage"
import AlertsPage from "../pages/alerts/AlertsPage"
import SettingsPage from "../pages/settings/SettingsPage"
import CreateIssueRequestPage from "../pages/issue/CreateIssueRequestPage"
import IssueRequestListPage from "../pages/issue/IssueRequestListPage"
import IssueApprovalPage from "../pages/issue/IssueApprovalPage"
import IssueExecutionPage from "../pages/issue/IssueExecutionPage"
import IssueHistoryPage from "../pages/issue/IssueHistoryPage"
import InventoryReportPage from "../pages/reports/InventoryReportPage"
import FinancialReportPage from "../pages/reports/FinancialReportPage"
import IssueReportPage from "../pages/reports/IssueReportPage"
import UserPage from "../pages/users/UserPage"
import WarehousePage from "../pages/warehouses/WarehousePage"
import SupplierPage from "../pages/suppliers/SupplierPage"
import MedicineRequestsListPage from "../pages/procurement/MedicineRequestsListPage"
import CreateMedicineRequestPage from "../pages/procurement/CreateMedicineRequestPage"
import MedicineRequestApprovalPage from "../pages/procurement/MedicineRequestApprovalPage"
import PurchaseOrdersPage from "../pages/procurement/PurchaseOrdersPage"
import PurchaseOrderDetailPage from "../pages/procurement/PurchaseOrderDetailPage"
import CreatePurchaseOrderPage from "../pages/procurement/CreatePurchaseOrderPage"
import RequestsManagementPage from "../pages/procurement/RequestsManagementPage"
import GoodsReceiptsPage from "../pages/procurement/GoodsReceiptsPage"
import GoodsReceiptCreatePage from "../pages/procurement/GoodsReceiptCreatePage"
import LandingPage from "../pages/landing/LandingPage"
import NotFoundPage from "../pages/errors/NotFoundPage"
import ProfilePage from "../pages/profile/ProfilePage"

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
      return <Navigate to="/not-found" replace />
    }
  }

  return children
}

const DashboardEntry = () => {
  const roles = getUserRoles()
  if (roles.includes("ROLE_REQUESTER")) {
    return <RequesterDashboardPage />
  }
  return <DashboardPage />
}

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<LandingPage />}
        />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT", "ROLE_REQUESTER"]}>
              <DashboardEntry />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/inventory"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT"]}>
              <InventoryPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/inventory/adjustments/new"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT"]}>
              <InventoryAdjustmentPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicines"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT", "ROLE_REQUESTER"]}>
              <MedicinePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/batches"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT"]}>
              <BatchPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/issue-request/create"
          element={
          <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT"]}>
            <CreateIssueRequestPage />
          </ProtectedRoute>
          }
        />
        <Route
          path="/issue-request"
          element={
          <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT", "ROLE_REQUESTER"]}>
            <IssueRequestListPage />
          </ProtectedRoute>
          }
        />
        <Route
          path="/issue-request/approval"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]}>
              <IssueApprovalPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/issue/execute"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_STAFF"]}>
              <IssueExecutionPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/issue/history"
          element={
          <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT"]}>
            <IssueHistoryPage />
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
          path="/supplier-invoices"
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
              <Navigate to="/reports/inventory" replace />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports/inventory"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF", "ROLE_ACCOUNTANT"]}>
              <InventoryReportPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports/financial"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_ACCOUNTANT", "ROLE_WAREHOUSE_MANAGER"]}>
              <FinancialReportPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports/issues"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_ACCOUNTANT", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
              <IssueReportPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
              <UserPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/warehouses"
          element={
            <ProtectedRoute
              allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]}
            >
              <WarehousePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/suppliers"
          element={
            <ProtectedRoute
              allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]}
            >
              <SupplierPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicine-requests"
          element={
            <ProtectedRoute
              allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}
            >
              <MedicineRequestsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/requests"
          element={
            <ProtectedRoute
              allowedRoles={[
                "ROLE_ADMIN",
                "ROLE_WAREHOUSE_MANAGER",
                "ROLE_WAREHOUSE_STAFF",
              ]}
            >
              <RequestsManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/medicine-requests/create"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
              <CreateMedicineRequestPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/requests/new"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
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
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
              <PurchaseOrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/create"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
              <CreatePurchaseOrderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/:id/edit"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"]}>
              <CreatePurchaseOrderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchase-orders/:id"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
              <PurchaseOrderDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/goods-receipts"
          element={
            <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER", "ROLE_WAREHOUSE_STAFF"]}>
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
        <Route path="/not-found" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/not-found" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default AppRouter
