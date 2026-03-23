import { Button, Image, Menu } from "antd";
import { MenuFoldOutlined, MenuUnfoldOutlined } from "@ant-design/icons";
import type { MenuProps } from "antd";
import {
  CreditCardOutlined,
  DashboardOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  InboxOutlined,
  LineChartOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
} from "@ant-design/icons";
import { Link, useLocation } from "react-router-dom";
import logo from "../assets/pharmacy_logo.png";
import { getUserRoles } from "../utils/auth";
import { useEffect, useState } from "react";

type SidebarMenuItem = NonNullable<MenuProps["items"]>[number];

const baseMenuItems: NonNullable<MenuProps["items"]> = [
  {
    key: "overview",
    icon: <DashboardOutlined />,
    label: "Tổng quan",
    children: [
      {
        key: "dashboard",
        label: <Link to="/dashboard">Bảng điều khiển</Link>,
      },
    ],
  },
  {
    key: "warehouse",
    icon: <InboxOutlined />,
    label: "Kho",
    children: [
      {
        key: "inventory",
        label: <Link to="/inventory">Tồn kho</Link>,
      },
      {
        key: "medicines",
        label: <Link to="/medicines">Thuốc</Link>,
      },
      {
        key: "batches",
        label: <Link to="/batches">Lô thuốc</Link>,
      },
      {
        key: "warehouses",
        label: <Link to="/warehouses">Quản lí kho</Link>,
      },
    ],
  },
  {
    key: "procurement",
    icon: <ShoppingCartOutlined />,
    label: "Đơn hàng & yêu cầu",
    children: [
      {
        key: "orders",
        label: <Link to="/orders">Đơn hàng</Link>,
      },
      {
        key: "requests",
        label: <Link to="/requests">Yêu cầu</Link>,
      },
      {
        key: "medicine-requests",
        label: <Link to="/medicine-requests">Yêu cầu của tôi</Link>,
      },
      {
        key: "purchase-orders",
        label: <Link to="/purchase-orders">Đơn mua hàng</Link>,
      },
      {
        key: "goods-receipts",
        label: <Link to="/goods-receipts">Phiếu nhập kho</Link>,
      },
      {
        key: "suppliers",
        label: <Link to="/suppliers">Nhà cung cấp</Link>,
      },
    ],
  },
  {
    key: "issue",
    icon: <ExclamationCircleOutlined />,
    label: "Vấn đề",
    children: [
      {
        key: "issue-request",
        label: <Link to="/issue-request">Danh sách yêu cầu vấn đề</Link>,
      },
      {
        key: "issue-create",
        label: <Link to="/issue-request/create">Tạo yêu cầu vấn đề</Link>,
      },
      {
        key: "issue-approval",
        label: <Link to="/issue-request/approval">Phê duyệt vấn đề</Link>,
      },
      {
        key: "issue-execute",
        label: <Link to="/issue/execute">Thực hiện vấn đề</Link>,
      },
      {
        key: "issue-history",
        label: <Link to="/issue/history">Lịch sử vấn đề</Link>,
      },
    ],
  },
  {
    key: "finance",
    icon: <CreditCardOutlined />,
    label: "Tài chính",
    children: [
      {
        key: "payments",
        label: <Link to="/payments">Hóa đơn nhà cung cấp</Link>,
      },
    ],
  },
  {
    key: "reports",
    icon: <FileTextOutlined />,
    label: "Báo cáo",
    children: [
      {
        key: "reports-inventory",
        label: <Link to="/reports/inventory">Báo cáo tồn kho</Link>,
      },
      {
        key: "reports-financial",
        label: <Link to="/reports/financial">Báo cáo tài chính</Link>,
      },
      {
        key: "reports-issues",
        label: <Link to="/reports/issues">Báo cáo vấn đề</Link>,
      },
    ],
  },
  {
    key: "analytics",
    icon: <LineChartOutlined />,
    label: "Phân tích",
    children: [
      {
        key: "forecast",
        label: <Link to="/forecast">Dự báo</Link>,
      },
      {
        key: "alerts",
        label: <Link to="/alerts">Cảnh báo</Link>,
      },
    ],
  },
  {
    key: "system",
    icon: <SettingOutlined />,
    label: "Hệ thống",
    children: [
      {
        key: "users",
        label: <Link to="/users">Quản lý người dùng</Link>,
      },
      {
        key: "settings",
        label: <Link to="/settings">Cài đặt</Link>,
      },
    ],
  },
];

