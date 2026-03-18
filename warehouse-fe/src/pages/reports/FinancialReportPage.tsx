import { Button, DatePicker, Layout, Select, Tag, Typography, message } from "antd"
import type { ColumnsType } from "antd/es/table"
import { useMemo, useState } from "react"
import dayjs from "dayjs"
import { useQuery } from "@tanstack/react-query"
import SidebarNav from "../../layouts/SidebarNav"
import TopBar from "../../layouts/TopBar"
import FilterPanel from "../../components/reporting/FilterPanel"
import DataTable from "../../components/reporting/DataTable"
import { useFinancialReportQuery } from "../../hooks/useReports"
import { getActiveSuppliers } from "../../services/suppliers"
import { reportApi, type FinancialReportItem, type FinancialStatus } from "../../services/reports"

const { Content, Sider } = Layout
const { RangePicker } = DatePicker
const { Text } = Typography

const statusTag = (status: FinancialStatus) => {
  if (status === "PAID") return <Tag color="green">PAID</Tag>
  if (status === "PARTIAL") return <Tag color="gold">PARTIAL</Tag>
  return <Tag color="red">UNPAID</Tag>
}

const money = (value: number) =>
  Number(value || 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export default function FinancialReportPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)
  const [periodType, setPeriodType] = useState<"CUSTOM" | "MONTH" | "QUARTER" | "YEAR">("CUSTOM")
  const [supplierId, setSupplierId] = useState<number | undefined>()
  const [status, setStatus] = useState<FinancialStatus | undefined>()
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)

  const params = useMemo(
    () => ({
      page: page - 1,
      size,
      fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
      toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
      supplierId,
      status,
    }),
    [page, size, dateRange, supplierId, status]
  )

  const { data, isLoading } = useFinancialReportQuery(params)
  const { data: suppliers = [] } = useQuery({ queryKey: ["suppliers", "active"], queryFn: getActiveSuppliers })

  const columns: ColumnsType<FinancialReportItem> = [
    { title: "Invoice Code", dataIndex: "invoiceCode", width: 150 },
    { title: "Supplier", dataIndex: "supplierName" },
    { title: "Total Amount", dataIndex: "totalAmount", width: 150, render: (v: number) => money(v) },
    { title: "Paid Amount", dataIndex: "paidAmount", width: 150, render: (v: number) => money(v) },
    { title: "Remaining", dataIndex: "remainingAmount", width: 150, render: (v: number) => money(v) },
    { title: "Status", dataIndex: "status", width: 120, render: (value: FinancialStatus) => statusTag(value) },
  ]

  const onExport = async () => {
    try {
      const exportData = await reportApi.exportFinancialReport({
        fromDate: dateRange?.[0]?.format("YYYY-MM-DD"),
        toDate: dateRange?.[1]?.format("YYYY-MM-DD"),
        supplierId,
        status,
      })
      const headers = [
        "invoiceId",
        "invoiceCode",
        "supplierId",
        "supplierName",
        "invoiceDate",
        "dueDate",
        "totalAmount",
        "paidAmount",
        "remainingAmount",
        "status",
      ]
      const rows = exportData.items.map((item) => [
        item.invoiceId,
        item.invoiceCode,
        item.supplierId ?? "",
        item.supplierName ?? "",
        item.invoiceDate ?? "",
        item.dueDate ?? "",
        item.totalAmount,
        item.paidAmount,
        item.remainingAmount,
        item.status,
      ])
      const csv = [headers.join(","), ...rows.map((row) => row.map((v) => `"${String(v ?? "").replaceAll('"', '""')}"`).join(","))].join("\n")
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" })
      const url = URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url
      link.download = `financial-report-${dayjs().format("YYYYMMDD-HHmmss")}.csv`
      link.click()
      URL.revokeObjectURL(url)
      messageApi.success("Financial CSV export ready")
    } catch {
      messageApi.error("Failed to export financial report")
    }
  }

  const applyPeriodPreset = (type: "CUSTOM" | "MONTH" | "QUARTER" | "YEAR") => {
    setPeriodType(type)
    const now = dayjs()
    if (type === "CUSTOM") {
      return
    }
    if (type === "MONTH") {
      setDateRange([now.startOf("month"), now.endOf("month")])
      setPage(1)
      return
    }
    if (type === "QUARTER") {
      const quarterStartMonth = Math.floor(now.month() / 3) * 3
      const quarterStart = now.month(quarterStartMonth).startOf("month")
      const quarterEnd = quarterStart.add(2, "month").endOf("month")
      setDateRange([quarterStart, quarterEnd])
      setPage(1)
      return
    }
    setDateRange([now.startOf("year"), now.endOf("year")])
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
          <TopBar title="Financial Report" subtitle="Reporting" />
        </div>

        <Content className="flex flex-col gap-6 p-6 pt-[114px]">
          <FilterPanel
            onReset={() => {
              setDateRange(null)
              setPeriodType("CUSTOM")
              setSupplierId(undefined)
              setStatus(undefined)
              setPage(1)
            }}
            extraActions={<Button onClick={() => void onExport()}>Export</Button>}
          >
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
              <Text className="text-xs text-slate-500">Date Range</Text>
              <RangePicker
                className="w-full"
                value={dateRange}
                onChange={(value) => {
                  setDateRange(value as [dayjs.Dayjs, dayjs.Dayjs] | null)
                  setPeriodType("CUSTOM")
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Supplier</Text>
              <Select
                allowClear
                placeholder="All suppliers"
                value={supplierId}
                options={suppliers.map((supplier) => ({ value: supplier.supplierId, label: supplier.supplierName }))}
                onChange={(value) => {
                  setSupplierId(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex flex-col gap-2">
              <Text className="text-xs text-slate-500">Payment Status</Text>
              <Select
                allowClear
                placeholder="All status"
                value={status}
                options={[
                  { value: "PAID", label: "PAID" },
                  { value: "PARTIAL", label: "PARTIAL" },
                  { value: "UNPAID", label: "UNPAID" },
                ]}
                onChange={(value) => {
                  setStatus(value)
                  setPage(1)
                }}
              />
            </div>

            <div className="flex items-end">
              <Text className="text-xs text-slate-500">
                Totals: Invoiced {money(Number(data?.totalInvoiceAmount || 0))}, Remaining {money(Number(data?.totalRemainingAmount || 0))}
              </Text>
            </div>
          </FilterPanel>

          <DataTable<FinancialReportItem>
            rowKey="invoiceId"
            columns={columns}
            dataSource={data?.items || []}
            loading={isLoading}
            title={() => <Text className="text-[11px] uppercase tracking-[0.12em] text-slate-400">Financial Report</Text>}
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
