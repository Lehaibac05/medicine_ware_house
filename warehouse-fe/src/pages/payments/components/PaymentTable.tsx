import { Button, Space, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";

type PaymentRow = {
  key: string;
  payment_id: string;
  order_id: string;
  payment_date: string;
  amount: string;
  method: string;
  status: "Pending" | "Completed" | "Cancelled";
};

const data: PaymentRow[] = [
  {
    key: "PAY-0001",
    payment_id: "PAY-0001",
    order_id: "ORD-0001",
    payment_date: "12/01/2026",
    amount: "$12,400",
    method: "Bank Transfer",
    status: "Completed",
  },
  {
    key: "PAY-0002",
    payment_id: "PAY-0002",
    order_id: "ORD-0002",
    payment_date: "18/01/2026",
    amount: "$3,280",
    method: "Cash",
    status: "Pending",
  },
  {
    key: "PAY-0003",
    payment_id: "PAY-0003",
    order_id: "ORD-0003",
    payment_date: "21/01/2026",
    amount: "$8,950",
    method: "Credit Card",
    status: "Cancelled",
  },
  {
    key: "PAY-0004",
    payment_id: "PAY-0004",
    order_id: "ORD-0004",
    payment_date: "25/01/2026",
    amount: "$15,600",
    method: "Bank Transfer",
    status: "Completed",
  },
];

const columns: ColumnsType<PaymentRow> = [
  {
    title: "Payment ID",
    dataIndex: "payment_id",
    key: "payment_id",
  },
  {
    title: "Order ID",
    dataIndex: "order_id",
    key: "order_id",
  },
  {
    title: "Payment Date",
    dataIndex: "payment_date",
    key: "payment_date",
  },
  {
    title: "Amount",
    dataIndex: "amount",
    key: "amount",
  },
  {
    title: "Method",
    dataIndex: "method",
    key: "method",
  },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: PaymentRow["status"]) => {
      if (value === "Pending") return <Tag color="gold">Pending</Tag>;
      if (value === "Completed") return <Tag color="green">Completed</Tag>;
      return <Tag color="red">Cancelled</Tag>;
    },
  },
  {
    title: "Action",
    key: "action",
    render: () => (
      <Space>
        <Button size="small">View</Button>
        <Button size="small" type="primary">
          Refund
        </Button>
      </Space>
    ),
  },
];

function PaymentTable() {
  return <BaseTable columns={columns} dataSource={data} />;
}

export default PaymentTable;