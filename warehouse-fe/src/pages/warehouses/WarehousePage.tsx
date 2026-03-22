import { Button, Input, Typography } from "antd";
import { useMemo, useState } from "react";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import MainLayout from "../../layouts/MainLayout";
import WarehouseTable from "./components/WarehouseTable";

const { Text } = Typography;

export type WarehouseFilters = {
  location: string;
};

const WarehousePage = () => {
  const [location, setLocation] = useState("");
  const [search, setSearch] = useState("");
  const [appliedFilters, setAppliedFilters] = useState<WarehouseFilters>({
    location: "",
  });

  const hasFilters = useMemo(
    () => Boolean(location.trim()) || Boolean(search.trim()),
    [location, search],
  );

  const handleApplyFilters = () => {
    setAppliedFilters({
      location: location.trim(),
    });
  };

  const handleReset = () => {
    setLocation("");
    setSearch("");
    setAppliedFilters({
      location: "",
    });
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
              disabled={!hasFilters && !appliedFilters.location}
            >
              Áp dụng
            </Button>
          </div>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Vị trí kho</Text>
          <Input
            placeholder="Nhập khu vực hoặc địa chỉ kho..."
            value={location}
            onChange={(event) => setLocation(event.target.value)}
          />
        </div>
      </BaseFilterCard>

      <WarehouseTable
        filters={appliedFilters}
        search={search}
        onSearch={setSearch}
      />
    </MainLayout>
  );
};

export default WarehousePage;
