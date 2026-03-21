import { Badge, Button, Menu, Image } from "antd";
import type { MenuProps } from "antd";
import { Link, useLocation } from "react-router-dom";
import logo from "../assets/pharmacy_logo.png";
import { getUserRoles } from "../utils/auth";
import {
  DashboardOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  AppstoreOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  CreditCardOutlined,
  LineChartOutlined,
  AlertOutlined,
  FileTextOutlined,
  UserOutlined,
  SettingOutlined,
} from "@ant-design/icons";

const baseMenuItems: NonNullable<MenuProps["items"]> = [
  {
    key: "dashboard",
    icon: <DashboardOutlined />,
    label: <Link to="/dashboard">Dashboard</Link>,
  },
  {
    key: "inventory",
    icon: <InboxOutlined />,
    label: <Link to="/inventory">Inventory</Link>,
  },
  {
    key: "medicines",
    icon: <MedicineBoxOutlined />,
    label: <Link to="/medicines">Medicines</Link>,
  },
  {
    key: "batches",
    icon: <AppstoreOutlined />,
    label: <Link to="/batches">Batches</Link>,
  },
  {
    key: "issue",
    icon: <ShoppingCartOutlined />,
    label: "Issue",
    children: [
      {
        key: "issue-request",
        label: <Link to="/issue-request">My Issue Requests</Link>,
      },
      {
        key: "issue-create",
        label: <Link to="/issue-request/create">Create Issue Request</Link>,
      },
      {
        key: "issue-approval",
        label: <Link to="/issue-request/approval">Issue Approval</Link>,
      },
      {
        key: "issue-execute",
        label: <Link to="/issue/execute">Issue Execution</Link>,
      },
      {
        key: "issue-history",
        label: <Link to="/issue/history">Issue History</Link>,
      },
    ],
  },
  {
    key: "requests",
    icon: <ShoppingOutlined />,
    label: <Link to="/requests">Requests</Link>,
  },
  {
    key: "medicine-requests",
    icon: <ShoppingOutlined />,
    label: <Link to="/medicine-requests">My Requests</Link>,
  },
  {
    key: "purchase-orders",
    icon: <ShoppingOutlined />,
    label: <Link to="/purchase-orders">Purchase Orders</Link>,
  },
  {
    key: "goods-receipts",
    icon: <InboxOutlined />,
    label: <Link to="/goods-receipts">Goods Receipts</Link>,
  },
  {
    key: "payments",
    icon: <CreditCardOutlined />,
    label: <Link to="/payments">Supplier Invoices</Link>,
  },
  {
    key: "forecast",
    icon: <LineChartOutlined />,
    label: <Link to="/forecast">Forecast</Link>,
  },
  {
    key: "alerts",
    icon: <AlertOutlined />,
    label: <Link to="/alerts">Alerts</Link>,
  },
  {
    key: "reports",
    icon: <FileTextOutlined />,
    label: "Reports",
    children: [
      {
        key: "reports-inventory",
        label: <Link to="/reports/inventory">Inventory Report</Link>,
      },
      {
        key: "reports-financial",
        label: <Link to="/reports/financial">Financial Report</Link>,
      },
      {
        key: "reports-issues",
        label: <Link to="/reports/issues">Issue Report</Link>,
      },
    ],
  },
  {
    key: "users",
    icon: <UserOutlined />,
    label: <Link to="/users">Users Management</Link>,
  },
  {
    key: "settings",
    icon: <SettingOutlined />,
    label: <Link to="/settings">Settings</Link>,
  },
];

