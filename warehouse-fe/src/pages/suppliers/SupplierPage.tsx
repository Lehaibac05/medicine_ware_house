import { Button, Select, Typography, Input } from "antd";
import { useEffect, useState } from "react";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import MainLayout from "../../layouts/MainLayout";
import SupplierTable from "./components/SupplierTable";
import { getActiveSuppliers } from "../../services/suppliers";

const { Text } = Typography;

export type SupplierFilters = {
  status: string;
  supplierName?: string;
};

const statusOptions = [
  { value: "all", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Hoạt động" },
  { value: "INACTIVE", label: "Ngừng hoạt động" },
  { value: "SUSPENDED", label: "Tạm ngưng" },
];

const SupplierPage = () => {
  const [status, setStatus] = useState("all");
  const [search, setSearch] = useState("");
  const [appliedFilters, setAppliedFilters] = useState<SupplierFilters>({
    status: "all",
  });
  const [supplierOptions, setSupplierOptions] = useState<
    { value: string; label: string }[]
  >([]);
  const [supplierName, setSupplierName] = useState<string | undefined>(undefined);

  const handleApplyFilters = () => {
    setAppliedFilters({
      status,
      supplierName: supplierName || undefined, 
    });
  };

  useEffect(() => {
    const fetchSuppliers = async () => {
      try {
        const res = await getActiveSuppliers();

        setSupplierOptions(
          res.map((s) => ({
            value: s.supplierName,
            label: s.supplierName,
          }))
        );
      } catch {
        console.log("Load supplier failed");
      }
    };

    fetchSuppliers();
  }, []);

  const handleReset = () => {
    setStatus("all");
    setSearch("");
    setSupplierName(undefined);
    setAppliedFilters({
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
        {/* Status */}
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select
            options={statusOptions}
            value={status}
            onChange={setStatus}
          />
        </div>

        {/* Supplier Name */}
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Nhà cung cấp</Text>
          <Select
            placeholder="Chọn nhà cung cấp"
            options={supplierOptions}
            value={supplierName}
            onChange={setSupplierName}
            allowClear
            showSearch
            optionFilterProp="label"
          />
        </div>

        {/* Search */}
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Tìm kiếm</Text>
          <Input.Search
            placeholder="Tên hoặc email..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </BaseFilterCard>

      <SupplierTable
        filters={appliedFilters}
        search={search}
        onSearch={setSearch}
      />
    </MainLayout>
  );
};

export default SupplierPage;
