import { Button, DatePicker, Input, Layout, Select, Typography } from 'antd'
import { useState } from 'react'
import type { Dayjs } from 'dayjs'
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from '../../components/base/BaseFilterCard'
import AlertsStatsGrid from './components/AlertsStatsGrid'
import AlertsTable from './components/AlertsTable'

const { Content, Sider } = Layout
const { Text, Title } = Typography

const alertTypeOptions = [
  { value: 'all', label: 'All types' },
  { value: 'LOW_STOCK', label: 'Low stock' },
  { value: 'EXPIRING_SOON', label: 'Expiring soon' },
  { value: 'EXPIRED', label: 'Expired' },
  { value: 'SYSTEM', label: 'System warning' },
]

const severityOptions = [
  { value: 'all', label: 'All severity' },
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
  { value: 'CRITICAL', label: 'Critical' },
]

const sortOptions = [
  { value: 'date-desc', label: 'Date (newest)' },
  { value: 'date-asc', label: 'Date (oldest)' },
  { value: 'severity-desc', label: 'Severity (high → low)' },
]

export type AlertFilters = {
  alertType: string
  severity: string
  sortBy: string
  dateRange: [Dayjs | null, Dayjs | null] | null
  searchText: string
}

const AlertsPage = () => {
  const [filters, setFilters] = useState<AlertFilters>({
    alertType: 'all',
    severity: 'all',
    sortBy: 'date-desc',
    dateRange: null,
    searchText: '',
  })
  
  const [tempFilters, setTempFilters] = useState<AlertFilters>(filters)

  const handleApplyFilters = () => {
    setFilters(tempFilters)
  }
  
  const handleSearch = (value: string) => {
    setFilters(prev => ({ ...prev, searchText: value }))
  }

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
          <TopBar title="Alerts" subtitle="Monitoring" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <AlertsStatsGrid />

          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]" onClick={handleApplyFilters}>
                Apply filters
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Alert type</Text>
              <Select 
                options={alertTypeOptions} 
                value={tempFilters.alertType}
                onChange={(value) => setTempFilters(prev => ({ ...prev, alertType: value }))}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Severity</Text>
              <Select 
                options={severityOptions} 
                value={tempFilters.severity}
                onChange={(value) => setTempFilters(prev => ({ ...prev, severity: value }))}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Sort by</Text>
              <Select 
                options={sortOptions} 
                value={tempFilters.sortBy}
                onChange={(value) => setTempFilters(prev => ({ ...prev, sortBy: value }))}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Date range</Text>
              <DatePicker.RangePicker 
                format="DD/MM/YYYY" 
                className="w-full"
                value={tempFilters.dateRange}
                onChange={(dates) => setTempFilters(prev => ({ ...prev, dateRange: dates }))}
              />
            </div>
          </BaseFilterCard>

          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="flex flex-col">
              <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
                Alerts list
              </Text>
              <Title level={4} className="!m-0">
                Current alerts
              </Title>
            </div>

            <Input.Search
              placeholder="Search by alert id, medicine, warehouse..."
              className="w-full md:max-w-sm"
              allowClear
              onSearch={handleSearch}
              onChange={(e) => !e.target.value && handleSearch('')}
            />
          </div>

          <AlertsTable filters={filters} />
        </Content>
      </Layout>
    </Layout>
  )
}

export default AlertsPage

