import { Button, DatePicker, Form, Input, InputNumber, Layout, Space, Typography, message } from "antd"
import { useEffect, useMemo, useRef } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import dayjs from "dayjs"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { useCreateGoodsReceiptMutation, usePurchaseOrderQuery } from "../../hooks/useWorkflow"

const { Content, Sider } = Layout
const { Text } = Typography

type ReceivedItemForm = {
  itemId: number
  medicine: string
  requestedQuantity: number
  receivedQuantity: number
  actualExpiryDate?: dayjs.Dayjs
  lotNumber?: string
  manufactureDate?: dayjs.Dayjs
}

type FormValues = {
  qualityCheckNotes?: string
  qualityPassed?: boolean
  receivedItems: ReceivedItemForm[]
}

const normalizeOrderStatus = (status?: string) => status?.trim().toUpperCase() ?? ""

const toOrderStatusLabel = (status?: string) => {
  const normalized = normalizeOrderStatus(status)
  if (normalized === "PENDING") return "Chờ xử lý"
  if (normalized === "CONFIRMED") return "Đã xác nhận"
  if (normalized === "SHIPPING") return "Đang giao hàng"
  if (normalized === "RECEIVED") return "Đã nhận hàng"
  if (normalized === "APPROVED") return "Đã duyệt"
  if (normalized === "REJECTED") return "Đã từ chối"
  if (normalized === "CANCELLED") return "Đã hủy"
  return normalized || "-"
}

export default function GoodsReceiptCreatePage() {
  const { purchaseOrderId } = useParams()
  const navigate = useNavigate()
  const [form] = Form.useForm<FormValues>()
  const initializedRef = useRef(false)
  const [messageApi, contextHolder] = message.useMessage()

  const poId = Number(purchaseOrderId)
  const { data: order } = usePurchaseOrderQuery(poId)
  const createMutation = useCreateGoodsReceiptMutation()

  const initialItems = useMemo(() => {
    if (!order?.items) return []
    return order.items.map((item) => ({
      itemId: item.itemId,
      medicine: item.medicine?.medicineName || `Thuốc #${item.medicine?.medicineId}`,
      requestedQuantity: item.requestedQuantity,
      receivedQuantity: item.requestedQuantity,
    }))
  }, [order])

  const poStatus = normalizeOrderStatus(order?.status)
  const canCreateReceipt = poStatus === "SHIPPING" || poStatus === "CONFIRMED"

  useEffect(() => {
    initializedRef.current = false
    form.resetFields()
  }, [poId, form])

  useEffect(() => {
    if (initializedRef.current || initialItems.length === 0) {
      return
    }

    const existingItems = form.getFieldValue("receivedItems") as FormValues["receivedItems"] | undefined
    if (existingItems && existingItems.length > 0) {
      initializedRef.current = true
      return
    }

    form.setFieldsValue({
      qualityPassed: true,
      receivedItems: initialItems,
    })
    initializedRef.current = true
  }, [form, initialItems])

  const onFinish = async (values: FormValues) => {
    if (!canCreateReceipt) {
      messageApi.error("Chỉ có thể tạo phiếu nhập khi đơn mua hàng đã xác nhận hoặc đang giao")
      return
    }

    try {
      await createMutation.mutateAsync({
        purchaseOrderId: poId,
        qualityCheckNotes: values.qualityCheckNotes,
        qualityPassed: values.qualityPassed ?? true,
        receivedItems: values.receivedItems.map((item, index) => ({
          itemId: item.itemId ?? initialItems[index]?.itemId,
          receivedQuantity: item.receivedQuantity,
          actualExpiryDate: item.actualExpiryDate ? item.actualExpiryDate.format("YYYY-MM-DD") : undefined,
          lotNumber: item.lotNumber,
          manufactureDate: item.manufactureDate ? item.manufactureDate.format("YYYY-MM-DD") : undefined,
        })),
      })
      messageApi.success("Tạo phiếu nhập thành công. Đơn mua hàng đã chuyển sang trạng thái đã nhận và chờ quản lý duyệt")
      navigate("/goods-receipts")
    } catch {
      messageApi.error("Tạo phiếu nhập thất bại")
    }
  }

  return (
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider width={260} className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen">
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar />
        </div>

        <Content className="p-6 pt-[114px] flex flex-col gap-6">
          <div className="rounded-2xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Text strong>Đơn mua hàng: {order?.orderCode || `#${poId}`}</Text>
            <br />
            <Text type="secondary">Trạng thái hiện tại: {toOrderStatusLabel(poStatus)}</Text>
          </div>

          <Form
            form={form}
            layout="vertical"
            onFinish={onFinish}
          >
            <div className="rounded-2xl bg-white p-4 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
              <Form.Item name="qualityCheckNotes" label="Ghi chú kiểm tra chất lượng">
                <Input.TextArea rows={3} />
              </Form.Item>

              <div className="grid gap-3">
                {initialItems.map((item, index) => (
                  <div
                    key={`${item.itemId}-${index}`}
                    className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 p-3 md:grid-cols-[2fr_1fr_1fr_1fr_1fr]"
                  >
                    <Form.Item name={["receivedItems", index, "itemId"]} hidden>
                      <InputNumber />
                    </Form.Item>

                    <div>
                      <Text className="text-xs text-slate-500">Thuốc</Text>
                      <div className="font-medium text-slate-900">{item.medicine}</div>
                      <Text className="text-xs text-slate-500">Số lượng đặt: {item.requestedQuantity}</Text>
                    </div>

                    <Form.Item
                      name={["receivedItems", index, "receivedQuantity"]}
                      label="Số lượng nhận"
                      className="!mb-0"
                      rules={[{ required: true }]}
                    >
                      <InputNumber min={1} className="w-full" />
                    </Form.Item>

                    <Form.Item
                      name={["receivedItems", index, "actualExpiryDate"]}
                      label="Hạn dùng thực tế"
                      className="!mb-0"
                    >
                      <DatePicker className="w-full" />
                    </Form.Item>

                    <Form.Item
                      name={["receivedItems", index, "lotNumber"]}
                      label="Số lô"
                      className="!mb-0"
                    >
                      <Input />
                    </Form.Item>

                    <Form.Item
                      name={["receivedItems", index, "manufactureDate"]}
                      label="Ngày sản xuất"
                      className="!mb-0"
                    >
                      <DatePicker className="w-full" />
                    </Form.Item>
                  </div>
                ))}
              </div>

              <Space className="mt-4 w-full justify-end">
                <Link to={`/purchase-orders/${poId}`}>
                  <Button>Hủy</Button>
                </Link>
                <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
                  Gửi phiếu nhập
                </Button>
              </Space>
            </div>
          </Form>
        </Content>
      </Layout>
    </Layout>
  )
}
