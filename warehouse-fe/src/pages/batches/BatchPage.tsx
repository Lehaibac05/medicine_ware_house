import { Button, Layout, Select, Typography } from "antd";
import { useState } from "react";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import BatchTable from "./components/BatchTable";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";

const { Content, Sider } = Layout;
const { Text } = Typography;

const manufacturerOptions = [
  { value: "all", label: "All manufacturer" },
  { value: "DHG Pharma", label: "DHG Pharma" },
  { value: "Imexpharm", label: "Imexpharm" },
  { value: "Sanofi", label: "Sanofi" },
  { value: "Traphaco", label: "Traphaco" },
];

const conditionOptions = [
  { value: "all", label: "All condition" },
  { value: "Room temperature", label: "Room temperature" },
  { value: "Dry place", label: "Dry place" },
  { value: "Cold storage", label: "Cold storage" },
  { value: "Refrigerated", label: "Refrigerated" },
];

const statusOptions = [
  { value: "all", label: "All status" },
  { value: "In stock", label: "In stock" },
  { value: "Near Expiry", label: "Near expiry" },
  { value: "Expired", label: "Expired" },
];

type BatchFilters = {
  medicineName: string;
  manufacturer: string;
  storageCondition: string;
  status: string;
};

const BatchPage = () => {
  const [medicineName] = useState("");
  const [manufacturer, setManufacturer] = useState("all");
  const [storageCondition, setStorageCondition] = useState("all");
  const [status, setStatus] = useState("all");
  const [search, setSearch] = useState("");
  const [appliedFilters, setAppliedFilters] = useState<BatchFilters>({
    medicineName: "",
    manufacturer: "all",
    storageCondition: "all",
    status: "all",
  });

  const handleApplyFilters = () => {
    setAppliedFilters({
      medicineName: medicineName.trim(),
      manufacturer,
      storageCondition,
      status,
    });
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
          <TopBar title="Batches" subtitle="Warehouse" />
        </div>
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseFilterCard
            actions={
              <Button
                type="primary"
                className="h-[40px]"
                onClick={handleApplyFilters}
              >
                Apply
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Manufacturer</Text>
              <Select
                options={manufacturerOptions}
                value={manufacturer}
                onChange={setManufacturer}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Storage Condition</Text>
              <Select
                options={conditionOptions}
                value={storageCondition}
                onChange={setStorageCondition}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select
                options={statusOptions}
                value={status}
                onChange={setStatus}
              />
            </div>
          </BaseFilterCard>
          <BatchTable
            filters={appliedFilters}
            search={search}
            onSearch={setSearch}
          />
        </Content>
      </Layout>
    </Layout>
  );
};

export default BatchPage;
