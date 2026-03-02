import { Button, Space, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";

const { Text } = Typography;

type InventoryRow = {
  key: string;
  name: string;
  batch: string;
  expiry: string;
  qty: string;
  warehouse: string;
  status: "In stock" | "Low" | "Overstock";
};

const data: InventoryRow[] = [
  {
    key: "PCM-0423",
    name: "Paracetamol 500mg",
    batch: "PCM-0423",
    expiry: "12/05/2026",
    qty: "4,200",
    warehouse: "Main Warehouse",
    status: "In stock",
  },
  {
    key: "AMX-2198",
    name: "Amoxicillin 250mg",
    batch: "AMX-2198",
    expiry: "25/02/2026",
    qty: "820",
    warehouse: "Cold Storage 1",
    status: "Low",
  },
  {
    key: "INS-9041",
    name: "Insulin Glargine",
    batch: "INS-9041",
    expiry: "02/08/2026",
    qty: "1,120",
    warehouse: "Cold Storage 2",
    status: "In stock",
  },
  {
    key: "VIT-1180",
    name: "Vitamin C 500mg",
    batch: "VIT-1180",
    expiry: "18/11/2026",
    qty: "6,480",
    warehouse: "Secondary Warehouse",
    status: "Overstock",
  },
];

const columns: ColumnsType<InventoryRow> = [
  {
    title: "Medicine name",
    dataIndex: "name",
    key: "name",
    render: (value: string) => <Text strong>{value}</Text>,
  },
  { title: "Batch number", dataIndex: "batch", key: "batch" },
  { title: "Expiry date", dataIndex: "expiry", key: "expiry" },
  { title: "Quantity", dataIndex: "qty", key: "qty" },
  { title: "Warehouse", dataIndex: "warehouse", key: "warehouse" },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: InventoryRow["status"]) => {
      if (value === "Low") return <Tag color="gold">Low</Tag>;
      if (value === "Overstock") return <Tag color="green">Overstock</Tag>;
      return <Tag>In stock</Tag>;
    },
  },
  {
    title: "Action",
    key: "action",
    render: () => (
      <Space>
        <Button size="small">View</Button>
        <Button size="small" type="primary">
          Adjust stock
        </Button>
      </Space>
    ),
  },
];

function InventoryTable() {
  return <BaseTable columns={columns} dataSource={data} />;
}

export default InventoryTable;
