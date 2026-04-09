import { Card, Typography } from "antd";
import { useDashboardSummaryQuery } from "../../hooks/useReports";
import ChartWidget from "../../components/reporting/ChartWidget";
import StatsGrid from "./components/StatsGrid";
import DashboardForecastPanel from "./components/DashboardForecastPanel";
import AlertsPanel from "./components/AlertsPanel";
import InventoryTable from "../inventory/components/InventoryTable";
import MainLayout from "../../layouts/MainLayout";

const { Text, Title } = Typography;

const DashboardPage = () => {
  const { data } = useDashboardSummaryQuery();

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
          title="Invoice Status"
          type="pie"
          data={(data?.invoiceStatusDistribution || []).map((item) => ({
            label: item.status,
            value: Number(item.value || 0),
          }))}
        />

        <ChartWidget
          title="Monthly Spending"
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
