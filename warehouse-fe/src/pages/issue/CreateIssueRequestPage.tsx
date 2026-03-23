import { Alert, Button, DatePicker, Form, Input, InputNumber, Layout, Select, Typography, message } from "antd"
import dayjs from "dayjs"
import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getAllMedicines } from "../../services/medicines"
import { createIssueRequest, getIssueStockInsight, type IssueRequest } from "../../services/issue"
import type { Medicine, Warehouse } from "../../services/types"
import { getWarehouses } from "../../services/warehouses"

const { Content, Sider } = Layout
const { Text } = Typography

type FormValues = {
  medicineId: number
  quantity: number
  warehouseId: number
  department: string
  purpose?: string
  neededDate?: dayjs.Dayjs
}

export default function CreateIssueRequestPage() {
  const [form] = Form.useForm<FormValues>()
  const [messageApi, contextHolder] = message.useMessage()
  const [medicines, setMedicines] = useState<Medicine[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [stockInsight, setStockInsight] = useState<IssueRequest | null>(null)
  const navigate = useNavigate()
  const watchedMedicineId = Form.useWatch("medicineId", form)
  const watchedWarehouseId = Form.useWatch("warehouseId", form)
  const watchedQuantity = Form.useWatch("quantity", form)

  useEffect(() => {
    const load = async () => {
      try {
        const [medicineData, warehouseData] = await Promise.all([getAllMedicines(), getWarehouses()])
        setMedicines(medicineData)
        setWarehouses(warehouseData)
        form.setFieldsValue({
          medicineId: medicineData[0]?.medicineId,
          warehouseId: warehouseData[0]?.warehouseId,
          quantity: 1,
        })
      } catch {
        messageApi.error("Failed to load form data")
      }
    }

    void load()
  }, [form, messageApi])

  useEffect(() => {
    const loadStockInsight = async () => {
      if (!watchedMedicineId || !watchedWarehouseId) {
        setStockInsight(null)
        return
      }

      try {
        const insight = await getIssueStockInsight(
          watchedMedicineId,
          watchedWarehouseId,
          watchedQuantity,
        )
        setStockInsight(insight)
      } catch {
        setStockInsight(null)
      }
    }

    void loadStockInsight()
  }, [watchedMedicineId, watchedWarehouseId, watchedQuantity])

  const applySuggestedQuantity = () => {
    if (stockInsight?.suggestedAvailableQuantity != null) {
      form.setFieldValue("quantity", stockInsight.suggestedAvailableQuantity)
    }
  }

  const switchToWarehouse = (warehouseId?: number) => {
    if (warehouseId) {
      form.setFieldValue("warehouseId", warehouseId)
    }
  }

  const onFinish = async (values: FormValues) => {
    try {
      setSubmitting(true)
      await createIssueRequest({
        medicineId: values.medicineId,
        quantity: values.quantity,
        warehouseId: values.warehouseId,
        department: values.department,
        purpose: values.purpose,
        neededDate: values.neededDate ? values.neededDate.toISOString() : undefined,
      })
      messageApi.success("Issue request created")
      navigate("/issue-request")
    } catch {
      messageApi.error("Failed to create issue request")
    } finally {
      setSubmitting(false)
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
        <Content className="p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Form<FormValues> form={form} layout="vertical" onFinish={onFinish}>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Form.Item label="Medicine" name="medicineId" rules={[{ required: true }]}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    options={medicines.map((medicine) => ({ value: medicine.medicineId, label: medicine.name }))}
                  />
                </Form.Item>
                <Form.Item label="Warehouse" name="warehouseId" rules={[{ required: true }]}>
                  <Select
                    options={warehouses.map((warehouse) => ({
                      value: warehouse.warehouseId,
                      label: warehouse.name || `Warehouse ${warehouse.warehouseId}`,
                    }))}
                  />
                </Form.Item>
                <Form.Item label="Quantity" name="quantity" rules={[{ required: true }]}>
                  <InputNumber min={1} className="w-full" />
                </Form.Item>
                <Form.Item label="Department" name="department" rules={[{ required: true }]}>
                  <Input placeholder="Example: Inpatient, Emergency" />
                </Form.Item>
                <Form.Item label="Needed Date" name="neededDate">
                  <DatePicker className="w-full" />
                </Form.Item>
              </div>
              <Form.Item label="Purpose" name="purpose">
                <Input.TextArea rows={3} placeholder="Issue purpose" />
              </Form.Item>

              {stockInsight && watchedQuantity > 0 ? (
                stockInsight.availableStockInWarehouse != null && stockInsight.availableStockInWarehouse < watchedQuantity ? (
                  <Alert
                    type="warning"
                    showIcon
                    className="mb-4"
                    message={`Only ${stockInsight.availableStockInWarehouse} available in ${stockInsight.warehouseName || "selected warehouse"}`}
                    description={
                      <div className="space-y-1">
                        <Text className="block">You requested: {watchedQuantity}</Text>
                        <div className="flex flex-wrap items-center gap-2">
                          <Button size="small" onClick={applySuggestedQuantity}>
                            Use {stockInsight.suggestedAvailableQuantity ?? stockInsight.availableStockInWarehouse}
                          </Button>
                          {stockInsight.alternativeWarehouses?.map((option) => (
                            <Button
                              key={option.warehouseId}
                              size="small"
                              type="default"
                              onClick={() => switchToWarehouse(option.warehouseId)}
                            >
                              Switch to {option.warehouseName || `Warehouse ${option.warehouseId}`} ({option.availableQuantity})
                            </Button>
                          ))}
                        </div>
                        <Text type="secondary" className="block">
                          You can still submit this request. Manager will decide approve/reject/partial.
                        </Text>
                      </div>
                    }
                  />
                ) : (
                  <Alert
                    type="success"
                    showIcon
                    className="mb-4"
                    message={`Current stock (${stockInsight.warehouseName || "Warehouse"}): ${stockInsight.availableStockInWarehouse ?? 0}`}
                  />
                )
              ) : null}

              <div className="mt-4 flex justify-end gap-2">
                <Button onClick={() => navigate(-1)}>Cancel</Button>
                <Button type="primary" htmlType="submit" loading={submitting}>Submit Request</Button>
              </div>
            </Form>
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}
