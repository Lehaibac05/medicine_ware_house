import { Button, Select, Typography } from "antd";
import { useState } from "react";
import MedicineTable from "./components/MedicineTable";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import MainLayout from "../../layouts/MainLayout";

const { Text } = Typography;

const manufacturerOptions = [
  { value: "all", label: "Tất cả nhà sản xuất" },
  { value: "DHG Pharma", label: "DHG Pharma" },
  { value: "Imexpharm", label: "Imexpharm" },
  { value: "Sanofi", label: "Sanofi" },
  { value: "Traphaco", label: "Traphaco" },
];

const conditionOptions = [
  { value: "all", label: "Tất cả điều kiện bảo quản" },
  { value: "Room temperature", label: "Room temperature" },
  { value: "Dry place", label: "Dry place" },
  { value: "Cold storage", label: "Cold storage" },
  { value: "Refrigerated", label: "Refrigerated" },
];

const sortOptions = [
  { value: "name_asc", label: "Tên thuốc A -> Z" },
  { value: "name_desc", label: "Tên thuốc Z -> A" },
  { value: "manufacturer_asc", label: "Tên nhà sản xuất A -> Z" },
  { value: "manufacturer_desc", label: "Tên nhà sản xuất Z -> A" },
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
    <MainLayout>
      <BaseFilterCard
        actions={
          <div className="flex gap-2">
            <Button className="h-[40px] flex-1" onClick={handleReset}>
              Khôi phục
            </Button>
            <Button
              type="primary"
              className="h-[40px] flex-1"
              onClick={handleApplyFilters}
            >
              Áp dụng
            </Button>
          </div>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Nhà sản xuất</Text>
          <Select
            options={manufacturerOptions}
            value={manufacturer}
            onChange={setManufacturer}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">
            Điều kiện bảo quản
          </Text>
          <Select
            options={conditionOptions}
            value={storageCondition}
            onChange={setStorageCondition}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Sắp xếp theo</Text>
          <Select options={sortOptions} value={sort} onChange={setSort} />
        </div>
      </BaseFilterCard>

      <MedicineTable
        filters={appliedFilters}
        search={search}
        sort={sort}
        onSearch={setSearch}
      />
    </MainLayout>
  );
};

export default MedicinePage;