import { Button, Form, Input, InputNumber, Layout, Select, Typography, message } from "antd"
import { useEffect, useMemo, useState } from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getBatchesByMedicine, updateBatch } from "../../services/batches"
import type { Batch } from "../../services/types"

const { Content, Sider } = Layout
const { Text } = Typography

type FormValues = {
  batchId: number
  newQuantity: number
  reason: string
}

export default function InventoryAdjustmentPage() {
  const [form] = Form.useForm<FormValues>()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [messageApi, contextHolder] = message.useMessage()

  const medicineId = Number(searchParams.get("medicineId"))
  const warehouseId = Number(searchParams.get("warehouseId"))

  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [batches, setBatches] = useState<Batch[]>([])

  useEffect(() => {
    const loadBatches = async () => {
      if (!medicineId || Number.isNaN(medicineId)) {
        messageApi.error("Invalid medicine id")
        navigate("/inventory", { replace: true })
        return
      }

      try {
        setLoading(true)
        const data = await getBatchesByMedicine(medicineId)
        const filtered = warehouseId && !Number.isNaN(warehouseId)
          ? data.filter((batch) => batch.warehouse?.warehouseId === warehouseId)
          : data
        setBatches(filtered)

        if (filtered.length > 0) {
          form.setFieldsValue({
            batchId: filtered[0].batchId,
            newQuantity: filtered[0].quantity,
          })
        }
      } catch {
        messageApi.error("Failed to load batches")
      } finally {
        setLoading(false)
      }
    }

    void loadBatches()
  }, [medicineId, warehouseId, form, messageApi, navigate])

  const selectedBatch = useMemo(() => {
    const batchId = form.getFieldValue("batchId")
    return batches.find((batch) => batch.batchId === batchId) || null
  }, [batches, form])

  const onFinish = async (values: FormValues) => {
    if (!medicineId || Number.isNaN(medicineId)) return

    try {
      setSaving(true)
      const target = batches.find((batch) => batch.batchId === values.batchId)
      if (!target) {
        messageApi.error("Batch not found")
        return
      }

      await updateBatch(medicineId, values.batchId, {
        quantity: values.newQuantity,
      })

      messageApi.success("Inventory quantity updated")
      navigate("/inventory")
    } catch {
      messageApi.error("Failed to update inventory quantity")
    } finally {
      setSaving(false)
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
          <TopBar title="Inventory Correction" subtitle="Adjust Quantity" />
        </div>

        <Content className="p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Text className="mb-4 block text-[11px] uppercase tracking-[0.12em] text-slate-400">
              Manual stock correction
            </Text>

            <Form form={form} layout="vertical" onFinish={onFinish}>
              <Form.Item label="Batch" name="batchId" rules={[{ required: true, message: "Please select a batch" }]}>
                <Select
                  loading={loading}
                  options={batches.map((batch) => ({
                    value: batch.batchId,
                    label: `${batch.lotNumber} | Qty: ${batch.quantity.toLocaleString()} | Exp: ${batch.expiryDate || "-"}`,
                  }))}
                />
              </Form.Item>

              <Form.Item label="Current Quantity">
                <InputNumber value={selectedBatch?.quantity ?? 0} disabled className="w-full" />
              </Form.Item>

              <Form.Item
                label="New Quantity"
                name="newQuantity"
                rules={[
                  { required: true, message: "Please input new quantity" },
                  { type: "number", min: 0, message: "Quantity must be >= 0" },
                ]}
              >
                <InputNumber min={0} className="w-full" />
              </Form.Item>

              <Form.Item
                label="Reason"
                name="reason"
                rules={[{ required: true, message: "Please provide adjustment reason" }]}
              >
                <Input.TextArea rows={3} placeholder="Explain why quantity needs correction" />
              </Form.Item>

              <div className="flex justify-end gap-2">
                <Link to="/inventory">
                  <Button>Cancel</Button>
                </Link>
                <Button type="primary" htmlType="submit" loading={saving}>
                  Apply correction
                </Button>
              </div>
            </Form>
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}
