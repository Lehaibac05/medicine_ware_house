import { Button, Layout, Select, Typography } from "antd";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import ForecastTable from "./components/ForecastTable";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";
import ForecastPanel from "../dashboard/components/ForecastPanel";

const { Content, Sider } = Layout;
const { Text } = Typography;

const riskOptions = [
  { value: "all", label: "All risk levels" },
  { value: "low", label: "Low" },
  { value: "medium", label: "Medium" },
  { value: "high", label: "High" },
];

const periodOptions = [
  { value: "all", label: "All periods" },
  { value: "q1-2026", label: "Q1 2026" },
  { value: "q2-2026", label: "Q2 2026" },
  { value: "q3-2026", label: "Q3 2026" },
  { value: "q4-2026", label: "Q4 2026" },
];

const medicineOptions = [
  { value: "all", label: "All medicines" },
  { value: "paracetamol", label: "Paracetamol 500mg" },
  { value: "amoxicillin", label: "Amoxicillin 250mg" },
  { value: "insulin", label: "Insulin Glargine" },
  { value: "vitamin-c", label: "Vitamin C 500mg" },
];

const forecastStats = [
  {
    label: "Dự báo kỳ này",
    value: 18,
    note: "Áp dụng cho Q1 2026",
  },
  {
    label: "Rủi ro cao",
    value: 4,
    note: "Nguy cơ thiếu hàng trong 30 ngày",
  },
  {
    label: "Độ tin cậy TB",
    value: "89%",
    note: "Dựa trên dữ liệu 12 tháng gần nhất",
  },
  {
    label: "Cần nhập thêm",
    value: 6,
    note: "Đề xuất tăng tồn kho ngay",
  },
];

const ForecastPage = () => {
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
          <TopBar title="Forecast" subtitle="Inventory Analytics" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseStatsGrid stats={forecastStats} />
          <ForecastPanel />
          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]">
                Apply
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine</Text>
              <Select options={medicineOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Risk Level</Text>
              <Select options={riskOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Forecast Period</Text>
              <Select options={periodOptions} defaultValue="all" />
            </div>
          </BaseFilterCard>

          <ForecastTable />
        </Content>
      </Layout>
    </Layout>
  );
};

export default ForecastPage;
