import { Card, Typography } from "antd";
import { useDashboardSummaryQuery } from "../../hooks/useReports";
import ChartWidget from "../../components/reporting/ChartWidget";
import StatsGrid from "./components/StatsGrid";
import DashboardForecastPanel from "./components/DashboardForecastPanel";
import AlertsPanel from "./components/AlertsPanel";
import InventoryTable from "../inventory/components/InventoryTable";
import MainLayout from "../../layouts/MainLayout";

const { Title, Text } = Typography;

const formatInvoiceStatusLabel = (status?: string) => {
  const normalized = status?.toUpperCase();

  switch (normalized) {
    case "PAID":
      return "Đã thanh toán";
    case "UNPAID":
      return "Chưa thanh toán";
    case "PARTIALLY_PAID":
      return "Thanh toán một phần";
    case "OVERDUE":
      return "Quá hạn";
    default:
      return status || "Không xác định";
  }
};

const DashboardPage = () => {
  const { data } = useDashboardSummaryQuery();

  const invoiceData =
    data?.invoiceStatusDistribution?.map((item) => ({
      label: formatInvoiceStatusLabel(item.status),
      value: +item.value || 0,
    })) || [];

  const monthlyData =
    data?.monthlySpending?.map((item) => ({
      label: item.month,
      value: +item.amount || 0,
    })) || [];

  return (
    <MainLayout>
      <StatsGrid />

      {/* Forecast + Alerts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <DashboardForecastPanel />
        <AlertsPanel />
      </div>

      {/* Inventory */}
      <Card className="!rounded-2xl !border-0 shadow-[0_10px_24px_rgba(15,23,42,0.06)]">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Inventory
        </Text>

        <Title level={4} className="!mt-1 !mb-4 font-semibold">
          Tồn kho hiện tại
        </Title>

        <InventoryTable />
      </Card>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartWidget
          title="Trạng thái hóa đơn"
          type="pie"
          data={invoiceData}
        />

        <ChartWidget
          title="Chi tiêu theo tháng"
          type="line"
          data={monthlyData}
        />
      </div>
    </MainLayout>
  );
};

export default DashboardPage;