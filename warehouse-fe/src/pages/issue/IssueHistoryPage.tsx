import { Button, DatePicker, Input, Layout, Select, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import dayjs from "dayjs"
import { useEffect, useState } from "react"
import BaseFilterCard from "../../components/base/BaseFilterCard"
import BaseTable from "../../components/base/BaseTable"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { getIssueHistory, type IssueExecutionItem } from "../../services/issue"
import { getAllMedicines } from "../../services/medicines"
import type { Medicine } from "../../services/types"

const { Content, Sider } = Layout
const { Text } = Typography

export default function IssueHistoryPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [loading, setLoading] = useState(false)
  const [rows, setRows] = useState<IssueExecutionItem[]>([])
  const [medicines, setMedicines] = useState<Medicine[]>([])
  const [medicineId, setMedicineId] = useState<number | undefined>()
  const [department, setDepartment] = useState("")
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await getIssueHistory({
        medicineId,
        department: department.trim() || undefined,
        fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
        toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
      })
      setRows(data)
    } catch {
      messageApi.error("Không thể tải lịch sử cấp thuốc")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const loadMedicines = async () => {
      try {
        setMedicines(await getAllMedicines())
      } catch {
        messageApi.error("Không thể tải danh sách thuốc")
      }
    }
    void loadMedicines()
    void loadData()
  }, [])

  const columns: ColumnsType<IssueExecutionItem> = [
    { title: "Mã yêu cầu", dataIndex: "orderId", width: 100 },
    { title: "Thuốc", dataIndex: "medicineName" },
    { title: "Số lô", dataIndex: "lotNumber", width: 140 },
    { title: "Hạn dùng", dataIndex: "expiryDate", width: 120, render: (v?: string) => (v ? new Date(v).toLocaleDateString() : "-") },
    { title: "Số lượng", dataIndex: "quantity", width: 80 },
    { title: "Khoa/Phòng", dataIndex: "department", width: 140 },
    { title: "Kho", dataIndex: "warehouseName", width: 150 },
    { title: "Người cấp", dataIndex: "issuedByName", width: 150 },
    { title: "Thời điểm cấp", dataIndex: "issuedAt", width: 170, render: (v?: string) => (v ? new Date(v).toLocaleString() : "-") },
  ]

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
        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <BaseFilterCard
            actions={<Button type="primary" onClick={() => void loadData()}>Áp dụng lọc</Button>}
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Thuốc</Text>
              <Select
                allowClear
                placeholder="Tất cả thuốc"
                value={medicineId}
                options={medicines.map((medicine) => ({ value: medicine.medicineId, label: medicine.name }))}
                onChange={(value) => setMedicineId(value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Khoa/Phòng</Text>
              <Input value={department} onChange={(e) => setDepartment(e.target.value)} placeholder="Nhập khoa/phòng" />
            </div>
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Ngày cấp</Text>
              <DatePicker.RangePicker
                className="w-full"
                value={dateRange}
                onChange={(value) => setDateRange(value as [dayjs.Dayjs, dayjs.Dayjs] | null)}
              />
            </div>
          </BaseFilterCard>

          <BaseTable
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Lịch sử cấp thuốc</Text>}
            columns={columns}
            dataSource={rows}
            rowKey={(row) => `${row.orderItemId}`}
            loading={loading}
            scroll={{ x: 1200 }}
          />
        </Content>
      </Layout>
    </Layout>
  )
}
