import { Button, Descriptions, Modal, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { AxiosError } from "axios";
import React from "react";
import { useNavigate } from "react-router-dom";
import StatusTag from "../../components/common/StatusTag";
import {
  useApproveGoodsReceiptMutation,
  useGoodsReceiptDetailQuery,
  useGoodsReceiptsQuery,
} from "../../hooks/useWorkflow";
import { clearAuthToken } from "../../utils/auth";
import MainLayout from "../../layouts/MainLayout";
import BaseTable from "../../components/base/BaseTable";

const { Text } = Typography;

type ReceiptRow = {
  key: string;
  receiptId: number;
  receiptCode: string;
  createdAt?: string;
  purchaseOrder: string;
  receivedBy: string;
  status: string;
};

const normalizeReceiptStatus = (status?: string) =>
  status
    ?.trim()
    .toUpperCase()
    .replace(/[\s-]+/g, "_") ?? "";

const formatDateTime = (value?: string) =>
  value ? new Date(value).toLocaleString("vi-VN") : "-";

export default function GoodsReceiptsPage() {
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const [selectedReceiptId, setSelectedReceiptId] = React.useState<number>();
  const [isDetailOpen, setIsDetailOpen] = React.useState(false);
  const [pagination, setPagination] = React.useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const { data = { content: [], totalElements: 0 }, isLoading } = useGoodsReceiptsQuery({
    page: pagination.current - 1,
    size: pagination.pageSize,
  });
  const reviewMutation = useApproveGoodsReceiptMutation();
  const { data: receiptDetail, isLoading: isDetailLoading } = useGoodsReceiptDetailQuery(
    isDetailOpen ? selectedReceiptId : undefined,
  );

  React.useEffect(() => {
    if (data && 'totalElements' in data) {
      setPagination((prev) => ({
        ...prev,
        total: data.totalElements,
      }))
    }
  }, [data]);

  const receipts = Array.isArray(data) ? data : (data?.content || []);
  const rows: ReceiptRow[] = receipts.map((receipt: any) => ({
    key: String(receipt.receiptId),
    receiptId: receipt.receiptId,
    receiptCode: receipt.receiptCode,
    createdAt: receipt.createdAt,
    purchaseOrder: receipt.purchaseOrder?.orderCode || "-",
    receivedBy:
      receipt.receivedBy?.fullName || receipt.receivedBy?.username || "-",
    status: normalizeReceiptStatus(receipt.status),
  }));

  const onReview = async (row: ReceiptRow, approved: boolean) => {
    try {
      await reviewMutation.mutateAsync({ id: row.receiptId, approved });
      messageApi.success(
        approved
          ? "Duyệt phiếu nhập thành công"
          : "Từ chối phiếu nhập thành công",
      );
    } catch (error) {
      if (error instanceof AxiosError) {
        if (error.response?.status === 401) {
          clearAuthToken();
          messageApi.error(
            "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại",
          );
          navigate("/login", { replace: true });
          return;
        }

        if (error.response?.status === 403) {
          messageApi.error(
            approved
              ? "Bạn không có quyền duyệt phiếu nhập này"
              : "Bạn không có quyền từ chối phiếu nhập này",
          );
          return;
        }

        const responseData = error.response?.data;
        const serverMessage =
          typeof responseData === "string"
            ? responseData
            : responseData?.message;
        const statusText = error.response?.status
          ? `HTTP ${error.response.status}`
          : "";
        messageApi.error(
          serverMessage ||
          statusText ||
          (approved
            ? "Duyệt phiếu nhập thất bại"
            : "Từ chối phiếu nhập thất bại"),
        );
        return;
      }

      messageApi.error(
        approved ? "Duyệt phiếu nhập thất bại" : "Từ chối phiếu nhập thất bại",
      );
    }
  };

  const columns: ColumnsType<ReceiptRow> = [
    { title: "Mã phiếu nhập", dataIndex: "receiptCode", width: 160 },
    {
      title: "Thời gian tạo",
      dataIndex: "createdAt",
      width: 190,
      render: (value?: string) => formatDateTime(value),
    },
    { title: "Đơn mua hàng", dataIndex: "purchaseOrder", width: 160 },
    { title: "Người nhận", dataIndex: "receivedBy" },
    {
      title: "Trạng thái",
      dataIndex: "status",
      width: 200,
      render: (value: string) => (
        <StatusTag
          domain="goodsReceipt"
          status={normalizeReceiptStatus(value)}
        />
      ),
    },
    {
      title: "Hành động",
      width: 320,
      render: (_: unknown, record: ReceiptRow) => {
        const isPending = record.status === "PENDING_APPROVAL";
        const isCurrentRowPending =
          reviewMutation.isPending &&
          reviewMutation.variables?.id === record.receiptId;

        return (
          <Space>
            <Button
              size="small"
              onClick={() => {
                setSelectedReceiptId(record.receiptId);
                setIsDetailOpen(true);
              }}
            >
              Xem
            </Button>
            <Button
              size="small"
              onClick={() => void onReview(record, true)}
              disabled={!isPending}
              loading={
                isCurrentRowPending &&
                reviewMutation.variables?.approved !== false
              }
            >
              Duyệt phiếu nhập
            </Button>
            <Button
              danger
              size="small"
              onClick={() => void onReview(record, false)}
              disabled={!isPending}
              loading={
                isCurrentRowPending &&
                reviewMutation.variables?.approved === false
              }
            >
              Từ chối
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <MainLayout>
      {contextHolder}
      <BaseTable
        rowKey="key"
        loading={isLoading}
        columns={columns}
        dataSource={rows}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: (page, pageSize) => {
            setPagination((prev) => ({
              ...prev,
              current: page,
              pageSize,
            }))
          },
        }}
        cardClassName="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
        title={() => (
          <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">
            Danh sách phiếu nhập chờ duyệt
          </Text>
        )}
      />

      <Modal
        title="Chi tiết phiếu nhập"
        open={isDetailOpen}
        onCancel={() => setIsDetailOpen(false)}
        footer={null}
        width={980}
      >
        <Descriptions
          bordered
          size="small"
          column={2}
          className="mb-4"
        >
          <Descriptions.Item label="Mã phiếu nhập">
            {receiptDetail?.receiptCode || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            {receiptDetail?.status ? (
              <StatusTag
                domain="goodsReceipt"
                status={normalizeReceiptStatus(receiptDetail.status)}
              />
            ) : (
              "-"
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Đơn mua hàng">
            {receiptDetail?.purchaseOrder?.orderCode || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="Tổng tiền đơn mua hàng">
            {receiptDetail?.purchaseOrder?.totalAmount?.toLocaleString("vi-VN") || "0"}
          </Descriptions.Item>
          <Descriptions.Item label="Người nhận">
            {receiptDetail?.receivedBy?.fullName || receiptDetail?.receivedBy?.username || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="Người duyệt">
            {receiptDetail?.approvedBy?.fullName || receiptDetail?.approvedBy?.username || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="Thời điểm nhận">
            {formatDateTime(receiptDetail?.receivedAt)}
          </Descriptions.Item>
          <Descriptions.Item label="Thời điểm duyệt">
            {formatDateTime(receiptDetail?.approvedAt)}
          </Descriptions.Item>
          <Descriptions.Item label="Ghi chú kiểm tra" span={2}>
            {receiptDetail?.qualityCheckNotes || "-"}
          </Descriptions.Item>
          <Descriptions.Item label="Kiểm tra chất lượng" span={2}>
            {receiptDetail?.qualityPassed === undefined ? (
              "-"
            ) : receiptDetail.qualityPassed ? (
              <Tag color="green">Đạt</Tag>
            ) : (
              <Tag color="red">Không đạt</Tag>
            )}
          </Descriptions.Item>
        </Descriptions>

        <Table
          size="small"
          loading={isDetailLoading}
          dataSource={receiptDetail?.purchaseOrder?.items || []}
          rowKey={(item) => String(item.itemId)}
          pagination={false}
          columns={[
            {
              title: "Thuốc",
              dataIndex: ["medicine", "medicineName"],
              render: (_: unknown, row: any) =>
                row?.medicine?.medicineName || `#${row?.medicine?.medicineId || "-"}`,
            },
            {
              title: "SL đặt",
              dataIndex: "requestedQuantity",
              width: 90,
            },
            {
              title: "SL nhận",
              dataIndex: "receivedQuantity",
              width: 90,
              render: (value?: number) => value ?? "-",
            },
            {
              title: "Đơn giá",
              dataIndex: "unitPrice",
              width: 120,
              render: (value?: number) => (value ?? 0).toLocaleString("vi-VN"),
            },
            {
              title: "Hạn dùng",
              dataIndex: "expectedExpiryDate",
              width: 130,
              render: (value?: string) => value || "-",
            },
            {
              title: "Ghi chú",
              dataIndex: "notes",
              render: (value?: string) => value || "-",
            },
          ]}
        />
      </Modal>
    </MainLayout>
  );
}
