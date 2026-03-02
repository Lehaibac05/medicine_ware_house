<<<<<<< HEAD
import { Layout } from "antd";
import AlertsPanel from "./components/AlertsPanel";
import ForecastPanel from "./components/ForecastPanel";
import InventoryTable from "../inventory/components/InventoryTable";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";

const { Content, Sider } = Layout;

const warehouseStats = [
  { label: "Tổng số thuốc", value: 12480, note: "32 mới nhập hôm nay" },
  { label: "Sắp hết hạn", value: 186, note: "Cần ưu tiên xuất" },
  { label: "Tồn kho thấp", value: 42, note: "Dưới mức an toàn" },
  { label: "Đơn chờ duyệt", value: 28, note: "7 đơn khẩn" },
];
=======
import { Layout } from 'antd'
import AlertsPanel from './components/AlertsPanel'
import ForecastPanel from './components/ForecastPanel'
import InventoryTable from '../inventory/components/InventoryTable'
import StatsGrid from './components/StatsGrid'
import SidebarNav from './components/SidebarNav'
import TopBar from './components/TopBar'

const { Content, Sider } = Layout
>>>>>>> 1cd68f6a43bf2c1f3c2c1a8b62f0d2a30f3990a5

const DashboardPage = () => {
  return (
    <Layout className="min-h-screen bg-slate-100">
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>
      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
<<<<<<< HEAD
          <TopBar title="Dashboard" subtitle="Warehouse"/>
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseStatsGrid stats={warehouseStats} />
=======
          <TopBar title="Dashboard" subtitle="Overview" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <StatsGrid />
>>>>>>> 1cd68f6a43bf2c1f3c2c1a8b62f0d2a30f3990a5
          <div className="grid gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <ForecastPanel />
            <AlertsPanel />
          </div>
          <InventoryTable />
        </Content>
      </Layout>
    </Layout>
<<<<<<< HEAD
  );
};

export default DashboardPage;
=======
  )
}

export default DashboardPage
>>>>>>> 1cd68f6a43bf2c1f3c2c1a8b62f0d2a30f3990a5
