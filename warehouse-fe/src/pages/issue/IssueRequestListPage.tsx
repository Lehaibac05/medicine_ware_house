import { Alert, Button, DatePicker, Form, Input, InputNumber, Modal, Select, Typography } from "antd"
import type { ColumnsType } from "antd/es/table"
import type { Dayjs } from "dayjs"
import { useEffect, useState } from "react"
import BaseTable from "../../components/base/BaseTable"
import { useToast } from "../../hooks/useToast"
import MainLayout from "../../layouts/MainLayout"
import { createIssueRequest, getIssueStockInsight, getMyIssueRequests, type IssueRequest } from "../../services/issue"
import { getAllMedicines } from "../../services/medicines"
import type { Medicine, Warehouse } from "../../services/types"
import { getWarehouses } from "../../services/warehouses"
import IssueStatusTag from "./components/IssueStatusTag"

const { Text } = Typography

type FormValues = {
  medicineId: number
  quantity: number
  warehouseId: number
  department: string
  purpose?: string
  neededDate?: Dayjs
}

export default function IssueRequestListPage() {
  const [form] = Form.useForm<FormValues>()
  const { toast, contextHolder } = useToast()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<IssueRequest[]>([])
  const [createOpen, setCreateOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [medicines, setMedicines] = useState<Medicine[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [stockInsight, setStockInsight] = useState<IssueRequest | null>(null)

  const watchedMedicineId = Form.useWatch("medicineId", form)
  const watchedWarehouseId = Form.useWatch("warehouseId", form)
  const watchedQuantity = Form.useWatch("quantity", form)

  const loadData = async () => {
    try {
      setLoading(true)
      setRows(await getMyIssueRequests())
    } catch (error) {
      toast.error(error, "Failed to load your issue requests")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  useEffect(() => {
    if (!createOpen) return

    const loadFormData = async () => {
      try {
        const [medicineData, whData] = await Promise.all([getAllMedicines(), getWarehouses()])
        const warehouseList = whData
        setMedicines(medicineData)
        setWarehouses(warehouseList)
        form.setFieldsValue({
          medicineId: medicineData[0]?.medicineId,
          warehouseId: warehouseList[0]?.warehouseId,
          quantity: 1,
        })
      } catch (error) {
        toast.error(error, "Failed to load form data")
      }
    }

    void loadFormData()
  }, [createOpen, form, toast])

  useEffect(() => {
    if (!createOpen || !watchedMedicineId || !watchedWarehouseId) {
      setStockInsight(null)
      return
    }

    const loadStockInsight = async () => {
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
  }, [createOpen, watchedMedicineId, watchedWarehouseId, watchedQuantity])

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

  const onCreateRequest = async (values: FormValues) => {
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
      toast.success("Issue request created successfully")
      setCreateOpen(false)
      form.resetFields()
      await loadData()
    } catch (error) {
      toast.error(error, "Failed to create issue request")
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<IssueRequest> = [
    { title: "Mã yêu cầu", dataIndex: "orderId", width: 110 },
    { title: "Thuốc", dataIndex: "medicineName" },
    { title: "Requested", dataIndex: "requestedQuantity", width: 100 },
    { title: "Approved", dataIndex: "approvedQuantity", width: 100, render: (v?: number) => v ?? "-" },
    { title: "Department", dataIndex: "department", width: 140 },
    { title: "Needed Date", dataIndex: "neededDate", width: 170, render: (v?: string) => (v ? new Date(v).toLocaleString() : "-") },
    { title: "Status", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Reject Reason",
      dataIndex: "rejectionReason",
      width: 260,
      render: (value?: string, record?: IssueRequest) => {
        if (record?.status !== "REJECTED") return "-"
        return value?.trim() ? value : "No reason provided"
      },
    },
  ]

  return (
    <MainLayout>
      {contextHolder}
      <BaseTable
        title={() => (
          <div className="flex items-center justify-between">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">My issue requests</Text>
            <Button type="primary" onClick={() => setCreateOpen(true)}>
              Create Request
            </Button>
          </div>
        )}
        columns={columns}
        dataSource={rows}
        rowKey="orderId"
        loading={loading}
      />

      <Modal
        title="Create Issue Request"
        open={createOpen}
        onCancel={() => {
          if (submitting) return
          setCreateOpen(false)
          form.resetFields()
          setStockInsight(null)
        }}
        footer={null}
        width={860}
        destroyOnClose
      >
        <Form<FormValues> form={form} layout="vertical" onFinish={onCreateRequest}>
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
            <Button
              onClick={() => {
                setCreateOpen(false)
                form.resetFields()
                setStockInsight(null)
              }}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Submit Request
            </Button>
          </div>
        </Form>
      </Modal>
    </MainLayout>
  )
}
