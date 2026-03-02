import { Button, Space, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";

type BatchRow = {
  key: string;
  batch: string;
  name: string;
  mfg: string;
  expiry: string;
  quantity: string;
  warehouse: string;
  status: "In stock" | "Near Expiry" | "Expiried";
};

const data: BatchRow[] = [
  {
    key: "PCM-0423",
    batch: "PCM-0423",
    name: "Paracetamol 500mg",
    mfg: "12/05/2024",
    expiry: "12/05/2026",
    quantity: "4,200",
    warehouse: "Main Warehouse",
    status: "In stock",
  },
  {
    key: "AMX-2198",
    batch: "AMX-2198",
    name: "Amoxicillin 250mg",
    mfg: "25/02/2024",
    expiry: "25/02/2026",
    quantity: "820",
    warehouse: "Cold Storage 1",
    status: "Near Expiry",
  },
  {
    key: "INS-9041",
    batch: "INS-9041",
    name: "Insulin Glargine",
    mfg: "02/08/2024",
    expiry: "02/08/2026",
    quantity: "1,120",
    warehouse: "Cold Storage 2",
    status: "In stock",
  },
  {
    key: "VIT-1180",
    batch: "VIT-1180",
    name: "Vitamin C 500mg",
    mfg: "18/11/2024",
    expiry: "18/11/2026",
    quantity: "6,480",
    warehouse: "Secondary Warehouse",
    status: "Expiried",
  },
];

const columns: ColumnsType<BatchRow> = [
  {
    title: "Batch number",
    dataIndex: "batch",
    key: "batch",
  },
  {
    title: "Medicine name",
    dataIndex: "name",
    key: "name",
  },
  {
    title: "Mfg date",
    dataIndex: "mfg",
    key: "mfg",
  },
  {
    title: "Expiry date",
    dataIndex: "expiry",
    key: "expiry",
  },
  {
    title: "Quantity",
    dataIndex: "quantity",
    key: "quantity",
  },
  {
    title: "Warehouse",
    dataIndex: "warehouse",
    key: "warehouse",
  },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: BatchRow["status"]) => {
      if (value === "Near Expiry") return <Tag color="gold">Near Expiry</Tag>;
      if (value === "In stock") return <Tag color="green">In Stock</Tag>;
      return <Tag color="red">Expried</Tag>;
    },
  },
  {
    title: "Action",
    key: "action",
    render: () => (
      <Space>
        <Button size="small">View</Button>
        <Button size="small" type="primary">
          Adjust Stock
        </Button>
      </Space>
    ),
  },
];

function BatchTable() {
  return <BaseTable columns={columns} dataSource={data} />;
}

export default BatchTable;
