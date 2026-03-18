import { DeleteOutlined, PlusOutlined } from "@ant-design/icons"
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Layout,
  Select,
  Typography,
  message,
} from "antd"
import dayjs from "dayjs"
import { useEffect, useState } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getAllMedicines } from "../../services/medicines"
import { createMedicineRequest } from "../../services/medicineRequests"
import type { Medicine, Warehouse } from "../../services/types"
import { hasAnyRole } from "../../utils/auth"
import { getWarehouses } from "../../services/warehouses"

const { Content, Sider } = Layout
const { Text } = Typography

type FormValues = {
  warehouseId: number
  requiredDate?: dayjs.Dayjs
  notes?: string
  items: Array<{
    medicineId: number
    quantity: number
    notes?: string
  }>
}

export default function CreateMedicineRequestPage() {
  const [form] = Form.useForm<FormValues>()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [submitting, setSubmitting] = useState(false)
  const [medicines, setMedicines] = useState<Medicine[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])

  useEffect(() => {
    const load = async () => {
      const [medData, whData] = await Promise.all([getAllMedicines(), getWarehouses()])
      setMedicines(medData)
      setWarehouses(whData)

      const prefillMedicineId = Number(searchParams.get("medicineId"))
      const prefillWarehouseId = Number(searchParams.get("warehouseId"))
      const suggestedQty = Number(searchParams.get("suggestedQty"))
      const fromAlertId = searchParams.get("fromAlert")

      form.setFieldsValue({
        warehouseId: Number.isFinite(prefillWarehouseId) && prefillWarehouseId > 0
          ? prefillWarehouseId
          : whData[0]?.warehouseId,
        notes: fromAlertId ? `Created from alert #${fromAlertId}` : undefined,
        items: [{
          medicineId:
            Number.isFinite(prefillMedicineId) && prefillMedicineId > 0
              ? prefillMedicineId
              : medData[0]?.medicineId,
          quantity: Number.isFinite(suggestedQty) && suggestedQty > 0 ? suggestedQty : 1,
        }],
      })
    }

    void load()
  }, [form, searchParams])

  const onFinish = async (values: FormValues) => {
    try {
      setSubmitting(true)
      await createMedicineRequest({
        warehouseId: values.warehouseId,
        requiredDate: values.requiredDate ? values.requiredDate.format("YYYY-MM-DD") : undefined,
        notes: values.notes,
        items: values.items.map((item) => ({
          medicineId: item.medicineId,
          quantity: item.quantity,
          notes: item.notes,
        })),
      })
      messageApi.success("Medicine request created")
      const managerView = hasAnyRole(["ROLE_ADMIN", "ROLE_WAREHOUSE_MANAGER"])
      navigate(managerView ? "/requests" : "/medicine-requests")
    } catch {
      messageApi.error("Failed to create medicine request")
    } finally {
      setSubmitting(false)
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
          <TopBar title="Create Medicine Request" subtitle="Procurement" />
        </div>

        <Content className="p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Form<FormValues> form={form} layout="vertical" onFinish={onFinish}>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Form.Item label="Warehouse" name="warehouseId" rules={[{ required: true }]}>
                  <Select
                    options={warehouses.map((warehouse) => ({
                      value: warehouse.warehouseId,
                      label: warehouse.name || `Warehouse ${warehouse.warehouseId}`,
                    }))}
                  />
                </Form.Item>

                <Form.Item label="Required Date" name="requiredDate">
                  <DatePicker className="w-full" />
                </Form.Item>
              </div>

              <Form.Item label="Notes" name="notes">
                <Input.TextArea rows={3} placeholder="Request notes" />
              </Form.Item>

              <Text className="mb-2 block text-[11px] uppercase tracking-[0.12em] text-slate-400">
                Requested medicines
              </Text>

              <Form.List name="items" rules={[{ validator: async (_, value) => {
                if (!value || value.length < 1) {
                  throw new Error("At least one medicine item is required")
                }
              } }]}>
                {(fields, { add, remove }) => (
                  <div className="space-y-3">
                    {fields.map((field) => (
                      <div key={field.key} className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 p-3 md:grid-cols-[2fr_1fr_2fr_auto]">
                        <Form.Item
                          label="Medicine"
                          name={[field.name, "medicineId"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <Select
                            options={medicines.map((medicine) => ({
                              value: medicine.medicineId,
                              label: medicine.name,
                            }))}
                            showSearch
                            optionFilterProp="label"
                          />
                        </Form.Item>

                        <Form.Item
                          label="Quantity"
                          name={[field.name, "quantity"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <InputNumber min={1} className="w-full" />
                        </Form.Item>

                        <Form.Item label="Item Notes" name={[field.name, "notes"]} className="!mb-0">
                          <Input placeholder="Optional" />
                        </Form.Item>

                        <div className="flex items-end">
                          <Button danger icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                        </div>
                      </div>
                    ))}

                    <Button
                      type="dashed"
                      icon={<PlusOutlined />}
                      onClick={() => add({ quantity: 1 })}
                    >
                      Add medicine
                    </Button>
                  </div>
                )}
              </Form.List>

              <div className="mt-6 flex justify-end gap-2">
                <Button onClick={() => navigate(-1)}>Cancel</Button>
                <Button type="primary" htmlType="submit" loading={submitting}>
                  Submit Request
                </Button>
              </div>
            </Form>
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}
