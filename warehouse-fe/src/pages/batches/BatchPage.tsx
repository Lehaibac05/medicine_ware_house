import { Button, DatePicker, Select, Typography } from "antd";
import type { Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import MainLayout from "../../layouts/MainLayout";
import BatchTable from "./components/BatchTable";

const { Text } = Typography;
const { RangePicker } = DatePicker;

const statusOptions = [
  { value: "all", label: "Tất cả trạng thái" },
  { value: "AVAILABLE", label: "Còn hàng" },
  { value: "EXPIRED", label: "Hết hạn" },
];

type BatchFilters = {
  warehouseId: number | "all";
  dateRange: [Dayjs | null, Dayjs | null] | null;
  status: string;
};

type BatchFilterOptions = {
  warehouses: Array<{
    value: number;
    label: string;
  }>;
};

const BatchPage = () => {
  const [warehouseId, setWarehouseId] = useState<number | "all">("all");
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(
    null
  );
  const [status, setStatus] = useState("all");
  const [search, setSearch] = useState("");
  const [filterOptions, setFilterOptions] = useState<BatchFilterOptions>({
    warehouses: [],
  });
  const [appliedFilters, setAppliedFilters] = useState<BatchFilters>({
    warehouseId: "all",
    dateRange: null,
    status: "all",
  });

  const warehouseOptions = useMemo(
    () => [
      { value: "all", label: "Tất cả kho" },
      ...filterOptions.warehouses,
    ],
    [filterOptions.warehouses]
  );

  const handleApplyFilters = () => {
    setAppliedFilters({
      warehouseId,
      dateRange,
      status,
    });
  };

  const handleReset = () => {
    setWarehouseId("all");
    setDateRange(null);
    setStatus("all");
    setSearch("");
    setAppliedFilters({
      warehouseId: "all",
      dateRange: null,
      status: "all",
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
            >
              Áp dụng
            </Button>
          </div>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Kho</Text>
          <Select
            options={warehouseOptions}
            value={warehouseId}
            onChange={setWarehouseId}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Khoảng ngày NSX - HSD</Text>
          <RangePicker
            className="w-full"
            format="DD/MM/YYYY"
            value={dateRange}
            onChange={(value) =>
              setDateRange(value ? [value[0], value[1]] : null)
            }
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select options={statusOptions} value={status} onChange={setStatus} />
        </div>
      </BaseFilterCard>

      <BatchTable
        filters={appliedFilters}
        search={search}
        onSearch={setSearch}
        onFilterOptionsChange={setFilterOptions}
      />
    </MainLayout>
  );
};

export default BatchPage;
