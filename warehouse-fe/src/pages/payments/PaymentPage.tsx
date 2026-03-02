import { Button, Input, Layout, Select, Typography, DatePicker } from "antd";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import PaymentTable from "./components/PaymentTable";
import BaseStatsGrid from "../../components/base/BaseStatsGrid";

const { Content, Sider } = Layout;
const { Text } = Typography;
const { RangePicker } = DatePicker;

const statusOptions = [
  { value: "all", label: "All status" },
  { value: "completed", label: "Completed" },
  { value: "pending", label: "Pending" },
  { value: "cancelled", label: "Cancelled" },
];

const methodOptions = [
  { value: "all", label: "All methods" },
  { value: "bank-transfer", label: "Bank Transfer" },
  { value: "cash", label: "Cash" },
  { value: "credit-card", label: "Credit Card" },
  { value: "e-wallet", label: "E-Wallet" },
];

const paymentStats = [
  {
    label: "Tổng giao dịch",
    value: 12480,
    note: "32 giao dịch mới hôm nay",
  },
  {
    label: "Thanh toán hoàn thành",
    value: 11896,
    note: "95% tỷ lệ thành công",
  },
  {
    label: "Đang chờ xử lý",
    value: 42,
    note: "Cần xác nhận từ hệ thống",
  },
  {
    label: "Thanh toán thất bại",
    value: 542,
    note: "Cần kiểm tra lại phương thức thanh toán",
  },
];

const PaymentPage = () => {
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
          <TopBar title="Payments" subtitle="Finance" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseStatsGrid stats={paymentStats} />

          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]">
                Apply
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Payment ID</Text>
              <Input placeholder="Enter payment ID" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Payment Method</Text>
              <Select options={methodOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Payment Status</Text>
              <Select options={statusOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Payment Date Range</Text>
              <RangePicker
                className="w-full"
                format="DD/MM/YYYY"
                placeholder={["Start date", "End date"]}
              />
            </div>
          </BaseFilterCard>

          <PaymentTable />
        </Content>
      </Layout>
    </Layout>
  );
};

export default PaymentPage;