const filterMenuItems = (
  items: NonNullable<MenuProps["items"]>,
  flags: {
    isManager: boolean;
    isStaffOnly: boolean;
    canViewInventoryReport: boolean;
    canViewFinancialReport: boolean;
    canViewIssueReport: boolean;
    canCreateIssueRequest: boolean;
    canApproveIssue: boolean;
    canExecuteIssue: boolean;
    canViewIssueHistory: boolean;
  },
): NonNullable<MenuProps["items"]> =>
  items
    .map((item) => {
      if (!item || typeof item !== "object") return item;

      const nextItem: Exclude<SidebarMenuItem, null> = { ...item };

      if (nextItem.key === "requests" && !flags.isManager) {
        return null;
      }

      if (nextItem.key === "warehouses" && !flags.isManager) {
        return null;
      }

      if (nextItem.key === "suppliers" && !flags.isManager) {
        return null;
      }

      if (nextItem.key === "medicine-requests" && !flags.isStaffOnly) {
        return null;
      }

      if (
        nextItem.key === "reports-inventory" &&
        !flags.canViewInventoryReport
      ) {
        return null;
      }

      if (
        nextItem.key === "reports-financial" &&
        !flags.canViewFinancialReport
      ) {
        return null;
      }

      if (
        nextItem.key === "reports-issues" &&
        !flags.canViewIssueReport
      ) {
        return null;
      }

      if (nextItem.key === "issue-request" && !flags.canCreateIssueRequest) {
        return null;
      }

      if (nextItem.key === "issue-create" && !flags.canCreateIssueRequest) {
        return null;
      }

      if (nextItem.key === "issue-approval" && !flags.canApproveIssue) {
        return null;
      }

      if (nextItem.key === "issue-execute" && !flags.canExecuteIssue) {
        return null;
      }

      if (nextItem.key === "issue-history" && !flags.canViewIssueHistory) {
        return null;
      }

      if ("children" in nextItem && Array.isArray(nextItem.children)) {
        const children = filterMenuItems(nextItem.children, flags);
        if (children.length === 0) {
          return null;
        }
        nextItem.children = children;
      }

      return nextItem;
    })
    .filter((item): item is Exclude<SidebarMenuItem, null> => item !== null);

const findOpenKeys = (
  items: NonNullable<MenuProps["items"]>,
  targetKey: string,
  parents: string[] = [],
): string[] => {
  for (const item of items) {
    if (!item || typeof item !== "object") continue;

    if (item.key === targetKey) {
      return parents;
    }

    if ("children" in item && Array.isArray(item.children)) {
      const nested = findOpenKeys(item.children, targetKey, [
        ...parents,
        String(item.key),
      ]);
      if (nested.length > 0) {
        return nested;
      }
    }
  }

  return [];
};

function SidebarNav({
  collapsed,
  setCollapsed,
}: {
  collapsed: boolean;
  setCollapsed: (val: boolean) => void;
}) {
  const { pathname } = useLocation();
  const roles = getUserRoles();

  const isManager =
    roles.includes("ROLE_ADMIN") || roles.includes("ROLE_WAREHOUSE_MANAGER");
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
    roles.includes("ROLE_ACCOUNTANT");
  const canApproveIssue = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_WAREHOUSE_MANAGER");
  const canExecuteIssue = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_WAREHOUSE_STAFF");
  const canViewIssueHistory =
    roles.includes("ROLE_ADMIN") ||
    roles.includes("ROLE_WAREHOUSE_MANAGER") ||
    roles.includes("ROLE_WAREHOUSE_STAFF") ||
    roles.includes("ROLE_ACCOUNTANT");

  const menuItems = filterMenuItems(baseMenuItems, {
    isManager,
    isStaffOnly,
    canViewInventoryReport,
    canViewFinancialReport,
    canViewIssueReport,
    canCreateIssueRequest,
    canApproveIssue,
    canExecuteIssue,
    canViewIssueHistory,
  });

  const selectedKey = pathname.startsWith("/reports/financial")
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

  const defaultOpenKeys = findOpenKeys(menuItems, selectedKey);
  const [openKeys, setOpenKeys] = useState<string[]>(defaultOpenKeys);

  const handleOpenChange = (keys: string[]) => {
    if (collapsed) return; 
    setOpenKeys(keys);
  };

  useEffect(() => {
    if (collapsed) {
      setOpenKeys([]);
    } else {
      setOpenKeys(defaultOpenKeys);
    }
  }, [collapsed, selectedKey]);

  return (
    <div className="flex h-full min-h-0 flex-col gap-6 overflow-hidden">
      <div className="flex items-center justify-between">
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

          {!collapsed && (
            <div>
              <div className="text-[15px] uppercase tracking-[0.12em] text-slate-400">
                Warehouse
              </div>
              <div className="text-[20px] font-semibold text-slate-900">
                Pharmacy
              </div>
            </div>
          )}
        </div>

        <Button
          type="text"
          className="rounded-full cursor-pointer"
          onClick={() => {
            setCollapsed(!collapsed);
          }}
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
        />
      </div>

      <div className="sidebar-shell min-h-0 flex-1 overflow-y-auto pr-1">
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          openKeys={openKeys}
          onOpenChange={handleOpenChange}
          items={menuItems}
          className={`sidebar-nav-menu !border-0 ${
            collapsed ? "sidebar-collapsed" : ""
          }`}
          inlineIndent={18}
        />
      </div>
    </div>
  );
}

export default SidebarNav;