import { Layout } from 'antd'
import AlertsPanel from './components/AlertsPanel'
import ForecastPanel from './components/ForecastPanel'
import InventoryTable from '../inventory/components/InventoryTable'
import StatsGrid from './components/StatsGrid'
import SidebarNav from './components/SidebarNav'
import TopBar from './components/TopBar'

const { Content, Sider } = Layout

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
          <TopBar title="Dashboard" subtitle="Overview" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <StatsGrid />
          <div className="grid gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <ForecastPanel />
            <AlertsPanel />
          </div>
          <InventoryTable />
        </Content>
      </Layout>
    </Layout>
  )
}

export default DashboardPage
