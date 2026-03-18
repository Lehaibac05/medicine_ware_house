import { Button, DatePicker, Input, Layout, Select, Tag, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useMemo, useState } from "react"
import dayjs from "dayjs"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import { useInventoryReportQuery } from "../../hooks/useReports"
import FilterPanel from "../../components/reporting/FilterPanel"
import DataTable from "../../components/reporting/DataTable"
import { reportApi, type InventoryReportItem, type InventoryStatus } from "../../services/reports"
import { getWarehouses } from "../../services/warehouses"
import { useQuery } from "@tanstack/react-query"

const { Content, Sider } = Layout
const { RangePicker } = DatePicker
const { Text } = Typography

const statusTag = (status: InventoryStatus) => {
  if (status === "LOW_STOCK") return <Tag color="red">LOW_STOCK</Tag>
  if (status === "EXPIRING_SOON") return <Tag color="gold">EXPIRING_SOON</Tag>
  return <Tag color="green">NORMAL</Tag>
}

export default function InventoryReportPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [medicineName, setMedicineName] = useState("")
  const [medicineGroup, setMedicineGroup] = useState("")
  const [warehouseId, setWarehouseId] = useState<number | undefined>()
  const [status, setStatus] = useState<InventoryStatus | undefined>()
  const [expiryRange, setExpiryRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)
  const [periodType, setPeriodType] = useState<"CUSTOM" | "MONTH" | "QUARTER" | "YEAR">("CUSTOM")
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)

  const params = useMemo(
    () => ({
      page: page - 1,
      size,
      medicineName: medicineName.trim() || undefined,
      medicineGroup: medicineGroup.trim() || undefined,
      warehouseId,
      status,
      expiryFrom: expiryRange?.[0]?.format("YYYY-MM-DD"),
      expiryTo: expiryRange?.[1]?.format("YYYY-MM-DD"),
    }),
    [page, size, medicineName, medicineGroup, warehouseId, status, expiryRange]
  )

  const { data, isLoading } = useInventoryReportQuery(params)
  const { data: warehouses = [] } = useQuery({ queryKey: ["warehouses"], queryFn: getWarehouses })

  const columns: ColumnsType<InventoryReportItem> = [
    { title: "Medicine", dataIndex: "medicineName" },
    { title: "Warehouse", dataIndex: "warehouseName" },
    { title: "Total Stock", dataIndex: "totalStock", width: 130 },
    { title: "Batch Count", dataIndex: "batchCount", width: 120 },
    { title: "Nearest Expiry", dataIndex: "nearestExpiryDate", width: 150, render: (value?: string) => value || "-" },
    { title: "Status", dataIndex: "status", width: 140, render: (value: InventoryStatus) => statusTag(value) },
  ]

  const onExport = async () => {
    try {
      const exportData = await reportApi.exportInventoryReport({
        medicineName: medicineName.trim() || undefined,
        medicineGroup: medicineGroup.trim() || undefined,
        warehouseId,
        status,
        expiryFrom: expiryRange?.[0]?.format("YYYY-MM-DD"),
        expiryTo: expiryRange?.[1]?.format("YYYY-MM-DD"),
      })

      const headers = [
        "medicineId",
        "medicineName",
        "warehouseId",
        "warehouseName",
        "totalStock",
        "batchCount",
        "nearestExpiryDate",
        "status",
        "reorderLevel",
      ]
      const rows = exportData.items.map((item) => [
        item.medicineId,
        item.medicineName,
        item.warehouseId,
        item.warehouseName,
        item.totalStock,
        item.batchCount,
        item.nearestExpiryDate || "",
        item.status,
        item.reorderLevel ?? "",
      ])
      const csv = [headers.join(","), ...rows.map((row) => row.map((v) => `"${String(v ?? "").replaceAll('"', '""')}"`).join(","))].join("\n")
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" })
      const url = URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url
      link.download = `inventory-report-${dayjs().format("YYYYMMDD-HHmmss")}.csv`
      link.click()
      URL.revokeObjectURL(url)
      messageApi.success("Inventory CSV export ready")
    } catch {
      messageApi.error("Failed to export inventory report")
    }
  }

  const applyPeriodPreset = (type: "CUSTOM" | "MONTH" | "QUARTER" | "YEAR") => {
    setPeriodType(type)
    const now = dayjs()
    if (type === "CUSTOM") {
      return
    }
    if (type === "MONTH") {
      setExpiryRange([now.startOf("month"), now.endOf("month")])
      setPage(1)
      return
    }
    if (type === "QUARTER") {
      const quarterStartMonth = Math.floor(now.month() / 3) * 3
      const quarterStart = now.month(quarterStartMonth).startOf("month")
      const quarterEnd = quarterStart.add(2, "month").endOf("month")
      setExpiryRange([quarterStart, quarterEnd])
      setPage(1)
      return
    }
    setExpiryRange([now.startOf("year"), now.endOf("year")])
    setPage(1)
  }

  return (
    <Layout className="min-h-screen bg-slate-100">
      {contextHolder}
      <Sider width={260} className="hidden lg:block !bg-white border-r border-slate-200 px-4 py-6 !fixed left-0 top-0 h-screen">
        <SidebarNav />
      </Sider>

      <Layout className="lg:ml-[260px]">
        <div className="fixed left-0 top-0 z-20 w-full lg:pl-[260px]">
          <TopBar title="Inventory Report" subtitle="Reporting" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <FilterPanel
            onReset={() => {
              setMedicineName("")
              setMedicineGroup("")
              setWarehouseId(undefined)
              setStatus(undefined)
              setExpiryRange(null)
              setPeriodType("CUSTOM")
              setPage(1)
            }}
            extraActions={
              <Button onClick={() => void onExport()}>Export</Button>
            }
          >
            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Warehouse</Text>
              <Select
                allowClear
                placeholder="All warehouses"
                value={warehouseId}
                options={warehouses.map((warehouse) => ({ value: warehouse.warehouseId, label: warehouse.name }))}
                onChange={(value) => {
                  setWarehouseId(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine Name</Text>
              <Input
                value={medicineName}
                placeholder="Search medicine"
                onChange={(event) => {
                  setMedicineName(event.target.value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Medicine Group</Text>
              <Input
                value={medicineGroup}
                placeholder="Example: manufacturer/group"
                onChange={(event) => {
                  setMedicineGroup(event.target.value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Status</Text>
              <Select
                allowClear
                value={status}
                placeholder="All status"
                options={[
                  { value: "LOW_STOCK", label: "LOW_STOCK" },
                  { value: "NORMAL", label: "NORMAL" },
                  { value: "EXPIRING_SOON", label: "EXPIRING_SOON" },
                ]}
                onChange={(value) => {
                  setStatus(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Period</Text>
              <Select
                value={periodType}
                options={[
                  { value: "CUSTOM", label: "Custom" },
                  { value: "MONTH", label: "This Month" },
                  { value: "QUARTER", label: "This Quarter" },
                  { value: "YEAR", label: "This Year" },
                ]}
                onChange={(value) => applyPeriodPreset(value)}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Expiry Range</Text>
              <RangePicker
                className="w-full"
                value={expiryRange}
                onChange={(value) => {
                  setExpiryRange(value as [dayjs.Dayjs, dayjs.Dayjs] | null)
                  setPeriodType("CUSTOM")
                  setPage(1)
                }}
              />
            </div>
          </FilterPanel>

          <DataTable<InventoryReportItem>
            rowKey={(row) => `${row.medicineId}-${row.warehouseId}`}
            columns={columns}
            dataSource={data?.items || []}
            loading={isLoading}
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Inventory Report</Text>}
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
