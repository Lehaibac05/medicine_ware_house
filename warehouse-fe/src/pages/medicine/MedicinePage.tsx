import { Button, Layout, Select, Typography } from "antd";
import { useState } from "react";
import MedicineTable from "./components/MedicineTable";
import BaseFilterCard from "../../components/base/BaseFilterCard";
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

const sortOptions = [
  { value: "name_asc", label: "Name A -> Z" },
  { value: "name_desc", label: "Name Z -> A" },
  { value: "manufacturer_asc", label: "Manufacturer A -> Z" },
  { value: "manufacturer_desc", label: "Manufacturer Z -> A" },
];

type MedicineFilters = {
  manufacturer: string;
  storageCondition: string;
};

const MedicinePage = () => {
  const [manufacturer, setManufacturer] = useState("all");
  const [storageCondition, setStorageCondition] = useState("all");
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState("name_asc");
  const [appliedFilters, setAppliedFilters] = useState<MedicineFilters>({
    manufacturer: "all",
    storageCondition: "all",
  });

  const handleApplyFilters = () => {
    setAppliedFilters({
      manufacturer,
      storageCondition,
    });
  };

  const handleReset = () => {
    setManufacturer("all");
    setStorageCondition("all");
    setAppliedFilters({
      manufacturer: "all",
      storageCondition: "all",
    });
    setSearch("");
    setSort("name_asc");
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
          <TopBar title="Medicine" subtitle="Warehouse" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseFilterCard
            actions={
              <div className="flex gap-2">
                <Button className="h-[40px] flex-1" onClick={handleReset}>
                  Reset
                </Button>
                <Button
                  type="primary"
                  className="h-[40px] flex-1"
                  onClick={handleApplyFilters}
                >
                  Apply
                </Button>
              </div>
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
              <Text className="text-xs text-slate-500">Sort by</Text>
              <Select options={sortOptions} value={sort} onChange={setSort} />
            </div>
          </BaseFilterCard>

          <MedicineTable
            filters={appliedFilters}
            search={search}
            sort={sort}
            onSearch={setSearch}
          />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MedicinePage;
