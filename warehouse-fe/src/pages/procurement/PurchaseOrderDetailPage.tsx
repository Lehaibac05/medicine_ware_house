import {
  Button,
  Card,
  Descriptions,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../services/api";
import {
  confirmPurchaseOrder,
  downloadPurchaseOrderPdf,
  getPurchaseOrderById,
  sendPurchaseOrderEmail,
  updatePurchaseOrderStatus,
  type PurchaseOrder,
} from "../../services/purchaseOrders";
import MainLayout from "../../layouts/MainLayout";

const { Title, Text } = Typography;

type OrderAction = "confirm" | "shipping" | null;

const normalizeOrderStatus = (status?: string) => {
  const normalized = status?.trim().toUpperCase();
  if (normalized === "PENDING") return "PENDING";
  if (normalized === "CONFIRMED") return "CONFIRMED";
  if (normalized === "SHIPPING") return "SHIPPING";
  if (normalized === "RECEIVED") return "RECEIVED";
  if (normalized === "APPROVED") return "APPROVED";
  return "PENDING";
};

const statusTag = (status: string) => {
  const normalized = status.toUpperCase();

  if (normalized === "APPROVED" || normalized === "RECEIVED")
    return <Tag color="green">Đã hoàn thành</Tag>;

  if (normalized === "SHIPPING") return <Tag color="blue">Đang giao</Tag>;

  if (normalized === "CONFIRMED")
    return <Tag color="geekblue">Đã xác nhận</Tag>;

  if (normalized === "PENDING") return <Tag color="gold">Chờ xử lý</Tag>;

  return <Tag>{normalized}</Tag>;
};

export default function PurchaseOrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [exportingPdf, setExportingPdf] = useState(false);
  const [sendingEmail, setSendingEmail] = useState(false);
  const [actionLoading, setActionLoading] = useState<OrderAction>(null);
  const [order, setOrder] = useState<PurchaseOrder | null>(null);

  const orderId = useMemo(() => Number(id), [id]);

  const load = async () => {
    if (!orderId || Number.isNaN(orderId)) {
      messageApi.error("Mã đơn mua không hợp lệ");
      navigate("/purchase-orders");
      return;
    }

    try {
      setLoading(true);
      const data = await getPurchaseOrderById(orderId);
      setOrder(data);
    } catch {
      messageApi.error("Không thể tải đơn mua");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [id]);

  const getActionError = (error: unknown, fallback: string) => {
    if (error instanceof ApiError && error.status === 403) {
      return "Bạn không có quyền thực hiện hành động này";
    }
    return fallback;
  };

  const onConfirm = async () => {
    if (!order) return;
    try {
      setActionLoading("confirm");
      await confirmPurchaseOrder(order.purchaseOrderId);
      messageApi.success("Đã xác nhận đơn mua");
      await load();
    } catch (error) {
      messageApi.error(getActionError(error, "Không thể xác nhận đơn mua"));
    } finally {
      setActionLoading(null);
    }
  };

  const onStatusChange = async (status: "SHIPPING") => {
    if (!order) return;
    const actionMap: Record<
      "SHIPPING",
      Exclude<OrderAction, null | "confirm">
    > = {
      SHIPPING: "shipping",
    };

    const statusTextMap: Record<string, string> = {
      PENDING: "Chờ xử lý",
      CONFIRMED: "Đã xác nhận",
      SHIPPING: "Đang giao",
      RECEIVED: "Đã nhận",
      APPROVED: "Đã duyệt",
    };

    try {
      setActionLoading(actionMap[status]);
      await updatePurchaseOrderStatus(order.purchaseOrderId, status);

      const text = statusTextMap[status] || status;
      messageApi.success(`Đã cập nhật trạng thái: ${text}`);

      await load();
    } catch (error) {
      messageApi.error(getActionError(error, "Không thể cập nhật trạng thái"));
    } finally {
      setActionLoading(null);
    }
  };

  const currentStatus = normalizeOrderStatus(order?.status);

  const onExportPdf = async () => {
    if (!order) return;
    try {
      setExportingPdf(true);
      await downloadPurchaseOrderPdf(order.purchaseOrderId, order.orderCode);
      messageApi.success("Xuất PDF thành công");
    } catch {
      messageApi.error("Xuất PDF thất bại");
    } finally {
      setExportingPdf(false);
    }
  };

  const onSendEmail = async () => {
    if (!order) return;
    try {
      setSendingEmail(true);
      const result = await sendPurchaseOrderEmail(order.purchaseOrderId);
      messageApi.success(result.message || "Purchase order email sent");
    } catch {
      messageApi.error("Failed to send purchase order email");
    } finally {
      setSendingEmail(false);
    }
  };

  return (
    <MainLayout>
      {contextHolder}
      <Space>
        <Link to="/purchase-orders">
          <Button>Quay lại</Button>
        </Link>
        <Button
          type="primary"
          onClick={() => void onConfirm()}
          loading={actionLoading === "confirm"}
          disabled={!order || currentStatus !== "PENDING"}
        >
          Xác nhận
        </Button>
        <Link
          to={order ? `/purchase-orders/${order.purchaseOrderId}/edit` : "#"}
        >
          <Button disabled={!order || currentStatus !== "PENDING"}>Sửa</Button>
        </Link>
        <Button
          onClick={() => void onStatusChange("SHIPPING")}
          loading={actionLoading === "shipping"}
          disabled={!order || currentStatus !== "CONFIRMED"}
        >
          Đánh dấu đã giao hàng
        </Button>
        <Link to={order ? `/goods-receipts/new/${order.purchaseOrderId}` : "#"}>
          <Button disabled={!order || currentStatus !== "SHIPPING"}>
            Tạo phiếu nhập hàng
          </Button>
        </Link>
        <Button
          onClick={onExportPdf}
          loading={exportingPdf}
          disabled={!order || currentStatus !== "CONFIRMED"}
        >
          Xuất file PDF
        </Button>
        <Button
          onClick={onSendEmail}
          loading={sendingEmail}
          disabled={!order || currentStatus !== "CONFIRMED"}
        >
         Gửi Email
        </Button>
      </Space>

      <Card
        loading={loading}
        className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]"
      >
        {order ? (
          <Space direction="vertical" size={20} className="w-full">
            <Space className="w-full justify-between">
              <Title level={4} className="!mb-0">
                {order.orderCode}
              </Title>
              {statusTag(order.status)}
            </Space>

            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="Nhà cung cấp">
                {order.supplier?.supplierName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="Kho">
                {order.warehouse?.warehouseName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="Ngày tạo">
                {order.createdAt
                  ? new Date(order.createdAt).toLocaleString()
                  : "-"}
              </Descriptions.Item>
              <Descriptions.Item label="Tổng tiền">
                {Number(order.totalAmount || 0).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>

            <Card size="small" title="Sản phẩm">
              <div className="grid gap-3">
                {order.items?.map((item) => (
                  <div
                    key={
                      item.itemId ||
                      `${item.medicine?.medicineId}-${item.requestedQuantity}`
                    }
                    className="rounded-xl border border-slate-200 px-3 py-2"
                  >
                    <Text strong>
                      {item.medicine?.medicineName ||
                        `Medicine #${item.medicine?.medicineId}`}
                    </Text>
                    <br />
                    <Text type="secondary">
                      Qty:{" "}
                      {Number(item.requestedQuantity || 0).toLocaleString()} |
                      Unit: {Number(item.unitPrice || 0).toLocaleString()}
                    </Text>
                  </div>
                ))}
              </div>
            </Card>
          </Space>
        ) : (
          <Text type="secondary">Không có dữ liệu</Text>
        )}
      </Card>
    </MainLayout>
  );
}
