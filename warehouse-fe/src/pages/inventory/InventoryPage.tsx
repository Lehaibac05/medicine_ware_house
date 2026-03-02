import {
  Button,
  DatePicker,
  Input,
  Layout,
  Select,
  Typography,
} from "antd";
import InventoryTable from "./components/InventoryTable";
import SidebarNav from "../dashboard/components/SidebarNav";
import TopBar from "../dashboard/components/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";

const { Content, Sider } = Layout;
const { Text, Title } = Typography;

const warehouseOptions = [
  { value: "all", label: "All warehouses" },
  { value: "main", label: "Main Warehouse" },
  { value: "cold-1", label: "Cold Storage 1" },
  { value: "cold-2", label: "Cold Storage 2" },
  { value: "secondary", label: "Secondary Warehouse" },
];

const statusOptions = [
  { value: "all", label: "All status" },
  { value: "in-stock", label: "In stock" },
  { value: "low", label: "Low" },
  { value: "overstock", label: "Overstock" },
];

const InventoryPage = () => {
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
          <TopBar title="Inventory" subtitle="Warehouse" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <div>
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Inventory
            </Text>
            <Title level={3} className="!m-0">
              Inventory Management
            </Title>
          </div>

          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]">
                Apply
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Warehouse</Text>
              <Select options={warehouseOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine name</Text>
              <Input placeholder="Enter medicine name" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select options={statusOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Expiry date</Text>
              <DatePicker.RangePicker format="DD/MM/YYYY" className="w-full" />
            </div>
          </BaseFilterCard>

          <InventoryTable />
        </Content>
      </Layout>
    </Layout>
  );
};

export default InventoryPage;
