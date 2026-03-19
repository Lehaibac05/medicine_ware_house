import { Card, Layout, Typography } from "antd"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { useDashboardSummaryQuery } from "../../hooks/useReports"
import ChartWidget from "../../components/reporting/ChartWidget"
import StatsGrid from "./components/StatsGrid"
import ForecastPanel from "./components/ForecastPanel"
import AlertsPanel from "./components/AlertsPanel"
import InventoryTable from "../inventory/components/InventoryTable"

const { Content, Sider } = Layout
const { Text, Title } = Typography

const DashboardPage = () => {
  const { data } = useDashboardSummaryQuery()

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
          <TopBar title="Dashboard" subtitle="Overview" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <StatsGrid />

          <div className="grid gap-6 lg:grid-cols-2">
            <ForecastPanel />
            <AlertsPanel />
          </div>

          <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Inventory snapshot</Text>
            <Title level={4} className="!mt-1 !mb-4">Tồn kho hiện tại</Title>
            <InventoryTable />
          </Card>

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
        </Content>
      </Layout>
    </Layout>
  )
}

export default DashboardPage
