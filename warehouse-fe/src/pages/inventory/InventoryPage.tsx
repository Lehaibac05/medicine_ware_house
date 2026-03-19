import {
  Button,
  DatePicker,
  Input,
  Layout,
  Select,
  Space,
  Typography,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import InventoryTable from "./components/InventoryTable";
import SidebarNav from "../../layouts/SidebarNav";
import TopBar from "../../layouts/TopBar";
import BaseFilterCard from "../../components/base/BaseFilterCard";
import { getWarehouses } from "../../services/warehouses";
import type { Warehouse } from "../../services/types";

const { Content, Sider } = Layout;
const { Text } = Typography;

const statusOptions = [
  { value: "all", label: "All status" },
  { value: "NORMAL", label: "In stock" },
  { value: "LOW_STOCK", label: "Low" },
  { value: "EXPIRING_SOON", label: "Expiring soon" },
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
      { value: "all", label: "All warehouses" },
      ...warehouses.map((warehouse) => ({
        value: String(warehouse.warehouseId),
        label: warehouse.name || `Warehouse ${warehouse.warehouseId}`,
      })),
    ],
    [warehouses],
  );

  const applyFilters = () => {
    setAppliedWarehouse(tempWarehouse === "all" ? undefined : Number(tempWarehouse));
    setAppliedMedicine(tempMedicine.trim() ? tempMedicine.trim() : undefined);
    setAppliedStatus(
      tempStatus === "all"
        ? undefined
        : (tempStatus as "NORMAL" | "LOW_STOCK" | "EXPIRING_SOON"),
    );
  };

  return (
    <Layout className="h-screen bg-slate-100">
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
          <Space className="w-full justify-end rounded-2xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Link to="/requests/new">
              <Button type="primary">Create Medicine Request</Button>
            </Link>
          </Space>

          <BaseFilterCard
            actions={
              <Button
                type="primary"
                className="h-[40px]"
                onClick={applyFilters}
              >
                Apply
              </Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Warehouse</Text>
              <Select
                options={warehouseOptions}
                value={tempWarehouse}
                onChange={setTempWarehouse}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine name</Text>
              <Input
                placeholder="Enter medicine name"
                value={tempMedicine}
                onChange={(e) => setTempMedicine(e.target.value)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select
                options={statusOptions}
                value={tempStatus}
                onChange={setTempStatus}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Expiry date</Text>
              <DatePicker.RangePicker format="DD/MM/YYYY" className="w-full" />
            </div>
          </BaseFilterCard>

          <InventoryTable
            warehouseId={appliedWarehouse}
            medicineName={appliedMedicine}
            status={appliedStatus}
          />
        </Content>
      </Layout>
    </Layout>
  );
};

export default InventoryPage;
