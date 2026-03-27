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
      toast.success("Tạo yêu cầu cấp thuốc thành công")
      setCreateOpen(false)
      form.resetFields()
      await loadData()
    } catch (error) {
      toast.error(error, "Tạo yêu cầu cấp thuốc thất bại")
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<IssueRequest> = [
    { title: "Mã yêu cầu", dataIndex: "orderId", width: 110 },
    { title: "Thuốc", dataIndex: "medicineName" },
    { title: "SL yêu cầu", dataIndex: "requestedQuantity", width: 100 },
    { title: "SL duyệt", dataIndex: "approvedQuantity", width: 100, render: (v?: number) => v ?? "-" },
    { title: "Khoa/Phòng", dataIndex: "department", width: 140 },
    { title: "Ngày cần", dataIndex: "neededDate", width: 170, render: (v?: string) => (v ? new Date(v).toLocaleString() : "-") },
    { title: "Trạng thái", dataIndex: "status", width: 120, render: (v: string) => <IssueStatusTag status={v} /> },
    {
      title: "Lý do từ chối",
      dataIndex: "rejectionReason",
      width: 260,
      render: (value?: string, record?: IssueRequest) => {
        if (record?.status !== "REJECTED") return "-"
        return value?.trim() ? value : "Không có lý do"
      },
    },
  ]

  return (
    <MainLayout>
      {contextHolder}
      <BaseTable
        title={() => (
          <div className="flex items-center justify-between">
            <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Danh sách yêu cầu cấp thuốc</Text>
            <Button type="primary" onClick={() => setCreateOpen(true)}>
              Tạo yêu cầu
            </Button>
          </div>
        )}
        columns={columns}
        dataSource={rows}
        rowKey="orderId"
        loading={loading}
      />

      <Modal
        title="Tạo yêu cầu cấp thuốc"
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
            <Form.Item label="Thuốc" name="medicineId" rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={medicines.map((medicine) => ({ value: medicine.medicineId, label: medicine.name }))}
              />
            </Form.Item>

            <Form.Item label="Kho" name="warehouseId" rules={[{ required: true }]}>
              <Select
                options={warehouses.map((warehouse) => ({
                  value: warehouse.warehouseId,
                  label: warehouse.name || `Kho ${warehouse.warehouseId}`,
                }))}
              />
            </Form.Item>

            <Form.Item label="Số lượng" name="quantity" rules={[{ required: true }]}>
              <InputNumber min={1} className="w-full" />
            </Form.Item>

            <Form.Item label="Khoa/Phòng" name="department" rules={[{ required: true }]}>
              <Input placeholder="Ví dụ: Nội trú, Cấp cứu" />
            </Form.Item>

            <Form.Item label="Ngày cần" name="neededDate">
              <DatePicker className="w-full" />
            </Form.Item>
          </div>

          <Form.Item label="Mục đích" name="purpose">
            <Input.TextArea rows={3} placeholder="Mục đích cấp thuốc" />
          </Form.Item>

          {stockInsight && watchedQuantity > 0 ? (
            stockInsight.availableStockInWarehouse != null && stockInsight.availableStockInWarehouse < watchedQuantity ? (
              <Alert
                type="warning"
                showIcon
                className="mb-4"
                  message={`Chỉ còn ${stockInsight.availableStockInWarehouse} trong ${stockInsight.warehouseName || "kho đã chọn"}`}
                description={
                  <div className="space-y-1">
                    <Text className="block">Bạn yêu cầu: {watchedQuantity}</Text>
                    <div className="flex flex-wrap items-center gap-2">
                      <Button size="small" onClick={applySuggestedQuantity}>
                        Dùng {stockInsight.suggestedAvailableQuantity ?? stockInsight.availableStockInWarehouse}
                      </Button>
                      {stockInsight.alternativeWarehouses?.map((option) => (
                        <Button
                          key={option.warehouseId}
                          size="small"
                          type="default"
                          onClick={() => switchToWarehouse(option.warehouseId)}
                        >
                          Chuyển sang {option.warehouseName || `Kho ${option.warehouseId}`} ({option.availableQuantity})
                        </Button>
                      ))}
                    </div>
                    <Text type="secondary" className="block">
                      Bạn vẫn có thể gửi yêu cầu. Quản lý sẽ quyết định duyệt, từ chối hoặc duyệt một phần.
                    </Text>
                  </div>
                }
              />
            ) : (
              <Alert
                type="success"
                showIcon
                className="mb-4"
                message={`Tồn kho hiện tại (${stockInsight.warehouseName || "Kho"}): ${stockInsight.availableStockInWarehouse ?? 0}`}
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
              Hủy
            </Button>
            <Button type="primary" htmlType="submit" loading={submitting}>
              Gửi yêu cầu
            </Button>
          </div>
        </Form>
      </Modal>
    </MainLayout>
  )
}
