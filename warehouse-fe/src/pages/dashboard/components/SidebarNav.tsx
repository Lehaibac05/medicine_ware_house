import { Badge, Button, Menu } from "antd";
import type { MenuProps } from "antd";
import { Link, useLocation } from "react-router-dom";

const menuItems: MenuProps["items"] = [
  { key: "dashboard", label: <Link to="/dashboard">Dashboard</Link> },
  { key: "inventory", label: <Link to="/inventory">Inventory</Link> },
  { key: "medicines", label: <Link to="/medicines">Medicines</Link> },
  { key: "batches", label: <Link to="/batches">Batches</Link> },
  { key: "orders", label: <Link to="/orders">Orders</Link> },
  { key: "payments", label: "Payments" },
  { key: "forecast", label: "AI Forecast" },
  { key: "alerts", label: <Link to="/alerts">Alerts</Link> },
  { key: "reports", label: "Reports" },
  { key: "users", label: "User Management" },
  { key: "settings", label: <Link to="/settings">System Settings</Link> },
];

function SidebarNav() {
  const { pathname } = useLocation();

  // lấy segment đầu tiên sau '/'
  const selectedKey = pathname.split("/")[1] || "dashboard";

  return (
    <div className="flex h-full flex-col gap-6">
      <div className="flex items-center gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-xl bg-slate-900 text-white font-bold">
          WH
        </div>
        <div>
          <div className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            Warehouse
          </div>
          <div className="text-[15px] font-semibold text-slate-900">
            Pharmacy
          </div>
        </div>
      </div>

      <Menu
        mode="inline"
        selectedKeys={[selectedKey]}
        items={menuItems}
        className="!border-0"
      />

      <div className="mt-auto rounded-2xl bg-slate-900 p-4 text-white">
        <div className="text-[11px] uppercase tracking-[0.12em] text-indigo-200">
          AI Insight
        </div>

        <div className="mt-2 text-sm font-semibold">Forecast accuracy</div>
        <div className="mt-1 text-2xl font-bold">98.6%</div>

        <div className="mt-1.5 !text-white">
          <Badge status="processing" text="Updated 2 minutes ago" />
        </div>

        <Button type="primary" className="mt-3 w-full">
          View insights
        </Button>
      </div>
    </div>
  );
}

export default SidebarNav;
