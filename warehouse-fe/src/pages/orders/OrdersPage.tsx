import { Button, DatePicker, Input, Layout, Select, Typography } from 'antd'
import { useState } from 'react'
import type { Dayjs } from 'dayjs'
import SidebarNav from '../dashboard/components/SidebarNav'
import TopBar from '../dashboard/components/TopBar'
import BaseFilterCard from '../../components/base/BaseFilterCard'
import OrdersStatsGrid from './components/OrdersStatsGrid'
import OrdersTable from './components/OrdersTable'

const { Content, Sider } = Layout
const { Text, Title } = Typography

const statusOptions = [
  { value: 'all', label: 'All statuses' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const sortOptions = [
  { value: 'date-desc', label: 'Date (newest)' },
  { value: 'date-asc', label: 'Date (oldest)' },
  { value: 'amount-desc', label: 'Amount (high → low)' },
  { value: 'amount-asc', label: 'Amount (low → high)' },
]

export type OrderFilters = {
  status: string
  sortBy: string
  dateRange: [Dayjs | null, Dayjs | null] | null
  searchText: string
}

const OrdersPage = () => {
  const [filters, setFilters] = useState<OrderFilters>({
    status: 'all',
    sortBy: 'date-desc',
    dateRange: null,
    searchText: '',
  })
  
  const [tempFilters, setTempFilters] = useState<OrderFilters>(filters)

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
          <TopBar title="Orders" subtitle="Management" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <div>
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Orders
            </Text>
            <Title level={3} className="!m-0">
              Order Management
            </Title>
          </div>

          <OrdersStatsGrid />

          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]" onClick={handleApplyFilters}>
                Apply filters
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Order status</Text>
              <Select 
                options={statusOptions} 
                value={tempFilters.status}
                onChange={(value) => setTempFilters(prev => ({ ...prev, status: value }))}
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
                Orders list
              </Text>
              <Title level={4} className="!m-0">
                All orders
              </Title>
            </div>

            <Input.Search
              placeholder="Search by order ID, customer name..."
              className="w-full md:max-w-sm"
              allowClear
              onSearch={handleSearch}
              onChange={(e) => !e.target.value && handleSearch('')}
            />
          </div>

          <OrdersTable filters={filters} />
        </Content>
      </Layout>
    </Layout>
  )
}

export default OrdersPage
