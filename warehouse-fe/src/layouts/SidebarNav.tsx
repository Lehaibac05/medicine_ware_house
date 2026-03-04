import { Badge, Button, Menu, Image } from "antd";
import type { MenuProps } from "antd";
import { Link, useLocation } from "react-router-dom";
import logo from "../assets/pharmacy_logo.png";
import {
  DashboardOutlined,
  InboxOutlined,
  MedicineBoxOutlined,
  AppstoreOutlined,
  ShoppingCartOutlined,
  CreditCardOutlined,
  LineChartOutlined,
  AlertOutlined,
  FileTextOutlined,
  UserOutlined,
  SettingOutlined,
} from "@ant-design/icons";

const menuItems: MenuProps["items"] = [
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
    key: "orders",
    icon: <ShoppingCartOutlined />,
    label: <Link to="/orders">Orders</Link>,
  },
  {
    key: "payments",
    icon: <CreditCardOutlined />,
    label: <Link to="/payments">Payments</Link>,
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
    label: <Link to="/reports">Reports</Link>,
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

  const selectedKey = pathname.split("/")[1] || "dashboard";

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
