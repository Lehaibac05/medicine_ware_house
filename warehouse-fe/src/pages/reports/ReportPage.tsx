import { Button, Card, DatePicker, Input, Layout, Select, Typography } from "antd";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";
import ReportTable from "./components/ReportTable";

const { Content, Sider } = Layout;
const { Text } = Typography;
const { RangePicker } = DatePicker;

const reportTypeOptions = [
  { value: "all", label: "Select type" },
  { value: "sales-summary", label: "Sales Summary" },
  { value: "inventory-audit", label: "Inventory Audit" },
  { value: "waste-report", label: "Waste Report" },
  { value: "payments-report", label: "Payments Report" },
];

const warehouseOptions = [
  { value: "all", label: "All warehouses" },
  { value: "main", label: "Main Warehouse" },
  { value: "cold-1", label: "Cold Storage 1" },
  { value: "cold-2", label: "Cold Storage 2" },
  { value: "secondary", label: "Secondary Warehouse" },
];

const reportStats = [
  { label: "Total revenue", value: "0000", note: "Revenue in selected period" },
  { label: "Total orders", value: "0000", note: "Orders matched by filters" },
  { label: "Total medicines sold", value: "0000", note: "Units sold in reports" },
  { label: "Total payments", value: "0000", note: "Successful payments count" },
];

const ReportPage = () => {
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
          <TopBar title="Reports" subtitle="Analytics" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]">
                Apply filters
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Report Type</Text>
              <Select options={reportTypeOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Date Range</Text>
              <RangePicker
                className="w-full"
                format="YYYY-MM-DD"
                placeholder={["YYYY-MM-DD", "YYYY-MM-DD"]}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Warehouse</Text>
              <Select options={warehouseOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Report ID</Text>
              <Input placeholder="Enter report ID" />
            </div>
          </BaseFilterCard>

          <BaseStatsGrid stats={reportStats} />

          <Card className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)] min-h-[320px] sm:min-h-[420px]">
            <div className="grid h-full min-h-[380px] place-items-center rounded-xl border border-dashed border-slate-300 bg-slate-50/80">
              <Text className="text-xs sm:text-sm uppercase tracking-[0.16em] text-slate-400 font-semibold text-center">
                Report visualization placeholder
              </Text>
            </div>
          </Card>

          <ReportTable />
        </Content>
      </Layout>
    </Layout>
  );
};

export default ReportPage;
