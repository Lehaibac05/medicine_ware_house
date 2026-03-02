import {
  Button,
  Input,
  Layout,
  Select,
  Typography,
} from "antd";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import BatchTable from "./components/BatchTable";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";

const { Content, Sider } = Layout;
const { Text } = Typography;

const manufacturerOptions = [
  { value: "all", label: "All manufacturer" },
  { value: "dhg-pharma", label: "DHG Pharma"},
  { value: "imexpharm", label: "Imexpharm" },
  { value: "sanofi", label: "Sanofi" },
  { value: "traphaco", label: "Traphaco" },
];

const conditionOptions = [
  { value: "all", label: "All condition" },
  { value: "room-temp", label: "Room temperature" },
  { value: "dry-place", label: "Dry place"},
  { value: "cold-storage", label: "Cold storage" },
  { value: "refrigerated", label: "Refrigerated" },
]

const statusOptions = [
  { value: "all", label: "All status" },
  { value: "in-stock", label: "In stock" },
  { value: "low", label: "Low" },
  { value: "overstock", label: "Overstock" },
];


const MedicinePage = () => {
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
          <TopBar title="Batches" subtitle="Warehouse" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseFilterCard
            actions={
              <Button type="primary" className="h-[40px]">
                Apply
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine name</Text>
              <Input placeholder="Enter medicine name" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Manufacturer</Text>
              <Select options={manufacturerOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Storage Condition</Text>
              <Select options={conditionOptions} defaultValue="all" />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select options={statusOptions} defaultValue="all" />
            </div>
          </BaseFilterCard>

          <BatchTable />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MedicinePage;