function SidebarNav() {
  const { pathname } = useLocation();
  const roles = getUserRoles();

  const isManager = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_WAREHOUSE_MANAGER");
  const isStaffOnly = roles.includes("ROLE_WAREHOUSE_STAFF") && !isManager;
  const canViewInventoryReport =
    roles.includes("ROLE_ADMIN") ||
    roles.includes("ROLE_WAREHOUSE_MANAGER") ||
    roles.includes("ROLE_WAREHOUSE_STAFF") ||
    roles.includes("ROLE_ACCOUNTANT");
  const canViewFinancialReport =
    roles.includes("ROLE_ADMIN") ||
    roles.includes("ROLE_WAREHOUSE_MANAGER") ||
    roles.includes("ROLE_ACCOUNTANT");
  const canViewIssueReport =
    roles.includes("ROLE_ADMIN") ||
    roles.includes("ROLE_WAREHOUSE_MANAGER") ||
    roles.includes("ROLE_WAREHOUSE_STAFF") ||
    roles.includes("ROLE_ACCOUNTANT");
  const canCreateIssueRequest =
    roles.includes("ROLE_ADMIN") ||
    roles.includes("ROLE_WAREHOUSE_MANAGER") ||
    roles.includes("ROLE_WAREHOUSE_STAFF") ||
    roles.includes("ROLE_ACCOUNTANT")
  const canApproveIssue = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_WAREHOUSE_MANAGER")
  const canExecuteIssue = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_WAREHOUSE_STAFF")
  const canViewIssueHistory =
    roles.includes("ROLE_ADMIN") ||
    roles.includes("ROLE_WAREHOUSE_MANAGER") ||
    roles.includes("ROLE_WAREHOUSE_STAFF") ||
    roles.includes("ROLE_ACCOUNTANT")

  const menuItems = baseMenuItems.filter((item) => {
    if (!item || typeof item !== "object") return true;

    if (item.key === "requests") {
      return isManager;
    }

    if (item.key === "medicine-requests") {
      return isStaffOnly;
    }

    if (item.key === "reports") {
      if (!canViewInventoryReport && !canViewFinancialReport && !canViewIssueReport) {
        return false;
      }

      const reportItem = item as Exclude<NonNullable<MenuProps["items"]>[number], null>
      if ("children" in reportItem && Array.isArray(reportItem.children)) {
        reportItem.children = reportItem.children.filter((child) => {
          if (!child || typeof child !== "object") return false
          if (child.key === "reports-inventory") return canViewInventoryReport
          if (child.key === "reports-financial") return canViewFinancialReport
          if (child.key === "reports-issues") return canViewIssueReport
          return true
        })
      }
    }

    if (item.key === "issue") {
      if (!canCreateIssueRequest && !canApproveIssue && !canExecuteIssue && !canViewIssueHistory) {
        return false
      }
      const issueItem = item as Exclude<NonNullable<MenuProps["items"]>[number], null>
      if ("children" in issueItem && Array.isArray(issueItem.children)) {
        issueItem.children = issueItem.children.filter((child) => {
          if (!child || typeof child !== "object") return false
          if (child.key === "issue-request") return canCreateIssueRequest
          if (child.key === "issue-create") return canCreateIssueRequest
          if (child.key === "issue-approval") return canApproveIssue
          if (child.key === "issue-execute") return canExecuteIssue
          if (child.key === "issue-history") return canViewIssueHistory
          return true
        })
      }
    }

    return true;
  });

  const selectedKey =
    pathname.startsWith("/reports/financial")
      ? "reports-financial"
      : pathname.startsWith("/reports/inventory")
      ? "reports-inventory"
    : pathname.startsWith("/reports/issues")
      ? "reports-issues"
    : pathname.startsWith("/issue-request/approval")
      ? "issue-approval"
    : pathname.startsWith("/issue-request/create")
      ? "issue-create"
    : pathname.startsWith("/issue-request")
      ? "issue-request"
    : pathname.startsWith("/issue/execute")
      ? "issue-execute"
    : pathname.startsWith("/issue/history")
      ? "issue-history"
      : pathname.split("/")[1] || "dashboard";
  const openKeys = pathname.startsWith("/reports/")
    ? ["reports"]
    : pathname.startsWith("/issue/") || pathname.startsWith("/issue-request")
      ? ["issue"]
      : []

  return (
    <div className="flex h-full flex-col gap-6">
      <div className="flex items-center gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-xl bg-slate-50">
          <Image
            src={logo}
            preview={false}
            width={32}
            height={32}
            style={{ objectFit: "contain" }}
          />
        </div>
        <div>
          <div className="text-[15px] uppercase tracking-[0.12em] text-slate-400">
            Warehouse
          </div>
          <div className="text-[20px] font-semibold text-slate-900">
            Pharmacy
          </div>
        </div>
      </div>

      <Menu
        mode="inline"
        selectedKeys={[selectedKey]}
        defaultOpenKeys={openKeys}
        items={menuItems}
        className="!border-0"
        inlineCollapsed={false}
      />

      <div className="mt-auto rounded-2xl bg-slate-800 p-4 text-white">
        <div className="text-[11px] uppercase tracking-[0.12em] text-indigo-200">
          AI Insight
        </div>

        <div className="mt-2 text-sm font-semibold">Forecast accuracy</div>
        <div className="mt-1 text-2xl font-bold">98.6%</div>

        <div className="mt-1.5 mb-1.5">
          <Badge
            status="processing"
            text={<span className="text-white">Updated 2 minutes ago</span>}
          />
        </div>

        <Button type="primary" className="mt-3 w-full">
          View insights
        </Button>
      </div>
    </div>
  );
}

export default SidebarNav;
