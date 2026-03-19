import { DeleteOutlined, PlusOutlined } from "@ant-design/icons"
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Layout,
  Select,
  Space,
  Typography,
  message,
} from "antd"
import dayjs from "dayjs"
import { useEffect, useMemo, useState } from "react"
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getAllMedicines } from "../../services/medicines"
import {
  createPurchaseOrder,
  getPurchaseOrderById,
  type CreatePurchaseOrderPayload,
  updatePurchaseOrder,
} from "../../services/purchaseOrders"
import { getMedicineRequests } from "../../services/medicineRequests"
import { getActiveSuppliers, type Supplier } from "../../services/suppliers"
import type { Medicine, Warehouse } from "../../services/types"
import { getWarehouses } from "../../services/warehouses"

const { Content, Sider } = Layout
const { Text } = Typography

type FormValues = {
  supplierId: number
  warehouseId: number
  expectedDeliveryDate?: dayjs.Dayjs
  notes?: string
  items: Array<{
    medicineId: number
    requestedQuantity: number
    unitPrice: number
    expectedExpiryDate?: dayjs.Dayjs
    notes?: string
  }>
}

export default function CreatePurchaseOrderPage() {
  const [form] = Form.useForm<FormValues>()
  const navigate = useNavigate()
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const requestIdParam = Number(searchParams.get("requestId"))
  const editOrderId = Number(id)
  const isEditMode = !Number.isNaN(editOrderId)

  const [messageApi, contextHolder] = message.useMessage()
  const [submitting, setSubmitting] = useState(false)
  const [loadingPrefill, setLoadingPrefill] = useState(false)
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [medicines, setMedicines] = useState<Medicine[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])

  const medicineNameById = useMemo(() => {
    return medicines.reduce<Record<number, string>>((acc, medicine) => {
      acc[medicine.medicineId] = medicine.name
      return acc
    }, {})
  }, [medicines])

  useEffect(() => {
    const init = async () => {
      try {
        const [supplierData, medicineData, warehouseData] = await Promise.all([
          getActiveSuppliers(),
          getAllMedicines(),
          getWarehouses(),
        ])

        setSuppliers(supplierData)
        setMedicines(medicineData)
        setWarehouses(warehouseData)

        if (isEditMode) {
          setLoadingPrefill(true)
          const order = await getPurchaseOrderById(editOrderId)

          if (order.status !== "PENDING") {
            messageApi.warning("Only PENDING purchase orders can be edited")
            navigate(`/purchase-orders/${editOrderId}`)
            return
          }

          form.setFieldsValue({
            supplierId: order.supplier?.supplierId,
            warehouseId: order.warehouse?.warehouseId,
            notes: order.notes,
            expectedDeliveryDate: order.expectedDeliveryDate
              ? dayjs(order.expectedDeliveryDate)
              : undefined,
            items: (order.items || []).map((item) => ({
              medicineId: item.medicine?.medicineId || 0,
              requestedQuantity: item.requestedQuantity || 1,
              unitPrice: item.unitPrice || 0,
              expectedExpiryDate: item.expectedExpiryDate ? dayjs(item.expectedExpiryDate) : undefined,
              notes: item.notes,
            })),
          })
          return
        }

        if (!requestIdParam || Number.isNaN(requestIdParam)) {
          form.setFieldsValue({
            warehouseId: warehouseData[0]?.warehouseId,
            items: [
              {
                medicineId: medicineData[0]?.medicineId,
                requestedQuantity: 1,
                unitPrice: 0,
              },
            ],
          })
          return
        }

        setLoadingPrefill(true)
        const requests = await getMedicineRequests()
        const sourceRequest = requests.find((request) => request.requestId === requestIdParam)

        if (!sourceRequest) {
          messageApi.warning("Request not found. Please create PO manually")
          return
        }

        form.setFieldsValue({
          warehouseId: sourceRequest.warehouseId,
          notes: sourceRequest.notes,
          expectedDeliveryDate: sourceRequest.requiredDate ? dayjs(sourceRequest.requiredDate) : undefined,
          items: sourceRequest.items.map((item) => ({
            medicineId: item.medicineId,
            requestedQuantity: item.quantity,
            unitPrice: 0,
            notes: item.notes,
          })),
        })
      } catch {
        messageApi.error("Failed to load form data")
      } finally {
        setLoadingPrefill(false)
      }
    }

    void init()
  }, [editOrderId, form, isEditMode, messageApi, navigate, requestIdParam])

  const onFinish = async (values: FormValues) => {
    try {
      setSubmitting(true)

      const payload: CreatePurchaseOrderPayload = {
        supplierId: values.supplierId,
        warehouseId: values.warehouseId,
        expectedDeliveryDate: values.expectedDeliveryDate
          ? values.expectedDeliveryDate.format("YYYY-MM-DD")
          : undefined,
        notes: values.notes,
        items: values.items.map((item) => ({
          medicineId: item.medicineId,
          requestedQuantity: item.requestedQuantity,
          unitPrice: item.unitPrice,
          expectedExpiryDate: item.expectedExpiryDate
            ? item.expectedExpiryDate.format("YYYY-MM-DD")
            : undefined,
          notes: item.notes,
        })),
      }

      if (isEditMode) {
        const updated = await updatePurchaseOrder(editOrderId, payload)
        messageApi.success("Purchase order updated")
        navigate(`/purchase-orders/${updated.purchaseOrderId}`)
      } else {
        const created = await createPurchaseOrder(payload)
        messageApi.success("Purchase order created")
        navigate(`/purchase-orders/${created.purchaseOrderId}`)
      }
    } catch {
      messageApi.error(isEditMode ? "Failed to update purchase order" : "Failed to create purchase order")
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
          <TopBar title={isEditMode ? "Edit Purchase Order" : "Create Purchase Order"} subtitle="Procurement" />
        </div>

        <Content className="p-6 pt-[114px]">
          <div className="rounded-2xl bg-white p-6 shadow-[0_12px_28px_rgba(15,23,42,0.06)]">
            <Form<FormValues> form={form} layout="vertical" onFinish={onFinish}>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <Form.Item label="Supplier" name="supplierId" rules={[{ required: true }]}>
                  <Select
                    showSearch
                    optionFilterProp="label"
                    options={suppliers.map((supplier) => ({
                      value: supplier.supplierId,
                      label: supplier.supplierName,
                    }))}
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

                <Form.Item label="Expected Delivery Date" name="expectedDeliveryDate">
                  <DatePicker className="w-full" />
                </Form.Item>
              </div>

              <Form.Item label="Notes" name="notes">
                <Input.TextArea rows={3} placeholder="PO notes" />
              </Form.Item>

              <Space className="mb-2 w-full justify-between">
                <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">PO items</Text>
                {requestIdParam ? (
                  <Link to="/medicine-requests">
                    <Button size="small">Back to requests</Button>
                  </Link>
                ) : null}
              </Space>

              <Form.List
                name="items"
                rules={[
                  {
                    validator: async (_, value) => {
                      if (!value || value.length < 1) {
                        throw new Error("At least one item is required")
                      }
                    },
                  },
                ]}
              >
                {(fields, { add, remove }) => (
                  <div className="space-y-3">
                    {fields.map((field) => (
                      <div
                        key={field.key}
                        className="grid grid-cols-1 gap-3 rounded-xl border border-slate-200 p-3 md:grid-cols-[2fr_1fr_1fr_1fr_2fr_auto]"
                      >
                        <Form.Item
                          label="Medicine"
                          name={[field.name, "medicineId"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <Select
                            showSearch
                            optionFilterProp="label"
                            options={medicines.map((medicine) => ({
                              value: medicine.medicineId,
                              label: medicine.name,
                            }))}
                          />
                        </Form.Item>

                        <Form.Item
                          label="Quantity"
                          name={[field.name, "requestedQuantity"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <InputNumber min={1} className="w-full" />
                        </Form.Item>

                        <Form.Item
                          label="Unit Price"
                          name={[field.name, "unitPrice"]}
                          rules={[{ required: true }]}
                          className="!mb-0"
                        >
                          <InputNumber min={0} className="w-full" />
                        </Form.Item>

                        <Form.Item
                          label="Expected Expiry"
                          name={[field.name, "expectedExpiryDate"]}
                          className="!mb-0"
                        >
                          <DatePicker className="w-full" />
                        </Form.Item>

                        <Form.Item label="Item Notes" name={[field.name, "notes"]} className="!mb-0">
                          <Input
                            placeholder={
                              form.getFieldValue(["items", field.name, "medicineId"])
                                ? medicineNameById[
                                    form.getFieldValue(["items", field.name, "medicineId"])
                                  ]
                                : "Optional"
                            }
                          />
                        </Form.Item>

                        <div className="flex items-end">
                          <Button danger icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                        </div>
                      </div>
                    ))}

                    <Button
                      type="dashed"
                      icon={<PlusOutlined />}
                      onClick={() =>
                        add({
                          medicineId: medicines[0]?.medicineId,
                          requestedQuantity: 1,
                          unitPrice: 0,
                        })
                      }
                    >
                      Add item
                    </Button>
                  </div>
                )}
              </Form.List>

              <div className="mt-6 flex items-center justify-end gap-2">
                <Button onClick={() => navigate(-1)}>Cancel</Button>
                <Button type="primary" htmlType="submit" loading={submitting || loadingPrefill}>
                  {isEditMode ? "Save Changes" : "Create Purchase Order"}
                </Button>
              </div>
            </Form>
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}
