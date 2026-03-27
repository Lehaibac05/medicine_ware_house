import { Card, Typography } from "antd";
import { useDashboardSummaryQuery } from "../../hooks/useReports";
import ChartWidget from "../../components/reporting/ChartWidget";
import StatsGrid from "./components/StatsGrid";
import ForecastPanel from "./components/ForecastPanel";
import AlertsPanel from "./components/AlertsPanel";
import InventoryTable from "../inventory/components/InventoryTable";
import MainLayout from "../../layouts/MainLayout";

const { Title } = Typography;

const formatInvoiceStatusLabel = (status?: string) => {
  const normalized = status?.toUpperCase();
  if (normalized === "PAID") return "Đã thanh toán";
  if (normalized === "UNPAID") return "Chưa thanh toán";
  if (normalized === "PARTIALLY_PAID") return "Thanh toán một phần";
  if (normalized === "OVERDUE") return "Quá hạn";
  return status || "Không xác định";
};

const DashboardPage = () => {
  const { data } = useDashboardSummaryQuery();

  return (
    <MainLayout>
      <StatsGrid />

      {/* Forecast + Alerts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ForecastPanel />
        <AlertsPanel />
      </div>

      {/* Tồn kho */}
      <Card className="!rounded-2xl !border-0 shadow-[0_10px_24px_rgba(15,23,42,0.06)]">

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
          data={(data?.invoiceStatusDistribution || []).map((item) => ({
            label: formatInvoiceStatusLabel(item.status),
            value: Number(item.value || 0),
          }))}
        />

        <ChartWidget
          title="Chi tiêu theo tháng"
          type="line"
          data={(data?.monthlySpending || []).map((item) => ({
            label: item.month,
            value: Number(item.amount || 0),
          }))}
        />
      </div>
    </MainLayout>
  );
};

export default DashboardPage;
