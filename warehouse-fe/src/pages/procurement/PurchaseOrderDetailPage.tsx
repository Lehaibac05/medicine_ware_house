import { Button, Card, Descriptions, Layout, Space, Tag, Typography, message } from "antd"
import { useEffect, useMemo, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { ApiError } from "../../services/api"
import {
  confirmPurchaseOrder,
  downloadPurchaseOrderPdf,
  getPurchaseOrderById,
  sendPurchaseOrderEmail,
  updatePurchaseOrderStatus,
  type PurchaseOrder,
} from "../../services/purchaseOrders"

const { Content, Sider } = Layout
const { Title, Text } = Typography

type OrderAction = "confirm" | "shipping" | null

const normalizeOrderStatus = (status?: string) => {
  const normalized = status?.trim().toUpperCase()
  if (normalized === "PENDING") return "PENDING"
  if (normalized === "CONFIRMED") return "CONFIRMED"
  if (normalized === "SHIPPING") return "SHIPPING"
  if (normalized === "RECEIVED") return "RECEIVED"
  if (normalized === "APPROVED") return "APPROVED"
  return "PENDING"
}

const statusTag = (status: string) => {
  const normalized = status.toUpperCase()
  if (normalized === "APPROVED" || normalized === "RECEIVED") return <Tag color="green">{normalized}</Tag>
  if (normalized === "SHIPPING") return <Tag color="blue">SHIPPED</Tag>
  if (normalized === "CONFIRMED") return <Tag color="geekblue">CONFIRMED</Tag>
  if (normalized === "PENDING") return <Tag color="gold">PENDING</Tag>
  return <Tag>{normalized}</Tag>
}

export default function PurchaseOrderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [exportingPdf, setExportingPdf] = useState(false)
  const [sendingEmail, setSendingEmail] = useState(false)
  const [actionLoading, setActionLoading] = useState<OrderAction>(null)
  const [order, setOrder] = useState<PurchaseOrder | null>(null)

  const orderId = useMemo(() => Number(id), [id])

  const load = async () => {
    if (!orderId || Number.isNaN(orderId)) {
      messageApi.error("Invalid purchase order id")
      navigate("/purchase-orders")
      return
    }

    try {
      setLoading(true)
      const data = await getPurchaseOrderById(orderId)
      setOrder(data)
    } catch {
      messageApi.error("Failed to load purchase order")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [id])

  const getActionError = (error: unknown, fallback: string) => {
    if (error instanceof ApiError && error.status === 403) {
      return "You do not have permission to perform this action"
    }
    return fallback
  }

  const onConfirm = async () => {
    if (!order) return
    try {
      setActionLoading("confirm")
      await confirmPurchaseOrder(order.purchaseOrderId)
      messageApi.success("Purchase order confirmed")
      await load()
    } catch (error) {
      messageApi.error(getActionError(error, "Failed to confirm purchase order"))
    } finally {
      setActionLoading(null)
    }
  }

  const onStatusChange = async (status: "SHIPPING") => {
    if (!order) return
    const actionMap: Record<"SHIPPING", Exclude<OrderAction, null | "confirm">> = {
      SHIPPING: "shipping",
    }

    try {
      setActionLoading(actionMap[status])
      await updatePurchaseOrderStatus(order.purchaseOrderId, status)
      messageApi.success(`Status updated to ${status}`)
      await load()
    } catch (error) {
      messageApi.error(getActionError(error, "Failed to update status"))
    } finally {
      setActionLoading(null)
    }
  }

  const currentStatus = normalizeOrderStatus(order?.status)

  const onExportPdf = async () => {
    if (!order) return
    try {
      setExportingPdf(true)
      await downloadPurchaseOrderPdf(order.purchaseOrderId, order.orderCode)
      messageApi.success("PDF exported successfully")
    } catch {
      messageApi.error("Failed to export PDF")
    } finally {
      setExportingPdf(false)
    }
  }

  const onSendEmail = async () => {
    if (!order) return
    try {
      setSendingEmail(true)
      const result = await sendPurchaseOrderEmail(order.purchaseOrderId)
      messageApi.success(result.message || "Purchase order email sent")
    } catch {
      messageApi.error("Failed to send purchase order email")
    } finally {
      setSendingEmail(false)
    }
  }

  return (
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider
        width={260}
        className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen"
      >
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Purchase Order Detail" subtitle="Procurement" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <Space>
            <Link to="/purchase-orders">
              <Button>Back</Button>
            </Link>
            <Button
              type="primary"
              onClick={() => void onConfirm()}
              loading={actionLoading === "confirm"}
              disabled={!order || currentStatus !== "PENDING"}
            >
              Confirm
            </Button>
            <Link to={order ? `/purchase-orders/${order.purchaseOrderId}/edit` : "#"}>
              <Button disabled={!order || currentStatus !== "PENDING"}>Edit</Button>
            </Link>
            <Button
              onClick={() => void onStatusChange("SHIPPING")}
              loading={actionLoading === "shipping"}
              disabled={!order || currentStatus !== "CONFIRMED"}
            >
              Mark Shipped
            </Button>
            <Link to={order ? `/goods-receipts/new/${order.purchaseOrderId}` : "#"}>
              <Button disabled={!order || currentStatus !== "SHIPPING"}>
                Create Goods Receipt
              </Button>
            </Link>
            <Button
              onClick={onExportPdf}
              loading={exportingPdf}
              disabled={!order || currentStatus !== "CONFIRMED"}
            >
              Export PDF
            </Button>
            <Button
              onClick={onSendEmail}
              loading={sendingEmail}
              disabled={!order || currentStatus !== "CONFIRMED"}
            >
              Send Email
            </Button>
          </Space>

          <Card loading={loading} className="!rounded-2xl shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            {order ? (
              <Space direction="vertical" size={20} className="w-full">
                <Space className="w-full justify-between">
                  <Title level={4} className="!mb-0">
                    {order.orderCode}
                  </Title>
                  {statusTag(order.status)}
                </Space>

                <Descriptions column={2} bordered size="small">
                  <Descriptions.Item label="Supplier">{order.supplier?.supplierName || "-"}</Descriptions.Item>
                  <Descriptions.Item label="Warehouse">{order.warehouse?.warehouseName || "-"}</Descriptions.Item>
                  <Descriptions.Item label="Created date">
                    {order.createdAt ? new Date(order.createdAt).toLocaleString() : "-"}
                  </Descriptions.Item>
                  <Descriptions.Item label="Total amount">
                    {Number(order.totalAmount || 0).toLocaleString()}
                  </Descriptions.Item>
                </Descriptions>

                <Card size="small" title="Items">
                  <div className="grid gap-3">
                    {order.items?.map((item) => (
                      <div
                        key={item.itemId || `${item.medicine?.medicineId}-${item.requestedQuantity}`}
                        className="rounded-xl border border-slate-200 px-3 py-2"
                      >
                        <Text strong>{item.medicine?.medicineName || `Medicine #${item.medicine?.medicineId}`}</Text>
                        <br />
                        <Text type="secondary">
                          Qty: {Number(item.requestedQuantity || 0).toLocaleString()} | Unit: {Number(item.unitPrice || 0).toLocaleString()}
                        </Text>
                      </div>
                    ))}
                  </div>
                </Card>
              </Space>
            ) : (
              <Text type="secondary">No data</Text>
            )}
          </Card>
        </Content>
      </Layout>
    </Layout>
  )
}
