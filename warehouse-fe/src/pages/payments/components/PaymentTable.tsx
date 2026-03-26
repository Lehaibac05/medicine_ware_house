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
  Number(value || 0).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

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
      title: "Hóa đơn",
      dataIndex: "invoiceCode",
      key: "invoiceCode",
      width: 160,
      render: (value: string) => <Text strong>{value}</Text>,
    },
    {
      title: "Nhà cung cấp",
      key: "supplier",
      render: (_, record) => record.supplier?.supplierName || "-",
      ellipsis: true,
    },
    {
      title: "Phiếu nhập hàng",
      key: "goodsReceipt",
      render: (_, record) => record.goodsReceipt?.receiptCode || "-",
      width: 160,
    },
    {
      title: "Tổng tiền đã thanh toán",
      key: "paidAmount",
      width: 180,
      render: (_, record) => formatMoney(record.paidAmount),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 170,
      render: (value: string) => <StatusTag domain="invoice" status={value} />,
    },
    {
      title: "Sai lệch",
      key: "mismatch",
      width: 130,
      render: (_, record) => {
        if (record.hasMismatch) {
          return (
            <Tooltip
              title={record.mismatchWarning || "Sai lệch so với phiếu nhập"}
            >
              <Text className="text-red-600">Có</Text>
            </Tooltip>
          );
        }
        return <Text className="text-emerald-700">Không</Text>;
      },
    },
    {
      title: "Hành động",
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
              Xác minh
            </Button>
            <Button
              size="small"
              danger
              onClick={() => onReject(record)}
              disabled={!canReject}
            >
              Từ chối
            </Button>
            <Button
              size="small"
              type="primary"
              onClick={() => onPay(record)}
              disabled={!canPay}
            >
              Thanh toán
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
          Danh sách hóa đơn nhà cung cấp
        </Text>
      </div>
      <div className="w-[350px]">
        <Input.Search
          placeholder="Tìm kiếm mã hóa đơn hoặc nhà cung cấp..."
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
