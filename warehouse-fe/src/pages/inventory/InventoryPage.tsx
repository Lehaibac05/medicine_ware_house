import { Button, DatePicker, Input, Select, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import InventoryTable from "./components/InventoryTable";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import MainLayout from "../../layouts/MainLayout";
import { getWarehouses } from "../../services/warehouses";
import type { Warehouse } from "../../services/types";

const { Text } = Typography;

const statusOptions = [
  { value: "all", label: "Tất cả trạng thái" },
  { value: "NORMAL", label: "Còn hàng" },
  { value: "LOW_STOCK", label: "Sắp hết hàng" },
  { value: "EXPIRING_SOON", label: "Sắp hết hạn" },
];

const InventoryPage = () => {
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [tempWarehouse, setTempWarehouse] = useState<string>("all");
  const [tempMedicine, setTempMedicine] = useState<string>("");
  const [tempStatus, setTempStatus] = useState<string>("all");

  const [appliedWarehouse, setAppliedWarehouse] = useState<number | undefined>(
    undefined,
  );
  const [appliedMedicine, setAppliedMedicine] = useState<string | undefined>(
    undefined,
  );
  const [appliedStatus, setAppliedStatus] = useState<
    "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON" | undefined
  >(undefined);

  useEffect(() => {
    const loadWarehouses = async () => {
      try {
        const data = await getWarehouses();
        setWarehouses(data);
      } catch {
        setWarehouses([]);
      }
    };

    void loadWarehouses();
  }, []);

  const warehouseOptions = useMemo(
    () => [
      { value: "all", label: "Tất cả kho" },
      ...warehouses.map((warehouse) => ({
        value: String(warehouse.warehouseId),
        label: warehouse.name || `Warehouse ${warehouse.warehouseId}`,
      })),
    ],
    [warehouses],
  );

  const applyFilters = () => {
    setAppliedWarehouse(
      tempWarehouse === "all" ? undefined : Number(tempWarehouse),
    );
    setAppliedMedicine(tempMedicine.trim() ? tempMedicine.trim() : undefined);
    setAppliedStatus(
      tempStatus === "all"
        ? undefined
        : (tempStatus as "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON"),
    );
  };

  return (
    <MainLayout>
      <BaseFilterCard
        actions={
          <Button type="primary" className="h-[40px]" onClick={applyFilters}>
            Áp dụng
          </Button>
        }
      >
        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Kho</Text>
          <Select
            options={warehouseOptions}
            value={tempWarehouse}
            onChange={setTempWarehouse}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Tên thuốc</Text>
          <Input
            placeholder="Nhập tên thuốc..."
            value={tempMedicine}
            onChange={(e) => setTempMedicine(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Trạng thái</Text>
          <Select
            options={statusOptions}
            value={tempStatus}
            onChange={setTempStatus}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Text className="text-xs text-slate-500">Hạn sử dụng</Text>
          <DatePicker.RangePicker format="DD/MM/YYYY" className="w-full" />
        </div>
      </BaseFilterCard>

      <InventoryTable
        warehouseId={appliedWarehouse}
        medicineName={appliedMedicine}
        status={appliedStatus}
      />
    </MainLayout>
  );
};

export default InventoryPage;
