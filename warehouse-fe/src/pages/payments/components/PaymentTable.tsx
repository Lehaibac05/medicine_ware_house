import { Button, Flex, Input, Space, Tooltip, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import BaseTable from "../../../components/base/BaseTable";
import StatusTag from "../../../components/common/StatusTag";
import type { SupplierInvoice } from "../../../services/workflow";

const { Text } = Typography;

type PaymentTableProps = {
  data: SupplierInvoice[];
  loading?: boolean;
  search: string;
  onSearchChange: (value: string) => void;
  onVerify: (invoice: SupplierInvoice) => void;
  onReject: (invoice: SupplierInvoice) => void;
  onPay: (invoice: SupplierInvoice) => void;
};

const formatMoney = (value?: number) =>
  Number(value || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function PaymentTable({
  data,
  loading,
  search,
  onSearchChange,
  onVerify,
  onReject,
  onPay,
}: PaymentTableProps) {
  const columns: ColumnsType<SupplierInvoice> = [
    {
      title: "Invoice",
      dataIndex: "invoiceCode",
      key: "invoiceCode",
      width: 160,
      render: (value: string) => <Text strong>{value}</Text>,
    },
    {
      title: "Supplier",
      key: "supplier",
      render: (_, record) => record.supplier?.supplierName || "-",
      ellipsis: true,
    },
    {
      title: "Goods Receipt",
      key: "goodsReceipt",
      render: (_, record) => record.goodsReceipt?.receiptCode || "-",
      width: 160,
    },
    {
      title: "Total",
      key: "totalAmount",
      width: 140,
      render: (_, record) => formatMoney(record.totalAmount),
    },
    {
      title: "Paid",
      key: "paidAmount",
      width: 140,
      render: (_, record) => formatMoney(record.paidAmount),
    },
    {
      title: "Remaining",
      key: "remainingAmount",
      width: 140,
      render: (_, record) => formatMoney(record.remainingAmount),
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      width: 170,
      render: (value: string) => <StatusTag domain="invoice" status={value} />,
    },
    {
      title: "Mismatch",
      key: "mismatch",
      width: 130,
      render: (_, record) => {
        if (record.hasMismatch) {
          return (
            <Tooltip title={record.mismatchWarning || "Mismatch with goods receipt"}>
              <Text className="text-red-600">Yes</Text>
            </Tooltip>
          );
        }
        return <Text className="text-emerald-700">No</Text>;
      },
    },
    {
      title: "Action",
      key: "action",
      width: 240,
      render: (_, record) => {
        const status = (record.status || "").toUpperCase();
        const canVerify = status === "PENDING_VERIFICATION";
        const canReject = status === "PENDING_VERIFICATION";
        const canPay = status === "VERIFIED" || status === "PARTIALLY_PAID";

        return (
          <Space>
            <Button
              size="small"
              onClick={() => onVerify(record)}
              disabled={!canVerify || !!record.hasMismatch}
            >
              Verify
            </Button>
            <Button
              size="small"
              danger
              onClick={() => onReject(record)}
              disabled={!canReject}
            >
              Reject
            </Button>
            <Button
              size="small"
              type="primary"
              onClick={() => onPay(record)}
              disabled={!canPay}
            >
              Pay
            </Button>
          </Space>
        );
      },
    },
  ];

  const tableHeader = (
    <Flex justify="space-between" align="center">
      <div className="flex flex-col">
        <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
          Supplier invoice list
        </Text>
      </div>
      <div className="w-[200px]">
        <Input.Search
          placeholder="Search by invoice code or supplier..."
          className="w-[320px]"
          allowClear
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          onSearch={onSearchChange}
        />
      </div>
    </Flex>
  );
  return (
    <BaseTable
      rowKey="invoiceId"
      title={() => tableHeader}
      columns={columns}
      dataSource={data}
      loading={loading}
      cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
    />
  );
}

export default PaymentTable;
