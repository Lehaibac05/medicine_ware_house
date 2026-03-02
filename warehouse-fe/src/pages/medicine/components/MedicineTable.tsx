import { Button, Space, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";

const { Text } = Typography;

type MedicineRow = {
  key: string;
  name: string;
  manufacturer: string;
  storage: string;
  total: string;
  status: "In stock" | "Low" | "Overstock";
};

const data: MedicineRow[] = [
  {
    key: "PCM-0423",
    name: "Paracetamol 500mg",
    manufacturer: "DHG Pharma",
    storage: "Room temperature",
    total: "4,200",
    status: "In stock",
  },
  {
    key: "AMX-2198",
    name: "Amoxicillin 250mg",
    manufacturer: "Imexpharm",
    storage: "Dry place",
    total: "820",
    status: "Low",
  },
  {
    key: "INS-9041",
    name: "Insulin Glargine",
    manufacturer: "Sanofi",
    storage: "Cold storage",
    total: "1,120",
    status: "In stock",
  },
  {
    key: "VIT-1180",
    name: "Vitamin C 500mg",
    manufacturer: "Traphaco",
    storage: "Room temperature",
    total: "6,480",
    status: "Overstock",
  },
];

const columns: ColumnsType<MedicineRow> = [
  {
    title: "Medicine name",
    dataIndex: "name",
    key: "name",
    render: (value: string) => <Text strong>{value}</Text>,
  },
  {
    title: "Manufacturer",
    dataIndex: "manufacturer",
    key: "manufacturer",
  },
  {
    title: "Storage Condition",
    dataIndex: "storage",
    key: "storage",
  },
  {
    title: "Total Stock",
    dataIndex: "total",
    key: "total",
  },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: MedicineRow["status"]) => {
      if (value === "Low") return <Tag color="gold">Low</Tag>;
      if (value === "Overstock") return <Tag color="green">Overstock</Tag>;
      return <Tag color="blue">In stock</Tag>;
    },
  },
  {
    title: "Action",
    key: "action",
    render: () => (
      <Space>
        <Button size="small">View</Button>
        <Button size="small" type="primary">
          Edit
        </Button>
      </Space>
    ),
  },
];

function MedicineTable() {
  return <BaseTable columns={columns} dataSource={data} />;
}

export default MedicineTable;
