import { Button, DatePicker, Layout, Select, Typography } from "antd";
import { useState } from "react";
import type { Dayjs } from "dayjs";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import OrdersStatsGrid from "./components/OrdersStatsGrid";
import OrdersTable from "./components/OrdersTable";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";

const { Content, Sider } = Layout;
const { Text } = Typography;

const statusOptions = [
  { value: "all", label: "All statuses" },
  { value: "PENDING", label: "Pending" },
  { value: "PROCESSING", label: "Processing" },
  { value: "COMPLETED", label: "Completed" },
  { value: "CANCELLED", label: "Cancelled" },
];

const sortOptions = [
  { value: "date-desc", label: "Date (newest)" },
  { value: "date-asc", label: "Date (oldest)" },
  { value: "amount-desc", label: "Amount (high → low)" },
  { value: "amount-asc", label: "Amount (low → high)" },
];

export type OrderFilters = {
  status: string;
  sortBy: string;
  dateRange: [Dayjs | null, Dayjs | null] | null;
  searchText: string;
};

const OrdersPage = () => {
  const [filters, setFilters] = useState<OrderFilters>({
    status: "all",
    sortBy: "date-desc",
    dateRange: null,
    searchText: "",
  });

  const [tempFilters, setTempFilters] = useState<OrderFilters>(filters);

  const handleApplyFilters = () => {
    setFilters(tempFilters);
  };

  const handleSearch = (value: string) => {
    setFilters((prev) => ({ ...prev, searchText: value }));
  };

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
          <OrdersStatsGrid />

          <BaseFilterCard
            actions={
              <Button
                type="primary"
                className="h-[40px]"
                onClick={handleApplyFilters}
              >
                Apply filters
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Order status</Text>
              <Select
                options={statusOptions}
                value={tempFilters.status}
                onChange={(value) =>
                  setTempFilters((prev) => ({ ...prev, status: value }))
                }
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Sort by</Text>
              <Select
                options={sortOptions}
                value={tempFilters.sortBy}
                onChange={(value) =>
                  setTempFilters((prev) => ({ ...prev, sortBy: value }))
                }
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Date range</Text>
              <DatePicker.RangePicker
                format="DD/MM/YYYY"
                className="w-full"
                value={tempFilters.dateRange}
                onChange={(dates) =>
                  setTempFilters((prev) => ({ ...prev, dateRange: dates }))
                }
              />
            </div>
          </BaseFilterCard>

          <OrdersTable filters={filters} onSearch={handleSearch} />
        </Content>
      </Layout>
    </Layout>
  );
};

export default OrdersPage;
