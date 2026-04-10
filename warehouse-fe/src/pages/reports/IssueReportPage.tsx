import { Button, DatePicker, Input, Layout, Select, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import dayjs from "dayjs"
import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import FilterPanel from "../../components/reporting/FilterPanel"
import DataTable from "../../components/reporting/DataTable"
import { useIssueReportQuery } from "../../hooks/useReports"
import { getAllMedicines } from "../../services/medicines"
import { getWarehouses } from "../../services/warehouses"
import { reportApi, type IssueReportItem } from "../../services/reports"

const { Content, Sider } = Layout
const { RangePicker } = DatePicker
const { Text } = Typography

export default function IssueReportPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [medicineId, setMedicineId] = useState<number | undefined>()
  const [department, setDepartment] = useState("")
  const [warehouseId, setWarehouseId] = useState<number | undefined>()
  const [issuedById, setIssuedById] = useState<number | undefined>()
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)

  const params = useMemo(
    () => ({
      medicineId,
      department: department.trim() || undefined,
      warehouseId,
      issuedById,
      fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
      toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
      page: page - 1,
      size,
    }),
    [medicineId, department, warehouseId, issuedById, dateRange, page, size],
  )

  const { data, isLoading } = useIssueReportQuery(params)
  const { data: medicines = [] } = useQuery({ queryKey: ["medicines"], queryFn: getAllMedicines })
  const { data: warehouses = [] } = useQuery({ queryKey: ["warehouses"], queryFn: () => getWarehouses() })

  const issuedByOptions = useMemo(() => {
    const map = new Map<number, string>()
    ;(data?.items || []).forEach((item) => {
      if (item.issuedById && item.issuedByName) {
        map.set(item.issuedById, item.issuedByName)
      }
    })
    return Array.from(map.entries()).map(([value, label]) => ({ value, label }))
  }, [data?.items])

  const columns: ColumnsType<IssueReportItem> = [
    { title: "Mã yêu cầu", dataIndex: "requestId", width: 100 },
    { title: "Thuốc", dataIndex: "medicineName" },
    { title: "Số lô", dataIndex: "lotNumber", width: 130 },
    { title: "Số lượng", dataIndex: "quantity", width: 90 },
    { title: "Kho", dataIndex: "warehouseName", width: 160 },
    { title: "Người cấp", dataIndex: "issuedByName", width: 150 },
    { title: "Khoa/Phòng", dataIndex: "department", width: 150 },
    {
      title: "Thời điểm cấp",
      dataIndex: "issuedAt",
      width: 170,
      render: (v?: string) => (v ? new Date(v).toLocaleString() : "-"),
    },
  ]

  const onExport = async () => {
    try {
      const exportData = await reportApi.exportIssueReport({
        medicineId,
        department: department.trim() || undefined,
        warehouseId,
        issuedById,
        fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
        toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
      })

      const headers = [
        "Mã dòng",
        "Mã yêu cầu",
        "Mã thuốc",
        "Tên thuốc",
        "Mã lô",
        "Số lô",
        "Hạn dùng",
        "Số lượng",
        "Mã kho",
        "Tên kho",
        "Mã người cấp",
        "Tên người cấp",
        "Khoa/Phòng",
        "Thời điểm cấp",
      ]
      const rows = exportData.items.map((item) => [
        item.orderItemId,
        item.requestId,
        item.medicineId,
        item.medicineName || "",
        item.batchId || "",
        item.lotNumber || "",
        item.expiryDate || "",
        item.quantity,
        item.warehouseId || "",
        item.warehouseName || "",
        item.issuedById || "",
        item.issuedByName || "",
        item.department || "",
        item.issuedAt || "",
      ])

      const csv = [headers.join(","), ...rows.map((row) => row.map((v) => `"${String(v ?? "").replaceAll('"', '""')}"`).join(","))].join("\n")
      const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" })
      const url = URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url
      link.download = `bao-cao-cap-thuoc-${dayjs().format("YYYYMMDD-HHmmss")}.csv`
      link.click()
      URL.revokeObjectURL(url)
      messageApi.success("Xuất CSV báo cáo thành công")
    } catch {
      messageApi.error("Xuất CSV báo cáo thất bại")
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

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <FilterPanel
            onReset={() => {
              setMedicineId(undefined)
              setDepartment("")
              setWarehouseId(undefined)
              setIssuedById(undefined)
              setDateRange(null)
              setPage(1)
            }}
            extraActions={<Button onClick={() => void onExport()}>Xuất file</Button>}
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Thuốc</Text>
              <Select
                allowClear
                placeholder="Tất cả thuốc"
                value={medicineId}
                options={medicines.map((m) => ({ value: m.medicineId, label: m.name }))}
                onChange={(value) => {
                  setMedicineId(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Khoa/Phòng</Text>
              <Input
                value={department}
                placeholder="Nhập khoa/phòng"
                onChange={(e) => {
                  setDepartment(e.target.value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Kho</Text>
              <Select
                allowClear
                placeholder="Tất cả kho"
                value={warehouseId}
                options={warehouses.map((w) => ({ value: w.warehouseId, label: w.name || `Kho ${w.warehouseId}` }))}
                onChange={(value) => {
                  setWarehouseId(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Người cấp</Text>
              <Select
                allowClear
                placeholder="Tất cả người dùng"
                value={issuedById}
                options={issuedByOptions}
                onChange={(value) => {
                  setIssuedById(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Ngày cấp</Text>
              <RangePicker
                className="w-full"
                value={dateRange}
                onChange={(value) => {
                  setDateRange(value as [dayjs.Dayjs, dayjs.Dayjs] | null)
                  setPage(1)
                }}
              />
            </div>
          </FilterPanel>

          <DataTable<IssueReportItem>
            rowKey="orderItemId"
            columns={columns}
            dataSource={data?.items || []}
            loading={isLoading}
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Báo cáo yêu cầu cấp thuốc</Text>}
            pagination={{
              current: page,
              pageSize: size,
              total: data?.totalElements || 0,
              showSizeChanger: true,
              pageSizeOptions: ["10", "20", "50", "100"],
              onChange: (nextPage, nextSize) => {
                setPage(nextPage)
                setSize(nextSize)
              },
            }}
          />
        </Content>
      </Layout>
    </Layout>
  )
}
